import express, { Request, Response } from 'express';
import nodemailer from 'nodemailer';
import SMTPTransport from 'nodemailer/lib/smtp-transport';
import { google } from 'googleapis';
import { oauth2 } from 'googleapis/build/src/apis/oauth2';

const router = express.Router();

// 보안 위해 환경변수 권장.
const ADMIN_EMAIL = process.env.ADMIN_EMAIL;
const ADMIN_PASSWORD = process.env.EMAIL_PASS;

console.log('--- 환경 변수 체크 ---');
console.log('ADMIN_EMAIL:', process.env.ADMIN_EMAIL);
console.log('EMAIL_PASS 존재 여부:', !!process.env.EMAIL_PASS);
console.log('EMAIL_PASS 길이:', process.env.EMAIL_PASS?.length);
console.log('EMAIL_PASS 시작/끝:', `${process.env.EMAIL_PASS?.substring(0, 2)}***${process.env.EMAIL_PASS?.slice(-2)}`);
console.log('--------------------');

router.post('/send/email', async(req: Request, res: Response) => {
    console.log('[POST] 문의 메일 발송 요청 도착');
    const { title, content, email } = req.body;
    console.log(email)

    if(!title || !content) {
        return res.status(400).json({ success: false, message: '필수 항목 누락' });
    }

    // 환경변수 체크
    if(!ADMIN_EMAIL || !ADMIN_PASSWORD) {
        console.error('🚨 서버 설정 에러: EMAIL_USER 또는 EMAIL_PASS가 .env에 없습니다.');
        return res.status(500).json({ success: false, message: '서버 메일 설정 오류' });
    }

    // 🔥 [핵심] 앱에 먼저 성공 응답을 보냅니다. (앱의 뱅글뱅글 멈춤 해결)
    res.status(202).json({ success: true, message: '접수 중입니다.' });

    try {
        const Oauth2 = google.auth.OAuth2;
        const oauth2Client = new Oauth2(
            process.env.OAUTH_CLIENT_ID,
            process.env.OAUTH_CLIENT_SECRET,
            "https://developers.google.com/oauthplayground"
        );

        oauth2Client.setCredentials({
            refresh_token: process.env.OAUTH_REFRESH_TOKEN
        });

        const transporter = nodemailer.createTransport({
            service: 'gmail',
            host: 'smtp.google.com',
            port: 587,
            secure: true,
            auth: {
                type: 'OAuth2',
                user: ADMIN_EMAIL,
                clientId: process.env.OAUTH_CLIENT_ID,
                clientSecret: process.env.OAUTH_CLIENT_SECRET,
                refreshToken: process.env.OAUTH_REFRESH_TOKEN,
                accessToken: process.env.OAUTH_ACCESS_TOKEN,
            },
        } as SMTPTransport.Options);

        const mailOptions = {
            from: `ModuTrip <${ADMIN_EMAIL}?`,
            to: ADMIN_EMAIL,
            subject: `[문의사항] ${title} `,
            text: `발신자: ${req.body.email}\n\n내용:\n${req.body.content}`,
        };

        await transporter.sendMail(mailOptions);
        console.log('✅ 메일 전송 성공');
    } catch (e) {
        console.error('❌ 메일 전송 실패 원인:', e);
    }
})

export default router;