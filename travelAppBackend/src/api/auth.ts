import express, { Router, Request, Response } from 'express';
import db from '../db';
import jwt from 'jsonwebtoken';
import { authMiddleware } from '../middlewares/auth';
import { endianness } from 'os';

const router: Router = express.Router();

const JWT_SECRET_KEY = process.env.JWT_SECRET_KEY;
if (!JWT_SECRET_KEY) {
    throw new Error('JWT_SECRET_KEY 환경 변수가 설정되지 않았습니다.');
}

interface User {
    user_id: number;
    email: string;
    nickname: string;
    profile_image?: string;
    social_provider: string;
    social_id: string;
    created_at: Date;
    updated_at: Date;
    password?: string;
    refresh_token?: string;
    deleted_at?: Date | null;
}

router.post('/social-login', async (req: Request, res: Response) => {
    console.log('========== 로그인 요청 시작 ==========');
    const { email, nickname, profile_image, social_provider, social_id } = req.body;

    if (!email || !social_id || !social_provider) {
        return res.status(400).json({ message: '필수 정보가 누락되었습니다.' });
    }

    const client = await db.getClient();

    try {
        // 1. 소셜 ID로 기존 회원 찾기
        const result = await client.query(
            'SELECT * FROM "user" WHERE social_id = $1 AND social_provider = $2',
            [social_id, social_provider]
        );

        let user: User = result.rows[0];

        if (user) {
            // 탈퇴한 회원(Soft Deleted)이 다시 로그인하면 계정 복구
            console.log(`2. DB에 저장된 기존 정보: ID(${user.user_id}), 닉네임(${user.nickname})`);
            if (user.deleted_at) {
                await client.query(
                    'UPDATE "user" SET deleted_at = NULL, updated_at = NOW() WHERE user_id = $1',
                    [user.user_id]
                );
                user.deleted_at = null; // 메모리 상 객체도 업데이트
                console.log(`탈퇴 회원(${user.user_id}) 계정 복구 완료`);
            }

            // 로그인 할 때마다 닉네임, 프로필 이미지 확인 및 업데이트.
            if(nickname && user.nickname !== nickname) {
                console.log(`3. 업데이트 실행조건 충족: ${user.nickname} -> ${nickname}`);
                await client.query(
                    'UPDATE "user" SET nickname = $1, profile_image = $2, updated_at = NOW() WHERE user_id = $3',
                    [nickname, profile_image, user.user_id]
                );
                user.nickname = nickname;
                user.profile_image = profile_image;
                console.log(`유저(${user.user_id}) 정보 최신화 완료: ${nickname}`);
            }
        } else {
            // 2. 소셜 정보가 없으면 이메일로 찾기 (계정 통합)
            const emailCheckResult = await db.query(
                `SELECT * FROM "user" WHERE email = $1`,
                [email]
            );

            if (emailCheckResult.rows.length > 0) {
                user = emailCheckResult.rows[0];
                
                // 계정 통합 시 소셜 정보 업데이트
                await db.query(
                    `UPDATE "user" SET social_provider = $1, social_id = $2, deleted_at = NULL WHERE user_id = $3`,
                    [social_provider, social_id, user.user_id]
                );
            } else {
                // 3. 아예 없으면 신규 가입
                const insertQuery = `
                    INSERT INTO "user" (email, nickname, profile_image, social_provider, social_id, created_at, updated_at)
                    VALUES ($1, $2, $3, $4, $5, NOW(), NOW())
                    RETURNING *
                `;
                const insertValues = [
                    email,
                    nickname || email.split('@')[0],
                    profile_image || null,
                    social_provider,
                    social_id
                ];
                const insertResult = await db.query(insertQuery, insertValues);
                user = insertResult.rows[0];
            }
        }

        // 토큰 발급 (테스트를 위해 1분으로 설정)
        const token = jwt.sign(
            { userId: user.user_id },
            JWT_SECRET_KEY,
            { expiresIn: '1m' } // 테스트용 1분
        );
        
        const refreshToken = jwt.sign(
            { userId: user.user_id },
            JWT_SECRET_KEY,
            { expiresIn: '7d' } // 리프레시 토큰은 7일 유지
        );

        await client.query(
            'UPDATE "user" SET refresh_token = $1 WHERE user_id = $2',
            [refreshToken, user.user_id]
        );

        await client.query('COMMIT');
        
        // 민감한 정보 제외하고 응답
        const userResponse: Omit<User, 'password' | 'refresh_token'> = {
            user_id: user.user_id,
            email: user.email,
            nickname: user.nickname,
            profile_image: user.profile_image || undefined,
            social_provider: user.social_provider,
            social_id: user.social_id,
            created_at: user.created_at,
            updated_at: user.updated_at,
            deleted_at: user.deleted_at || undefined
        };

        res.status(200).json({
            message: '로그인에 성공했습니다',
            token: token,
            refreshToken: refreshToken,
            user: userResponse
        });

    } catch (err) {
        console.error('소셜 로그인 처리 중 오류 발생:', (err as Error).stack);
        res.status(500).json({ message: '서버 오류가 발생했습니다.' });
    } finally {
        client.release();
    }
});

router.get("/mypage", async(req: Request, res: Response) => {
    const authHeader = req.headers.authorization;
    
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
        return res.status(401).json({ message: '인증 토큰이 필요합니다.' });
    }

    const token = authHeader.substring(7); // 'Bearer ' 제거 (안전한 방법)

    try {
        const decoded = jwt.verify(token, JWT_SECRET_KEY) as { userId: number };
        
        const result = await db.query(`
            SELECT 
                u.user_id,
                u.email,
                u.nickname,
                u.profile_image,
                u.created_at,
                u.push_activity,
                u.push_marketing,
                (SELECT COUNT(*) FROM post WHERE user_id = u.user_id AND deleted_at IS NULL) as post_count,
                (SELECT COUNT(*) FROM post_like WHERE user_id = u.user_id) as like_count,
                (SELECT COUNT(*) FROM comment WHERE user_id = u.user_id AND deleted_at IS NULL) as comment_count
            FROM "user" u
            WHERE u.user_id = $1 AND u.deleted_at IS NULL
        `, [decoded.userId]);

        if (result.rows.length === 0) {
            return res.status(404).json({ message: '사용자를 찾을 수 없습니다.' });
        }

        const user = result.rows[0];
        res.status(200).json({
            user_id: user.user_id,
            email: user.email,
            nickname: user.nickname,
            profile_image: user.profile_image,
            created_at: user.created_at,
            post_count: parseInt(user.post_count) || 0,
            like_count: parseInt(user.like_count) || 0,
            comment_count: parseInt(user.comment_count) || 0,
            push_activity: user.push_activity,
            push_marketing: user.push_marketing
        });
    } catch (err) {
        console.error('사용자 정보 조회 오류');
        res.status(401).json({ message: '유효하지 않은 토큰입니다.' });
    }
});

router.post("/logout", async(req: Request, res: Response) => {
    const userId = (req as any).user.id;
    const client = await db.getClient();

    try {
        await client.query('UPDATE "user" SET refresh_token = NULL WHERE user_id = $1', [userId]);

        res.status(200).json({ success: true, message: "로그아웃 성공" });

    } catch (error) {
        console.error('로그아웃 처리 중 오류');
        res.status(500).json({ success: false, message: "서버 오류" });
    } finally {
        client.release();
    }
});

router.put("/notification", authMiddleware, async(req: Request, res: Response) => {
    const userId = (req as any).user.id;
    const { type, enabled } = req.body;

    if(typeof enabled !== 'boolean') {
        return res.status(400).json({ message: "enabled 값은 boolean" });
    }

    const columnMap: Record<string, string> = {
        "activity": "push_activity",
        "marketing": "push_marketing"
    };
    
    const column = columnMap[type];
    if (!column) {
        return res.status(400).json({ message: "잘못된 타입 설정입니다." })
    }

    const client = await db.getClient();
    try {
        await client.query('BEGIN');
        // 매핑 객체를 통한 안전한 칼럼명 사용
        const queryText = `UPDATE "user" SET ${column} = $1 WHERE user_id = $2`;
        const result = await client.query(queryText, [enabled, userId]);

        if (result.rowCount === 0) {
            console.log("⚠️ 경고: 해당 user_id를 찾을 수 없어서 아무것도 변경 안 됨.");
            await client.query('ROLLBACK');
            return res.status(404).json({ success: false, message: "유저를 찾을 수 없습니다." });
        }

        await client.query('COMMIT'); 
        res.status(200).json({ success: true, message: "설정 변경 성공 "});
    } catch(error) {
        console.error('알림 설정 변경 중 오류');
        res.status(500).json({ success: false, message: "서버 오류" });
    } finally {
        client.release();
    }
})

// 회원 탈퇴
router.post("/withdraw", authMiddleware, async(req: Request, res: Response) => {
    // authMiddleware를 통과하면 req.user.id(또는 userId)에 접근 가능하다고 가정합니다.
    const userId = (req as any).user.id;
    const client = await db.getClient();

    try {
        await client.query('BEGIN') // 트랜잭션 시작

        // user 테이블 deleted_at 업데이트 및 refresh_token 무효화
        const withdrawQuery = `
            UPDATE "user"
            SET deleted_at = NOW(), refresh_token = NULL, updated_at = NOW()
            WHERE user_id = $1 AND deleted_at IS NULL
        `;
        const result = await client.query(withdrawQuery, [userId]);

        // 만약 이미 탈퇴한 사람이거나 없는 유저라면?
        if (result.rowCount === 0) {
            await client.query('ROLLBACK');
            return res.status(400).json({ success: false, message: "사용자를 찾을 수 없거나 이미 탈퇴한 계정입니다." });
        }
            // 해당 유저가 쓴 게시글 숨김 처리
        await client.query('UPDATE post SET deleted_at = NOW() WHERE user_id = $1', [userId]);

        await client.query('COMMIT');
        console.log(`회원 탈퇴 완료: User ID ${userId}`);
        res.status(200).json({ success: true, message: "회원 탈퇴가 완료되었습니다. 그동안 이용해 주셔서 감사합니다." });
    } catch(error) {
        await client.query('ROLLBACK');
        console.error('회원 탈퇴 처리 중 오류', error);
        res.status(500).json({ success: false, message: "서버 오류로 탈퇴 처리에 실패했습니다." });
    } finally {
        client.release();
    }
});

router.post('/updateProfile', authMiddleware, async (req: Request, res: Response) => {
    const userId = (req as any).user.id;
    const { nickname, profileImageUrl } = req.body;

    if (!nickname && !profileImageUrl) {
        return res.status(400).json({ 
            success: false, 
            message: '수정할 닉네임 또는 이미지 URL이 필요합니다.' 
        });
    }

    const client = await db.getClient();
    try {
        await client.query('BEGIN');

        /**
         * COALSCE($1, nickname) : $1(전달값)이 null이면 기존 nickname 유지.
         * 
         */
        const updateQuery = `
            UPDATE "user"
            SET
                nickname = COALESCE($1, nickname), 
                profile_image = COALESCE($2, profile_image),
                updated_at = NOW()
            WHERE user_id = $3 AND deleted_at IS NULL
            RETURNING user_id, nickname, profile_image;
        `;

        const result = await client.query(updateQuery, [
            nickname || null,
            profileImageUrl || null,
            userId
        ]);

        if(result.rowCount === 0) {
            await client.query('ROLLBACK');
            return res.status(404).json({
                success: false,
                message: '사용자를 찾을 수 없습니다.'
            });
        }
        await client.query('COMMIT');

        res.status(200).json({
            success: true,
            message: '프로필이 성공적으로 업데이트되었습니다.',
            user: result.rows[0]
        });
    } catch (error) {
        await client.query('ROLLBACK');
        console.error('프로필 업데이트 중 오류 발생:', error);
    }
});

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
            console.log('❌ [Auth] DB의 리프레시 토큰과 일치하지 않음');
            return res.status(401).json({ message: '유효하지 않은 리프레시 토큰입니다.' });
        }

        // 3. 새로운 액세스 토큰 및 리프레시 토큰 발급 (테스트용 1분)
        const newToken = jwt.sign(
            { userId: user.user_id },
            JWT_SECRET_KEY,
            { expiresIn: '1m' } // 테스트용 1분
        );

        const newRefreshToken = jwt.sign(
            { userId: user.user_id },
            JWT_SECRET_KEY,
            { expiresIn: '7d' } 
        );

        // 4. DB 업데이트
        await db.query(
            'UPDATE "user" SET refresh_token = $1, updated_at = NOW() WHERE user_id = $2',
            [newRefreshToken, user.user_id]
        );

        console.log(`✅ [Auth] 유저(${user.user_id}) 토큰 갱신 성공`);

        res.status(200).json({
            token: newToken,
            accessToken: newToken, 
            refreshToken: newRefreshToken
        });

    } catch (err) {
        console.error('❌ [Auth] 토큰 갱신 중 에러 발생:', err);
        res.status(401).json({ message: '리프레시 토큰이 만료되었거나 유효하지 않습니다.' });
    }
});

export default router;
