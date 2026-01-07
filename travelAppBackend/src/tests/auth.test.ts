import express from 'express';
import request from 'supertest';
import authRouter from '../api/auth';
import db from '../db';

const app = express();
app.use(express.json());
app.use('/api/auth', authRouter);

// 테스트 시작
describe('POST /api/auth/social-login', () => {
    // 각 테스트 끝나면 DB를 정리해 테스트 간 독립성 보장.
    afterEach(async () => {
        await db.query('DELETE FROM "user" WHERE email = $1', ['new.user@test.com']);
    });

    it('새로운 사용자가 로그인하면, 200 OK 와 함께 사용자 정보, 토큰 반환', async () => {
        // 테스트용 가짜 사용자 정보
        const newUser = {
            email: 'new.user@test.com',
            nickname: 'NewUser',
            social_provider: 'test_provider',
            social_id: 'test_id_1'
        };

        // supertest 사용해 실제 API에 POST 요청 보냄
        const response = await request(app)
            .post('/api/auth/social-login')
            .send(newUser);

        // 검증
        expect(response.status).toBe(200); // 상태 코드 200
        expect(response.body.token).toBeDefined(); // 응답 본문에 token 있어야 함.
        expect(response.body.user.email).toBe(newUser.email);
    })
})