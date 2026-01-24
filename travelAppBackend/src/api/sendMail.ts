import express, { Request, Response } from 'express';
import { google } from 'googleapis';
import MailComposer from 'nodemailer/lib/mail-composer'; // ✉️ 편지 포장 전문가

const router = express.Router();

router.post('/send/email', async (req: Request, res: Response) => {
    console.log('[POST] 문의 메일 발송 요청 (Gmail API 방식)');
    const { title, content, email } = req.body;

    // 환경변수 가져오기
    const ADMIN_EMAIL = process.env.ADMIN_EMAIL;
    const CLIENT_ID = process.env.OAUTH_CLIENT_ID;
    const CLIENT_SECRET = process.env.OAUTH_CLIENT_SECRET;
    const REFRESH_TOKEN = process.env.OAUTH_REFRESH_TOKEN;

    if (!title || !content) {
        return res.status(400).json({ success: false, message: '필수 항목 누락' });
    }

    if (!ADMIN_EMAIL || !CLIENT_ID || !CLIENT_SECRET || !REFRESH_TOKEN) {
        console.error('🚨 서버 설정 에러: .env 정보 부족');
        return res.status(500).json({ success: false, message: '서버 설정 오류' });
    }

    // 1. 앱에는 먼저 성공 응답 (비동기 처리)
    res.status(202).json({ success: true, message: '접수 중입니다.' });

    try {
        // 2. OAuth2 클라이언트 설정
        const OAuth2 = google.auth.OAuth2;
        const oauth2Client = new OAuth2(
            CLIENT_ID,
            CLIENT_SECRET,
            "https://developers.google.com/oauthplayground"
        );

        oauth2Client.setCredentials({
            refresh_token: REFRESH_TOKEN
        });

        // 3. 엑세스 토큰 갱신 (자동 처리됨)
        const gmail = google.gmail({ version: 'v1', auth: oauth2Client });

        // 4. 메일 내용 만들기 (Nodemailer의 MailComposer 사용)
        const mailOptions = {
            from: `MoyeoLog <${ADMIN_EMAIL}>`,
            to: ADMIN_EMAIL,
            subject: `[문의사항] ${title}`,
            text: `발신자: ${email || '익명'}\n\n내용:\n${content}`,
            html: `<p><strong>발신자:</strong> ${email || '익명'}</p><p><strong>내용:</strong></p><pre>${content}</pre>`
        };

        const mailComposer = new MailComposer(mailOptions);
        const message = await mailComposer.compile().build();

        // 5. 구글 API가 좋아하는 형태(Base64Url)로 변환
        const rawMessage = Buffer.from(message)
            .toString('base64')
            .replace(/\+/g, '-')
            .replace(/\//g, '_')
            .replace(/=+$/, '');

        // 6. 🚀 Gmail API로 직접 전송 (HTTPS 포트 443 사용 - 절대 안 막힘)
        await gmail.users.messages.send({
            userId: 'me',
            requestBody: {
                raw: rawMessage,
            },
        });

        console.log('✅ Gmail API로 전송 성공! (HTTP 방식)');

    } catch (e: any) {
        console.error('❌ 메일 전송 실패 원인:', e.message);
        if (e.response) {
            console.error('구글 응답:', e.response.data);
        }
    }
});

export default router;