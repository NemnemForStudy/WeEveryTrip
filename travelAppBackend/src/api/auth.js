// Express 라이브러리 가져옴. API 서버 기능 사용하기 위해 사용
const express = require('express');

// Express의 Router 기능 사용. 특정 결로 API 그룹화
const router = express.Router();
const db = require('../db');

const jwt = require('jsonwebtoken');

const JWT_SECRET_KEY = 'my-secret-for-travel-app';

// POST /api/ath/social-login 경로로 오는 POST 요청 처리하는 API 정의
// async / await 사용해 DB 작업을 비동기적 처리
router.post('/social-login', async(req, res) => {
    console.log('!!!!!!!!!! /api/auth/social-login 요청 도착 !!!!!!!!!!');
    const { email, nickname, profile_image, social_provider, social_id } = req.body

    if(!email || !social_id || !social_provider) {
        // 400 : 클라이언트 요청이 잘못되었음 의미
        return res.status(400).json({ message: '필수 정보(email, social_provider, social_id)가 누락되었습니다.' });
    }

    // try-catch 블록으로 DB 작업 중 발생할 수 있는 오류 처리
    try {
        // 1. social_id랑 social_provider로 기존 사용자 조회
        // $1, $2는 SQL 인젝션 공격 방지 위한 파라미터화된 쿼리
        const { rows } = await db.query(
            'SELECT * FROM "user" WHERE social_id = $1 AND social_provider = $2',
            [social_id, social_provider]
        );

        // 조회 사용자 정보
        let user = rows[0];

        // 2. 기존 사용자 없는 경우 새로 등록
        if(!user) {
            // INSERT 쿼리 사용해 새 사용자 추가
            // RETURNING * 구문으로 새로 추가된 사용자 정보 반환
            const insertQuery = `
                INSERT INTO "user"
                (email, nickname, profile_image, social_provider, social_id, created_at, updated_at)
                VALUES ($1, $2, $3, $4, $5, NOW(), NOW())
                RETURNING *
            `;

            // 쿼리에 전달할 파라미터들
            const insertValues = [
                email,
                nickname || email.split('@')[0], // 닉네임 없으면 이메일 아이디 부분 사용
                profile_image || null, //프로필 이미지 없으면 null
                social_provider,
                social_id
            ];

            // DB 쿼리 실행
            const result = await db.query(insertQuery, insertValues);
            user = result.rows[0]; // 새로 추가된 사용자 정보
            console.log('신규 회원 가입 완료:', user.email);
        } else {
            console.log('기존 회원 로그인: ', user.email)
        }
        
        // JWT 토큰 생성
        // jwt.sign() 함수는 세가지 정보 받아 토큰 만듦(페이로드, 비밀키, 옵션)
        const token = jwt.sign(
            { userId: user.user_id },
            // 비밀키 : 암호화 함.
            JWT_SECRET_KEY,
            // 옵션 : 토큰 유효기간 설정함.
            { expiresIn: '1h' }
        );

        // 3. 사용자 정보 클라이언트에 반환
        // 비밀번호 보안위해 제외
        delete user.password;
        res.status(200).json({
            message: '로그인에 성공했습니다',
            token: token,
            user: user
        });
    } catch(err) {
        // 데이터베이스 작업 중 오류가 발생하면, 서버 로그에 자세한 오류를 출력합니다.
    console.error('소셜 로그인 처리 중 오류 발생:', err.stack);
    // 클라이언트에는 간단한 오류 메시지와 함께 500 에러를 보냅니다.
    // 500 Internal Server Error: 서버 내부 문제로 요청을 처리할 수 없음을 의미합니다.
    res.status(500).json({ message: '서버 오류가 발생했습니다.' });
    }
})

module.exports = router;