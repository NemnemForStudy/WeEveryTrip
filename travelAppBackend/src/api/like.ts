import express, { Request, Response } from 'express';
import db from '../db';
import { authMiddleware } from '../middlewares/auth';

const router = express.Router();

// 1. 좋아요 추가 엔드포인트: POST /api/posts/:postId/like
router.post('/:postId/like', authMiddleware, async(req, res) => {
    const postId = req.params.postId;
    const userId = (req as any).user.id; // authMiddleware 넣어준 ID

    if(!userId) {
        return res.status(401).json({ message: '로그인이 필요합니다.' });
    }

    try {
        // DB 중복체크
        const checkResult = await db.query(
            'SELECT 1 FROM "post_like" WHERE "post_id" = $1 AND "user_id" = $2',
            [postId, userId]
        );

        if(checkResult.rows.length > 0) {
            // 409 Conflict는 중복/상태 불일치, 클라이언트가 롤백 하지 않도록 도와줌
            return res.status(409).json({
                success: false,
                message: '이미 좋아요를 누른 게시물'
            });
        }

        // INSERT 및 카운트 증가
        await db.query('BEGIN');

        await db.query(
            'INSERT INTO "post_like" ("post_id", "user_id", "created_at") VALUES ($1, $2, NOW())',
            [postId, userId]
        );

       // [수정] 좋아요 추가이므로 like_count를 +1 해야 함 (기존 -1은 오류)
       await db.query(
            'UPDATE "post" SET "like_count" = "like_count" + 1 WHERE "post_id" = $1',
            [postId]
        );
        
        await db.query('COMMIT'); // 트랜잭션 종료

        return res.status(200).json({ 
            success: true, 
            message: '좋아요 성공' 
        });

    } catch (error) {
        await db.query('ROLLBACK'); // 오류 발생 시 롤백
        console.error('좋아요 추가 중 오류:', error);
        return res.status(500).json({ success: false, message: '서버 오류' });
    }
});

// 2. 좋아요 취소 엔드포인트: DELETE /api/posts/:postId/like
router.delete('/:postId/like', authMiddleware, async(req: Request, res: Response) => {
    const postId = req.params.postId;
    const userId = (req as any).user.id;

    try {
        // [DB] DELETE 및 카운트 감소
        await db.query('BEGIN');
        
        const deleteResult = await db.query(
            'DELETE FROM "post_like" WHERE "post_id" = $1 AND "user_id" = $2 RETURNING *',
            [postId, userId]
        );

        if (deleteResult.rowCount === 0) {
            await db.query('ROLLBACK');
            return res.status(404).json({ success: false, message: '좋아요 기록이 없습니다.' });
        }

        await db.query(
          'UPDATE "post" SET "like_count" = GREATEST(0, "like_count" - 1) WHERE "post_id" = $1',
          [postId]
        );

        await db.query('COMMIT');

        return res.status(200).json({ success: true, message: '좋아요 취소 성공' });

    } catch (error) {
        await db.query('ROLLBACK');
        console.error('좋아요 취소 중 오류:', error);
        return res.status(500).json({ success: false, message: '서버 오류' });
    }
});

// 3. 좋아요 상태 확인 엔드포인트: GET /api/posts/:postId/is-liked
router.get("/:postId/is-liked", authMiddleware, async(req: Request, res: Response) => {
    const postId = req.params.postId;
    const userId = (req as any).user.id;

    try {
        await db.query('BEGIN');

        const checkResult = await db.query(
            // SELECT 1 FROM -> 조건에 맞는 행이 있는지 없는지 확인. 존재 유무(Existence)만 확인
            'SELECT 1 FROM "post_like" WHERE "post_id" = $1 AND "user_id" = $2',
            [postId, userId]
        );

        const isLiked = checkResult.rows.length > 0;

        return res.status(200).json({
            success: true,
            message: "좋아요 상태 성공",
            data: isLiked
        });
    } catch (error) {
        console.error('좋아요 상태 조회 중 서버 오류:', error);
        return res.status(500).json({ success: false, message: '서버 오류' });
    }
});

// 4. 좋아요 개수 조회 엔드포인트: GET /api/posts/:postId/count
router.get("/:postId/count", async(req: Request, res: Response) => {
    const postId = req.params.postId;
    
    try {
        // 해당 게시물에 좋아요 개수 COUNT(*) 계산
        // DB에서 직접 Count 하는 대신 테이블의 like_count 컬럼 읽음

        const countResult = await db.query(
            'SELECT "like_count" FROM "post" WHERE "post_id" = $1',
            [postId]
        );

        if(countResult.rows.length === 0) {
            // 게시물 존재하지 않을때
            return res.status(404).json({ success: false, message: '게시물을 찾을수가 없습니다.' });
        }

        const count = countResult.rows[0].like_count;

        return res.status(200).json({
            success: true,
            message: '좋아요 개수 조회 성공',
            data: count
        });
    } catch(error) {
        console.error('좋아요 개수 조회 중 서버 오류:', error);
        return res.status(500).json({ success: false, message: '서버 오류' });
    }
});

export default router;
