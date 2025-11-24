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
module.exports = router;