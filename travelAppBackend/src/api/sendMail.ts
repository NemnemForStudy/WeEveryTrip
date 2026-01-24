import express, { Request, Response } from 'express';
import nodemailer from 'nodemailer';
import SMTPTransport from 'nodemailer/lib/smtp-transport';
import { google } from 'googleapis';

const router = express.Router();

// 전역 transporter 캐싱 (연결 재사용)
let cachedTransporter: nodemailer.Transporter | null = null;

async function getTransporter() {
    if (cachedTransporter) {
        return cachedTransporter;
    }

    const CLIENT_ID = process.env.OAUTH_CLIENT_ID;
    const CLIENT_SECRET = process.env.OAUTH_CLIENT_SECRET;
    const REFRESH_TOKEN = process.env.OAUTH_REFRESH_TOKEN;
    const ADMIN_EMAIL = process.env.ADMIN_EMAIL;

    const OAuth2 = google.auth.OAuth2;
    const oauth2Client = new OAuth2(
        CLIENT_ID,
        CLIENT_SECRET,
        "https://developers.google.com/oauthplayground"
    );

    oauth2Client.setCredentials({
        refresh_token: REFRESH_TOKEN
    });

    const accessTokenResponse = await oauth2Client.getAccessToken();
    const accessToken = accessTokenResponse.token;

    if (!accessToken) {
        throw new Error("엑세스 토큰 갱신 실패");
    }

    cachedTransporter = nodemailer.createTransport({
        service: 'gmail',
        auth: {
            type: 'OAuth2',
            user: ADMIN_EMAIL,
            clientId: CLIENT_ID,
            clientSecret: CLIENT_SECRET,
            refreshToken: REFRESH_TOKEN,
            accessToken: accessToken,
        },
        // Render 환경 설정
        pool: {
            maxConnections: 1,
            maxMessages: Infinity,
            rateDelta: 20000,
            rateLimit: 5,
        },
        connectionUrl: 'smtps://smtp.gmail.com',
    } as SMTPTransport.Options);

    return cachedTransporter;
}

router.post('/send/email', async (req: Request, res: Response) => {
    console.log('[POST] 문의 메일 발송 요청 도착');
    const { title, content, email } = req.body;

    const ADMIN_EMAIL = process.env.ADMIN_EMAIL;
    const CLIENT_ID = process.env.OAUTH_CLIENT_ID;
    const CLIENT_SECRET = process.env.OAUTH_CLIENT_SECRET;
    const REFRESH_TOKEN = process.env.OAUTH_REFRESH_TOKEN;

    // 필수 값 검증
    if (!title || !content) {
        return res.status(400).json({ success: false, message: '필수 항목 누락' });
    }

    if (!ADMIN_EMAIL || !CLIENT_ID || !CLIENT_SECRET || !REFRESH_TOKEN) {
        console.error('🚨 서버 설정 에러: .env 설정 부족');
        return res.status(500).json({ success: false, message: '서버 설정 오류' });
    }

    // 먼저 응답 (202 Accepted)
    res.status(202).json({ success: true, message: '접수 중입니다.' });

    try {
        const transporter = await getTransporter();

        const mailOptions = {
            from: `MoyeoLog <${ADMIN_EMAIL}>`,
            to: ADMIN_EMAIL,
            subject: `[문의사항] ${title}`,
            html: `
                <p><strong>발신자:</strong> ${email || '익명'}</p>
                <hr />
                <p><strong>내용:</strong></p>
                <p>${content.replace(/\n/g, '<br>')}</p>
            `,
        };

        const result = await transporter.sendMail(mailOptions);
        console.log('✅ 메일 전송 성공:', result.messageId);

    } catch (error) {
        console.error('❌ 메일 전송 실패:', error);
        // 이미 응답했으므로 추가 응답 없음
    }
});

export default router;