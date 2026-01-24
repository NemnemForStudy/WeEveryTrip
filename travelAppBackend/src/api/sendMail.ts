import express, { Request, Response } from 'express';
import nodemailer from 'nodemailer';

const router = express.Router();

// 보안 위해 환경변수 권장.
const ADMIN_EMAIL = process.env.ADMIN_EMAIL;
const ADMIN_PASSWORD = process.env.EMAIL_PASS;

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

    const transporter = nodemailer.createTransport({
        service: 'gmail',
        auth: {
            user: ADMIN_EMAIL,
            pass: ADMIN_PASSWORD
        },
    });

    const mailOptions = {
        from: `ModuTrip APP <${ADMIN_EMAIL}>`,
        to: ADMIN_EMAIL,
        subject: `[문의사항] ${title}`,
        text: `발신자: ${email}\n\n내용:\n${content}`,
    };

    // promise chain으로 비동기 처리
    transporter.sendMail(mailOptions)
        .then(() => {
            console.log(`✅ [Background] 메일 전송 완료: ${title}`);
        })
        .catch((error) => {
            console.error('🚨 [Background] 메일 전송 실패:', error);
            // 여기서 DB에 '발송 실패' 로그를 남기거나 개발자에게 따로 알림을 줄 수도 있습니다.
        }); 
})

export default router;