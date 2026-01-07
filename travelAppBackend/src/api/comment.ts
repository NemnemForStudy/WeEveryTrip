import express, { Request, Response } from 'express';
import db from '../db';
import { authMiddleware } from '../middlewares/auth';

const router = express.Router();

// 1. 댓글 작성 엔드포인트: POST /api/posts/:postId/comment
// 댓글 작성도 로그인 사용자만 가능하니까 authMiddleware 필요
router.post("/:postId/comment", authMiddleware, async(req: Request, res: Response) => {
    // postId는 URL 자체 변수처럼 포함되어서 온다.
    // ex) /api/posts/53/comment 처럼
    // Express 추출 : req.params 객체는 URL 경로에 있는 모든 동적 세그먼트를 키-값 쌍으로 담음.
    const postId = req.params.postId; 
    const userId = (req as any).user.id;
    const { content } = req.body;

    if(!content || typeof content !== 'string' || content.trim().length === 0) {
        return res.status(400).json({ success: false, message: '댓글 내용 입력해야함' });
    }

    const client = await db.getClient();
    try {
        await client.query('BEGIN');

        const insertQuery = `
            INSERT INTO "comment" ("post_id", "user_id", "content", "created_at")
            VALUES ($1, $2, $3, NOW())
            RETURNING *;
        `;

        const insertResult = await client.query(
            insertQuery, [postId, userId, content]
        );

        const userQuery = `SELECT nickname, profile_image FROM "user" WHERE user_id = $1`;
        const userResult = await client.query(userQuery, [userId]);
        const userInfo = userResult.rows[0];

        const responseData = {
            ...insertResult.rows[0], // 댓글 정보
            nickname: userInfo ? userInfo.nickname : "알 수 없음",
            profile_image: userInfo ? userInfo.profile_image : null
        }

        // 트랜잭션 완료
        await client.query('COMMIT');

        return res.status(201).json({ 
            success: true, 
            message: '댓글 작성 성공' ,
            data: responseData
        });
    } catch(error) {
        await client.query('ROLLBACK');
        console.error("댓글 작성 오류", error);
        return res.status(500).json({ success: false, message: '서버 오류 발생' });
    } finally {
        client.release(); // 연결 해제
    }
});

// 2. 댓글 조회 엔드포인트: GET /api/posts/:postId/comments
// 댓글 조회는 로그인 없어도 가능하므로 authMiddleware 사용하지 않을 수 있음.
router.get('/:postId/comments', async(req: Request, res: Response) => {
    const postId = req.params.postId;

    try {
        const commentsQuery = `
            SELECT
                c.comment_id,
                c.content,
                c.created_at,
                c.updated_at,
                u.user_id,
                u.nickname,
                u.profile_image
            FROM "comment" c
            JOIN "user" u ON c.user_id = u.user_id
            WHERE c.post_id = $1 AND c.deleted_at IS NULL
            ORDER BY c.created_at DESC;
        `

        // 인젝션 방지로 [postId] 방식으로 보냄.
        const result = await db.query(commentsQuery, [postId]);

        return res.status(200).json({
            success: true,
            message: '댓글 목록 조회 성공',
            data: result.rows
        })
    } catch(error) {
        console.error('댓글 목록 조회 중 서비스 오류:', error);
        return res.status(500).json({ success: false, message: '서버 오류' });
    }
});

// 3. 댓글 수정 엔드포인트: PUT /api/posts/comments/:commentId
router.put('/comments/:commentId', authMiddleware, async(req: Request, res: Response) => {
    const { commentId } = req.params;
    console.log(req.body);
    const { content } = req.body;
    const userId = (req as any).user.id;

    try {
        const client = await db.getClient();

        const result = await client.query(
            `UPDATE "comment" SET content = $1, updated_at = NOW()
            WHERE comment_id = $2 AND user_id = $3 RETURNING *`,
            [content, commentId, userId]
        );

        client.release();

        if(result.rowCount === 0) {
            return res.status(403).json({ success: false, message: "권한이 없거나 댓글이 없습니다." });
        }
        res.json({ success: true, message: "댓글 수정 완료" });
    } catch (e) {
        // 에러 처리
        res.status(500).json({ success: false, message: "서버 에러" });
    }
});

// 4. 댓글 삭제 엔드포인트: DELETE /api/posts/comments/:commentId
router.delete('/comments/:commentId', authMiddleware, async(req: Request, res: Response) => {
    const { commentId } = req.params;
    const userId = (req as any).user.id;

    try {
        const client = await db.getClient();
        const result = await client.query(
            `DELETE FROM "comment" WHERE comment_id = $1 AND user_id = $2`,
            [commentId, userId]
        );

        client.release();

        if(result.rowCount === 0) {
            return res.status(403).json({ success: false, message: "권한이 없거나 댓글이 없습니다." });
        }

        res.json({ success: true, message: "댓글 삭제 완료" });
    } catch (e) {
        res.status(500).json({ success: false, message: "서버 에러" });
    }
});

export default router;
