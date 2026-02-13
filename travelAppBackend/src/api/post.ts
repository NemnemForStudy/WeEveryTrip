import express, { Request, Response } from 'express';
import multer from 'multer';
import path from 'path';
import jwt from 'jsonwebtoken';
import db from '../db';
import sharp from 'sharp';
import fs from 'fs/promises';
import likeRouter from './like';
import commentRouter from './comment';
import { authMiddleware } from '../middlewares/auth';
import { createClient } from '@supabase/supabase-js';

const supabase = createClient(
  process.env.SUPABASE_URL!, // !는 값 넣어놨으니 진행해! 라는 뜻임.
  process.env.SUPABASE_ANON_KEY!
);

const router = express.Router();
const JWT_SECRET_KEY = process.env.JWT_SECRET_KEY;
if (!JWT_SECRET_KEY) {
    throw new Error('JWT_SECRET_KEY 환경 변수가 설정되지 않았습니다.');
}

// 서브라우터 마운트
router.use('/', likeRouter);
router.use('/', commentRouter);

interface DecodedToken {
    userId: number;
    iat: number;
    exp: number;
}

// Multer 설정 (임시 저장소)
// 여기서 저장된 파일은 잠시 후 sharp로 가공되고 삭제될 예정임
const storage = multer.memoryStorage();
const upload = multer({ storage: storage });

// ==========================================
// 1. 게시물 생성 API (POST)
// ==========================================
router.post('/', upload.any(), async (req: Request, res: Response) => {
    console.log('👉 [POST] 글 등록 요청 도착');
    const authHeader = req.headers.authorization;
    if(!authHeader) return res.status(401).json({ messge: '인증 토큰이 필요합니다. '});
    
    const client = await db.getClient();

    let userId: number;
    try {
        // 1. 토큰 검증
        if(!authHeader.startsWith('Bearer ')) return res.status(401).json({ messge: '토큰 형식 오류' });
        const token = authHeader.substring(7);
        const decodedToken = jwt.verify(token, JWT_SECRET_KEY) as DecodedToken;
        userId = decodedToken.userId;

    } catch(e) {
        return res.status(401).json({ message: '유효하지 않은 토큰입니다. '});
    }

    try {
        // 2. 데이터 파싱
        const { category, title, content, coordinates, tags, imageLocations } = req.body;

        // multipart에서 문자열(JSON)로 오는게 일반적이라 파싱 필요
        let parsedImageLocations: any[] = [];
        try {
            if(imageLocations) parsedImageLocations = JSON.parse(imageLocations);
            console.log("📥 서버가 받은 이미지 메타데이터:", parsedImageLocations);
        } catch(e) {
            parsedImageLocations = [];
        }

        // Multipart로 오면 boolean도 문자열 'true'로 옴.
        const isDomestic = req.body.isDomestic === 'true';

        console.log(`📝 데이터 확인 - 카테고리: ${category}, 제목: ${title}, 좌표: ${coordinates}`);

        if(!category || !title) {
            return res.status(400).json({ message: '카테고리와 제목은 필수입니다.' });
        }

        // DB 트랜잭션 시작 전 파일을 먼저 다듬음.
        const files = req.files as Express.Multer.File[] | undefined;
        let finalImageUrls: string[] = [];

        if(files && files.length > 0) {
            // Promise.all로 병렬 처리
            const processedImages = files.map(async (file) => {
                const fileName = `resized-${Date.now()}-${file.originalname}`;
                
                try {
                    // Sharp로 리사이징 및
                    const resizeBuffer = await sharp(file.buffer)
                        .rotate()
                        .resize({ width: 1024, withoutEnlargement: true })
                        .withMetadata()
                        .jpeg({ quality: 80 })
                        .toBuffer(); // 파일로 저장하지 않고 다시 버퍼로 받음

                    // Supabase Storage 업로드
                    const { data, error } = await supabase.storage
                        .from('ModuTripPosts')
                        .upload(fileName, resizeBuffer, {
                            contentType: 'image/jpeg',
                            upsert: true
                        });

                    if(error) throw error;

                    // 공개 URL 가져오기
                    const { data: { publicUrl } } = supabase.storage
                        .from('ModuTripPosts')
                        .getPublicUrl(fileName);

                    return publicUrl;
                } catch(imgErr) {
                    console.error(`이미지 변환 실패 (${file.originalname}):`, imgErr);
                    // 변환 실패시 원본 경로라도 사용
                    return file.path.replace(/\\/g, "/");
                }
            });

            // 모든 이미지 처리 끝날 때까지 대기하고 실패한 결과(null)는 걸러냄
            finalImageUrls = (await Promise.all(processedImages)).filter(url => url !== null) as string[];
        }

        // 썸네일은 첫 번째 가공된 이미지 사용
        const thumbnailUrl = finalImageUrls.length > 0 ? finalImageUrls[0] : null;

        await client.query('BEGIN');

        // Location 저장
        let locationId = null;
        let parsedCoord = null;

        if(coordinates && coordinates !== "null" && coordinates !== "") {
            try {
                parsedCoord = JSON.parse(coordinates);
                // ST_GeomFromGerJSON 사용
                const locQuery = `
                    INSERT INTO location(type, name, coordinate, is_domestic)
                    VALUES($1, $2, ST_GeomFromGeoJSON($3), $4)
                    RETURNING location_id
                `;

                const locRes = await client.query(locQuery, ['post_location', title, coordinates, isDomestic]);
                locationId = locRes.rows[0].location_id;
            } catch(e) {
                console.warn("⚠️ 위치 정보 저장 실패 (형식 오류 등):", e);
                // 위치 저장 실패해도 글은 올라가게 함 (locationId는 null 유지)
            }
        }

        // post 저장
        const coordParam = (parsedCoord) ? coordinates : null;

        const startDateMs = req.body.startDate ? Number(req.body.startDate) : null;
        const endDateMs = req.body.endDate ? Number(req.body.endDate) : null;
        // "YYYY-MM-DD"
        const startDate = startDateMs ? new Date(startDateMs).toISOString().slice(0, 10) : null;
        const endDate = endDateMs ? new Date(endDateMs).toISOString().slice(0, 10) : null;
        const postQuery = `
            INSERT INTO post(
                user_id, category_id, location_id, title, content,
                coordinate, is_domestic, travel_start_date, travel_end_date, thumbnail_url, created_at, updated_at
            )
            VALUES (
                $1,
                (SELECT category_id FROM category WHERE category_name = $2),
                $3, $4, $5,
                ST_GeomFromGeoJSON($6),
                $7, $8, $9, $10, NOW(), NOW()
            )
            RETURNING post_id, title, created_at, travel_start_date, travel_end_date
        `;

        const postRes = await client.query(postQuery, [
            userId,
            category,
            locationId,
            title,
            content,
            coordParam,
            isDomestic,
            startDate,
            endDate,
            thumbnailUrl
        ]);
        const newPost = postRes.rows[0];

        // 이미지 저장(일대 다)
        if(finalImageUrls.length > 0) {
            const imgInserts = finalImageUrls.map((url, i) => {
                const meta = parsedImageLocations[i] || {};
                return client.query(
                    `INSERT INTO post_image(post_id, image_url, latitude, longitude, day_number, sort_index, timestamp, created_at)
                    VALUES($1, $2, $3, $4, $5, $6, $7, NOW())`,
                    [
                        newPost.post_id,
                        url,
                        meta.latitude ?? null,
                        meta.longitude ?? null,
                        meta.dayNumber ?? null,
                        meta.indexInDay ?? null,
                        meta.timestamp ?? null
                    ]
                );
            });
            await Promise.all(imgInserts);
        }

        // 태그 저장 (선택 사항)
        // (태그 로직이 복잡하면 일단 생략 가능, 에러 방지를 위해 try-catch 감쌈)
        if(tags) {
            try {
                // 태그가 '태그1,태그2' 문자열로 오거나 배열로 올 수 있음
                const tagList = Array.isArray(tags) ? tags : tags.split(',').map((t:string) => t.trim());
                for (const tagName of tagList) {
                    if(!tagName) continue;
                    await client.query(`
                        WITH inserted_tag AS(
                            INSERT INTO tag(tag_name, created_at) VALUES ($1, NOW())
                            ON CONFLICT (tag_name) DO UPDATE SET tag_name = $1 RETURNING tag_id
                        )
                        INSERT INTO post_tag(post_id, tag_id, created_at)
                        SELECT $2, tag_id, NOW() FROM inserted_tag
                        ON CONFLICT DO NOTHING
                    `, [tagName, newPost.post_id]);
                }
            } catch (tagError) {
                console.warn("태그 저장 중 오류 (무시됨):", tagError);
            }
        }
        await client.query('COMMIT');
        console.log(`✅ 게시물 생성 완료 (ID: ${newPost.post_id})`);

        res.status(201).json({ success: true, data: { ...newPost, images: finalImageUrls } });
    } catch(err) {
        await client.query('ROLLBACK');
        console.error('🚨 게시물 생성 실패 (DB Error):', (err as Error).stack);
        res.status(500).json({ success: false, message: '서버 오류로 저장 실패' });
    } finally {
        client.release();
    }
});


// ==========================================
// 2. 게시물 검색 API (GET /api/posts/search)
// ==========================================
router.get('/search', authMiddleware, async (req, res) => {
    const searchQuery = req.query.q as string;
    const userId = (req as any).user.id;

    console.log(`👉 [검색 요청] 검색어: ${searchQuery}`);

    if(!searchQuery) {
        return res.status(400).json({ message: "검색어를 입력해주세요." });
    }

    try {
        // 🔥 [안전 모드] 쿼리 단순화 (복잡한 조인/서브쿼리 일단 제외하고 기본부터 확인)
        const queryText = `
            SELECT 
                p.post_id,
                p.user_id,
                p.title, 
                p.content, 
                p.created_at,
                u.nickname,
                p.is_domestic,
                p.thumbnail_url,
                ST_AsGeoJSON(p.coordinate)::json as coordinate,
                c.category_name as category,
                COALESCE(p.like_count, 0) as like_count,
                (SELECT COUNT(*) FROM comment cm WHERE cm.post_id = p.post_id AND cm.deleted_at IS NULL) as comment_count
            FROM post p
            JOIN "user" u ON p.user_id = u.user_id
            LEFT JOIN category c ON p.category_id = c.category_id
            WHERE p.deleted_at IS NULL
            AND p.user_id = $2
            AND (p.title ILIKE $1 OR p.content ILIKE $1)
            ORDER BY p.created_at DESC
        `;

        const result = await db.query(queryText, [`%${searchQuery}%`, userId]);
        
        // 결과 반환
        res.status(200).json(result.rows);

    } catch (err) {
        console.error('🚨 검색 API 에러 발생:', (err as Error).stack); // 🔥 이 로그를 봐야 합니다!
        res.status(500).json({ message: '서버 에러: 로그를 확인하세요' });
    }
});

// 4. 게시물 상세 조회 API (GET /api/posts/:id)
router.get('/:id', async(req: Request, res: Response) => {
    const postId = req.params.id;
    console.log(`👉 [상세 조회 요청] ID: ${postId}`);

    try {
        const queryText = `
        SELECT
            p.post_id,
            p.user_id,
            p.title,
            p.content,
            p.created_at,
            u.nickname,
            p.is_domestic,
            p.thumbnail_url as "imgUrl",
            p.travel_start_date,
            p.travel_end_date,
            ST_ASGeoJSON(p.coordinate)::json as coordinate,
            c.category_name as category
        FROM post p
        JOIN "user" u ON p.user_id = u.user_id
        LEFT JOIN category c ON p.category_id = c.category_id
        WHERE p.post_id = $1 AND p.deleted_at IS NULL
        `;

        const result = await db.query(queryText, [postId]);

        if(result.rows.length === 0) {
            return res.status(404).json({ message: "게시물을 찾을 수 없습니다." });
        }

        // 이미지 리스트 조회
        const imagesQuery = `
            SELECT image_url FROM post_image
            WHERE post_id = $1
            ORDER BY created_at ASC
        `;
        const imagesResult = await db.query(imagesQuery, [postId]);
        const images = imagesResult.rows.map(row => row.image_url);

        // 사진별 위치 정보 조회 (Detail 지도에서 마커 여러개 표시용)
        // - GPS 없는 사진은 latitude/longitude가 null로 내려감
        const imageLocationsQuery = `
            SELECT
                image_url,
                latitude,
                longitude,
                day_number,
                sort_index,
                timestamp
            FROM post_image
            WHERE post_id = $1
            ORDER BY day_number ASC NULLS LAST, sort_index ASC NULLS LAST, created_at ASC
        `;
        const imageLocationsResult = await db.query(imageLocationsQuery, [postId]);
        const image_locations = imageLocationsResult.rows;

        res.status(200).json({ ...result.rows[0], images, image_locations });
    } catch (err) {
        console.error('🚨 상세 조회 에러:', (err as Error).stack);
        res.status(500).json({ message: '서버 에러' });
    }
})

// ==========================================
// 3. 전체 게시물 조회 API (GET /api/posts)
// ==========================================
router.get('/', async (req, res) => {
    // 안드로이드가 ?search=... 로 보낼 경우 대비
    if (req.query.search) {
        return res.redirect(`/api/posts/search?q=${req.query.search}`);
    }

    console.log(`👉 [전체 조회 요청]`);

    try {
        const queryText = `
            SELECT 
                p.post_id, 
                p.title, 
                p.content, 
                p.created_at,
                u.nickname,
                p.is_domestic,
                p.thumbnail_url,
                ST_AsGeoJSON(p.coordinate)::json as coordinate,
                c.category_name as category,
                p.user_id,
                COALESCE(p.like_count, 0) as like_count,
                (SELECT COUNT(*) FROM comment cm WHERE cm.post_id = p.post_id AND cm.deleted_at IS NULL) as comment_count
            FROM post p
            JOIN "user" u ON p.user_id = u.user_id
            LEFT JOIN category c ON p.category_id = c.category_id
            WHERE p.deleted_at IS NULL
            ORDER BY p.created_at DESC
        `;

        const result = await db.query(queryText);
        res.status(200).json(result.rows);

    } catch (err) {
        console.error('🚨 전체 조회 API 에러 발생:', (err as Error).stack); // 🔥 이 로그를 봐야 합니다!
        res.status(500).json({ message: '서버 에러: 로그를 확인하세요' });
    }
});

router.put('/:postId/update', authMiddleware, async(req: Request, res: Response) => {
    const postId = req.params.postId;
    const userId = (req as any).user.id;
    const {
        category, title, content, coordinate, isDomestic, travelStartDate, travelEndDate,
        tags,
        images,
        imageLocations
    } = req.body;
    const coordinateJson = coordinate ? JSON.stringify(coordinate) : null;

    const client = await db.getClient();
    try {
        await client.query('BEGIN');

        const checkResult = await client.query(
            'SELECT user_id FROM "post" WHERE post_id = $1',
            [postId]
        );

        if(checkResult.rows.length === 0) {
            await client.query('ROLLBACK');
            return res.status(404).json({ success: false, message: '게시물을 찾을 수 없습니다.' });
        }

        if(checkResult.rows[0].user_id !== userId) {
            await client.query('ROLLBACK');
            return res.status(403).json({ success: false, message: '수정 권한이 없습니다.' });
        }

        const result = await client.query(
            `UPDATE "post" SET
                category_id = (
                    SELECT category_id
                    FROM category
                    WHERE category_name = $1
                ),
                title = $2,
                content = $3,
                coordinate = $4,
                is_domestic = $5,
                travel_start_date = $6,
                travel_end_date = $7,
                updated_at = NOW()
            WHERE post_id = $8
            RETURNING post_id, user_id, category_id, location_id, title, content,
                      view_count, created_at, is_domestic, deleted_at,
                      thumbnail_url, like_count, travel_start_date, travel_end_date,
                      ST_ASGeoJSON(coordinate)::json AS coordinate`,
            [category, title, content, coordinateJson, isDomestic, travelStartDate, travelEndDate, postId]
        );

        await client.query('DELETE FROM post_image WHERE post_id = $1', [postId]);

        // imageLocations를 신뢰(없으면 images만으로라도 insert 가능)
        // imageLocations 항목 예시:
        // { imageUrl, latitude, longitude, dayNumber, sortIndex }
        for(const loc of (imageLocations ?? [])) {
            await client.query(
                `INSERT INTO post_image (post_id, image_url, latitude, longitude, day_number, sort_index, timestamp)
                 VALUES ($1, $2, $3, $4, $5, $6, $7)`,
                 [postId, loc.imageUrl, loc.latitude ?? null, loc.longitude ?? null, loc.dayNumber ?? null, loc.sortIndex ?? 0, loc.timestamp ?? null]
            );
        }

        // thumbnail_url을 images[0]으로 교체하고 싶다면 여기서 같이 갱신
        await client.query('UPDATE "post" SET thumbnail_url = $1 WHERE post_id = $2', [images?.[0] ?? null, postId]);

        // 태그 갱신: 기존 태그 삭제 후 새 태그 insert
        await client.query('DELETE FROM post_tag WHERE post_id = $1', [postId]);
        if (tags && Array.isArray(tags)) {
            for (const tagName of tags) {
                if (!tagName) continue;
                await client.query(`
                    WITH inserted_tag AS (
                        INSERT INTO tag(tag_name, created_at) VALUES ($1, NOW())
                        ON CONFLICT (tag_name) DO UPDATE SET tag_name = $1 RETURNING tag_id
                    )
                    INSERT INTO post_tag(post_id, tag_id, created_at)
                    SELECT $2, tag_id, NOW() FROM inserted_tag
                    ON CONFLICT DO NOTHING
                `, [tagName, postId]);
            }
        }

        await client.query('COMMIT');
        return res.status(200).json({
            success: true,
            message: "게시물 수정 성공!",
            data: result.rows[0]
        });
    } catch(error) {
        await client.query('ROLLBACK');
        console.error('게시물 수정 오류:', error);
        return res.status(500).json({ success: false, message: '서버 오류' });
    } finally {
        client.release();
    }
})

router.delete('/:postId/delete', authMiddleware, async(req: Request, res: Response) => {
    const postId = req.params.postId;
    const userId = (req as any).user.id;
    const client = await db.getClient();
    try {
        await client.query('BEGIN');

        const checkResult = await client.query(
            'SELECT user_id FROM post WHERE post_id = $1 AND deleted_at IS NULL',
            [postId]
        );

        if(checkResult.rows.length === 0) {
            await client.query('ROLLBACK');
            return res.status(404).json({ success: false, message: '게시물을 찾을 수 없습니다.' });
        }

        if(checkResult.rows[0].user_id !== userId) {
            await client.query('ROLLBACK');
            return res.status(403).json({ success: false, message: '삭제 권한이 없습니다.' });
        }

        await client.query(
            `UPDATE post
             SET deleted_at = NOW(), updated_at = NOW()
             WHERE post_id = $1`,
             [postId]
        );

        await client.query('COMMIT');
        return res.status(200).json({ success: true, message: '게시물이 삭제되었습니다.' });
    } catch(error) {
        await client.query('ROLLBACK');
        console.log('게시물 삭제 오류', error);
        return res.status(500).json({ success: false, message: '서버 오류' });
    } finally {
        client.release();
    }
})

// 새 사진 업로드 전용
// POST /api/posts/upload-images
router.post('/upload-images', authMiddleware, upload.any(), async (req: Request, res: Response) => {
    try {
        // multer가 처리한 파일들
        const files = (req.files as Express.Multer.File[]) ?? [];

        if (files.length === 0) {
            return res.status(400).json({ success: false, message: '업로드할 파일이 없습니다.' });
        }

        // create에서 쓰는 방식대로 "서버에서 접근 가능한 URL을 만들어야한다."
        // ex) /uploads/xxx.jpg
        const processed = files.map(async (file) => {
            // 메모리 방식이므로 file.path 대신 file.originalname 등을 활용해 파일명 생성
            const fileName = `resized-${Date.now()}-${file.originalname}`;

            try {
                // 1. Sharp로 메모리 버퍼(file.buffer)를 바로 가공
                const resizedBuffer = await sharp(file.buffer)
                    .rotate()
                    .resize({ width: 1024, withoutEnlargement: true })
                    .withMetadata()
                    .jpeg({ quality: 80 })
                    .toBuffer(); // ✅ 파일로 저장하지 않고 버퍼로 반환

                // 2. 가공된 버퍼를 Supabase에 바로 업로드
                const { data, error } = await supabase.storage
                    .from('ModuTripPosts')
                    .upload(fileName, resizedBuffer, {
                        contentType: 'image/jpeg',
                        upsert: true
                    });

                if (error) throw error;

                // 3. 공개 URL 생성
                const { data: { publicUrl } } = supabase.storage
                    .from('ModuTripPosts')
                    .getPublicUrl(fileName);

                // ✅ fs.unlink 코드가 필요 없어졌습니다!
                return publicUrl;
            } catch (e) {
                console.error(`이미지 변환/업로드 실패: `, e);
                return null;
            }
        });

        const urls = (await Promise.all(processed)).filter(url => url !== null);

        return res.status(200).json({
            success: true,
            message: "이미지 업로드 성공",
            urls
        });
    } catch (e) {
        console.error('🚨 이미지 업로드 라우터 에러:', e);
        return res.status(500).json({ success: false, message: '서버 오류' });
    }
})

typescript:travelAppBackend/src/api/auth.ts
// ... 기존 코드 하단에 추가 ...

/**
 * 토큰 갱신 API
 * POST /api/auth/refresh
 */
router.post('/refresh', async (req: Request, res: Response) => {
    console.log('🔄 [Auth] 토큰 갱신 요청 수신');
    const authHeader = req.headers.authorization;

    if (!authHeader || !authHeader.startsWith('Bearer ')) {
        return res.status(401).json({ message: '리프레시 토큰이 필요합니다.' });
    }

    const refreshToken = authHeader.substring(7);

    try {
        // 1. 리프레시 토큰 검증
        const decoded = jwt.verify(refreshToken, JWT_SECRET_KEY) as { userId: number };

        // 2. DB에 저장된 리프레시 토큰과 일치하는지 확인
        const result = await db.query(
            'SELECT * FROM "user" WHERE user_id = $1 AND refresh_token = $2 AND deleted_at IS NULL',
            [decoded.userId, refreshToken]
        );

        const user = result.rows[0];
        if (!user) {
            return res.status(401).json({ message: '유효하지 않은 리프레시 토큰입니다.' });
        }

        // 3. 새로운 액세스 토큰 및 리프레시 토큰 발급 (Rotation 방식 권장)
        const newToken = jwt.sign(
            { userId: user.user_id },
            JWT_SECRET_KEY,
            { expiresIn: '1m' } // 새 액세스 토큰 1시간
        );
    console.log(`✅ [Auth] 유저(${user.user_id}) 토큰 갱신 완료`);

        const newRefreshToken = jwt.sign(
            { userId: user.user_id },
            JWT_SECRET_KEY,
            { expiresIn: '7d' } // 새 리프레시 토큰 7일
        );

        // 4. DB 업데이트
        await db.query(
            'UPDATE "user" SET refresh_token = $1, updated_at = NOW() WHERE user_id = $2',
            [newRefreshToken, user.user_id]
        );

        res.status(200).json({
            token: newToken,
            accessToken: newToken, // 안드로이드 모델명에 맞춤
            refreshToken: newRefreshToken
        });

    } catch (err) {
        console.error('토큰 갱신 중 오류 발생:', err);
        res.status(401).json({ message: '리프레시 토큰이 만료되었거나 유효하지 않습니다.' });
    }
});

export default router;