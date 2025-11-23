const express = require('express');
const router = express.Router();
const db = require('../db'); // 위에서 만든 db연결 가져옴.

// POST /api/posts - 새 게시물 생성
router.post('/', async(req, res) => {
    // 앱에서 보낸 데이터
    const { category, title, content, tags, imgUrl } = req.body;

    // 데이터 유효성 검사
    if(!category || !title) {
        return res.status(400).json({ message: '카테고리, 제목은 필수 항목입니다.'});
    }

    try {
        const userId = 1;
        const categoryId = (category === "국내여행") ? 1 : 2;
        // DB에 데이터를 삽입하는 SQL 쿼리
        // image_url과 create_ad은 DB 스키마에 정의된 컬럼명으로 가정함.
        const query = `
            INSERT INTO post(user_id, category_id, title, content, created_at, updated_at)
            VALUES ($1, $2, $3, $4, NOW(), NOW())
            RETURNING post_id, user_id, category_id, title, content
        `;
        const values = [userId, categoryId, title, content];

        // db.js의 쿼리 함수 사용해 DB에 쿼리 실행
        const { rows } = await db.query(query, values);

        // 성공적으로 생성된 게시물 정보를 클라이언트에 반환(201 Created)
        res.status(201).json(rows[0]);
        
    } catch(err) {
        console.error('게시물 생성 중 오류 발생: ', err.stack);
        res.status(500).json({ message: '서버 오류로 인해 게시물을 생성할 수 없습니다.' });
    }
})
module.exports = router;