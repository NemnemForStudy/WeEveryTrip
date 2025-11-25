const express = require('express');
const router = express.Router();
const db = require('../db'); // 위에서 만든 db연결 가져옴.
const jwt = require('jsonwebtoken');
const multer = require('multer');
const JWT_SECRET_KEY = 'my-secret-for-travel-app';

const storage = multer.diskStorage({
    // 파일이 저장될 목적지 설정
    destination: function(req, file, cb) {
        cb(null, 'uploads/')
    },
    // 파일 이름 설정
    filename: function(req, file, cb) {
        cb(null, file.fieldname + '-' + Date.now() + path.extname(file.originalname));
    }
});

const upload = multer({ storage: storage });

// POST /api/posts - 새 게시물 생성
// upload.single('image') 미들웨어 추가해 image라는 이름으로 들어오는 파일 1개 처리함.
router.post('/', upload.array('image', 5), async(req, res) => {
    const authHeader = req.headers.authorization;
    if(!authHeader) {
        return res.status(401).json({ message: '인증 토큰이 필요합니다.' });
    }

    // DB 클라이언트를 트랜잭션용으로 가져옴
    const client = await db.getClient();

    try {
        const token = authHeader.split(' ')[1];

        // 토큰 검증 및 디코딩
        const decodedToken = jwt.verify(token, JWT_SECRET_KEY);

        // 토큰에서 userId 추출
        const userId = decodedToken.userId;

        if(!userId) {
            return res.status(401).json({ message: '인증 토큰이 유효하지 않습니다.' });
        }

        // 앱에서 보낸 데이터
        const { category, title, content, tags, imgUrl } = req.body;
        // 각 파일의 경로를 추출해 배열로 만듦
        const imageUrls = req.files ? req.files.map(file => file.path) : [];

        // 데이터 유효성 검사
        if(!category || !title) {
            return res.status(400).json({ message: '카테고리, 제목은 필수 항목입니다.'});
        }

        // 트랜잭션 시작
        await client.query('BEGIN');

        const categoryId = (category === "국내여행") ? 1 : 2;

        // DB에 데이터를 삽입하는 SQL 쿼리
        // image_url과 create_ad은 DB 스키마에 정의된 컬럼명으로 가정함.
        const postQuery = `
            INSERT INTO post(user_id, category_id, title, content, created_at, updated_at)
            VALUES ($1, $2, $3, $4, NOW(), NOW())
            RETURNING post_id, user_id, category_id, title, content
        `;
        const postValues = [userId, categoryId, title, content];
        const postResult = await client.query(postQuery, postValues);
        const newPost = postResult.rows[0];
        const newPostId = newPost.post_id;

        if(imageUrls.length > 0) {
            const imageInsertPromises = imageUrls.map(url => {
                const imageQuery = `
                    INSERT INTO post_image(post_id, image_url, created_at)
                    VALUES($1, $2, NOW())
                `;
                return client.query(imageQuery, [newPostId, url]);
            });

            // 모든 이미지 저장 쿼리가 완료될 떄까지 기다림.
            await Promise.all(imageInsertPromises);
        }

        // 모든 작업이 성공하면 트랜잭션 커밋
        await client.query('COMMIT');

        // 성공적으로 생성된 게시물 정보를 클라이언트에 반환(201 Created)
        res.status(201).json({
            ...newPost,
            images: imageUrls
        });
     } catch(err) {
        // 에러 발생 시 트랜잭션 롤백
        await client.query('ROLLBACK');

        if(err.name === 'JsonWebTokenError' || err.name === 'TokenExpiredError') {
            console.error('토큰 관련 오류:', err.message);
            return res.status(401).json({ message: '유효하지 않은 토큰입니다.' })
        }
        // 그 외 서버 에러 처리
        console.error('게시물 생성 중 오류 발생: ', err.stack);
        res.status(500).json({ message: '서버 오류로 인해 게시물을 생성할 수 없습니다.' });
    } finally {
        // 사용 DB 클라이언트 반환
        client.release();
    }
})

// GET /api/posts - 모든 게시물 조회 또는 제목으로 검색
router.get('/', async(req, res) =>  {
    // 클라이언트가 보낸 URL 쿼리에서 search 값을 가져옴.
    const { search } = req.query;
    console.log('=== 검색 요청 ===');
    console.log('search 파라미터:', search);
    console.log('search 타입:', typeof search);
    const client = await db.getClient(); // db 연결 가져옴.

    try {
        let queryText;
        const queryParams = [];

        if(search) {
            // 검색어가 있는 경우 : SQL 쿼리에서 WHERE 절 추가해 제목(title) 필터링함.
            queryText = `
                SELECT p.post_id, p.title, p.content, u.nickname, p.created_at
                FROM post p
                JOIN "user" u ON p.user_id = u.user_id
                WHERE p.title ILIKE $1
                ORDER BY p.created_at DESC
            `;
            queryParams.push(`%${search}%`);
        } else {
            // 검색어 없음
            queryText = `
                SELECT p.post_id, p.title, p.content, u.nickname, p.created_at
                FROM post p
                JOIN "user" u ON p.user_id = u.user_id
                ORDER BY p.created_at DESC
            `;
        }

        const result = await db.query(queryText, queryParams);
        console.log('=== 검색 결과 ===');
        console.log('쿼리:', queryText);
        console.log('파라미터:', queryParams);
        console.log('결과 행 수:', result.rows.length);
        console.log('결과:', result.rows);
        // 성공적 조회
        res.status(200).json(result.rows);
    } catch(err) {
        // 서버 에러 처리
        console.error('게시물 조회 중 오류 발생: ', err.stack);
        res.status(500).json({ message: '서버 오류로 인해 게시물 조회할 수 없습니다.' });
    } finally {
        // 사용한 클라이언트 반환
        client.release();
    }
})

// ==================================================
// 임시 테스트용 API (닉네임 컬럼 확인용)
// ==================================================
router.get('/test-nickname', async (req, res) => {
    try {
        // 다른 테이블은 전혀 건드리지 않고, 오직 "user" 테이블에서 "nickname"만 가져옵니다.
        const result = await db.query('SELECT nickname FROM "user" LIMIT 1');
        // 성공하면, 성공 메시지와 함께 실제 데이터를 보여줍니다.
        res.status(200).json({
            message: '성공! user 테이블에서 nickname 컬럼을 찾았습니다.',
            data: result.rows
        });
    } catch (err) {
        // 실패하면, 어떤 에러 메시지가 발생하는지 명확하게 보여줍니다.
        res.status(500).json({
            message: '오류! user 테이블에서 nickname 컬럼을 찾을 수 없습니다.',
            error: err.message,
            detail: err.detail
        });
    }
});

module.exports = router;