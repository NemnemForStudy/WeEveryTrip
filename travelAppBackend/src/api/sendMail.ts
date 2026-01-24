import express, { Request, Response } from 'express';
import nodemailer from 'nodemailer';
import SMTPTransport from 'nodemailer/lib/smtp-transport';
import { google } from 'googleapis';

const router = express.Router();

router.post('/send/email', async (req: Request, res: Response) => {
    console.log('[POST] 문의 메일 발송 요청 도착');
    const { title, content, email } = req.body;

    // 환경변수 가져오기
    const ADMIN_EMAIL = process.env.ADMIN_EMAIL;
    const CLIENT_ID = process.env.OAUTH_CLIENT_ID;
    const CLIENT_SECRET = process.env.OAUTH_CLIENT_SECRET;
    const REFRESH_TOKEN = process.env.OAUTH_REFRESH_TOKEN;

    // 1. 필수 값 검증
    if (!title || !content) {
        return res.status(400).json({ success: false, message: '필수 항목 누락' });
    }

    if (!ADMIN_EMAIL || !CLIENT_ID || !CLIENT_SECRET || !REFRESH_TOKEN) {
        console.error('🚨 서버 설정 에러: .env에 OAuth 관련 설정이 부족합니다.');
        return res.status(500).json({ success: false, message: '서버 설정 오류' });
    }

    // 2. 앱에 먼저 응답 (사용자 경험 개선)
    res.status(202).json({ success: true, message: '접수 중입니다.' });

    try {
        // 3. OAuth2 클라이언트 설정 및 엑세스 토큰 갱신
        const OAuth2 = google.auth.OAuth2;
        const oauth2Client = new OAuth2(
            CLIENT_ID,
            CLIENT_SECRET,
            "https://developers.google.com/oauthplayground"
        );

        oauth2Client.setCredentials({
            refresh_token: REFRESH_TOKEN
        });

        // ⚡️ 여기서 실시간으로 새 토큰을 받아옵니다!
        const accessTokenResponse = await oauth2Client.getAccessToken();
        const accessToken = accessTokenResponse.token;

        if (!accessToken) {
            throw new Error("엑세스 토큰 갱신 실패");
        }

        // 4. Nodemailer 설정 (성공했던 설정 그대로 적용)
        const transporter = nodemailer.createTransport({
            service: 'gmail',
            auth: {
                type: 'OAuth2',
                user: ADMIN_EMAIL, // kotlinstudyga@gmail.com
                clientId: CLIENT_ID,
                clientSecret: CLIENT_SECRET,
                refreshToken: REFRESH_TOKEN,
                accessToken: accessToken as string, // 갱신된 토큰 사용
            },
        } as SMTPTransport.Options);

        // 5. 메일 옵션 설정
        const mailOptions = {
            from: `MoyeoLog <${ADMIN_EMAIL}>`, // 오타 수정: ? -> >
            to: ADMIN_EMAIL,
            subject: `[문의사항] ${title}`,
            text: `발신자: ${email || '익명'}\n\n내용:\n${content}`,
        };

        // 6. 전송
        await transporter.sendMail(mailOptions);
        console.log('✅ 메일 전송 성공');

    } catch (e) {
        console.error('❌ 메일 전송 실패:', e);
        // 이미 202 응답을 보냈으므로 추가 응답은 하지 않습니다.
    }
});

export default router;