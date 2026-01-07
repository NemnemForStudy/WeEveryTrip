import dotenv from 'dotenv';
import express, { Express, Request, Response, NextFunction } from 'express';
import path from 'path'; 
import multer from 'multer';

import db from './db'

// 라우터들을 import 합니다.
import postsRouter from './api/post';
import authRouter from './api/auth';
import routeRouter from './api/route';  // 파일명을 route.ts로 변경했다면 이대로
import mailRouter from './api/sendMail';

db.query('SELECT NOW()', [])
    .then(res => console.log('DB 연결 성공:', res.rows[0]))
    .catch(err => console.error('DB 연결 실패:', err));

// .env 파일 로드
dotenv.config();
console.log("🚀 sendMail 라우터 파일이 로드되었습니다!");

const app: Express = express();
const port = Number(process.env.PORT) || 3000;

// CORS 설정
const allowedOrigins = process.env.ALLOWED_ORIGINS?.split(',') || ['http://localhost:3000'];
app.use((req, res, next) => {
    const origin = req.headers.origin as string;
    if (allowedOrigins.includes(origin)) {
        res.setHeader('Access-Control-Allow-Origin', origin);
    }
    res.setHeader('Access-Control-Allow-Methods', 'GET, POST, PUT, DELETE, OPTIONS');
    res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization');
    res.setHeader('Access-Control-Allow-Credentials', 'true');
    if (req.method === 'OPTIONS') {
        return res.sendStatus(200);
    }
    next();
});

// 보안 헤더 설정
app.use((req, res, next) => {
    res.setHeader('X-Content-Type-Options', 'nosniff');
    res.setHeader('X-Frame-Options', 'DENY');
    res.setHeader('X-XSS-Protection', '1; mode=block');
    res.setHeader('Strict-Transport-Security', 'max-age=31536000; includeSubDomains');
    next();
});

app.get('/debug', (req, res) => {
    res.send("🚀 서버가 최신 코드를 읽고 있습니다!");
});

// JSON 요청 본문을 파싱하기 위한 미들웨어입니다.
app.use(express.json({ limit: '50mb' }));
app.use(express.urlencoded({ limit: '50mb', extended: true }));

// 정적 파일 제공 설정
app.use('/uploads', express.static(path.join(__dirname, '../uploads')));

// 기본 경로 (Root Endpoint)
app.get('/', (req: Request, res: Response) => {
  res.send('여행 앱 백엔드 서버에 오신 것을 환영합니다!');
});

// API 라우터를 등록합니다.
app.use('/api', mailRouter);
app.use('/api/posts', postsRouter);
app.use('/api/auth', authRouter);
app.use('/api/routes', routeRouter);

app.use((err: any, req: Request, res: Response, next: NextFunction) => {
    if (err instanceof multer.MulterError) {
        console.error('🚨 MULTER ERROR:', err.code, err.message);
        return res.status(400).json({ 
            message: `파일 업로드 오류: ${err.code}`, 
            details: err.message 
        });
    }
    console.error('🚨 GLOBAL SERVER ERROR:', err.stack);
    res.status(500).json({ message: '서버 처리 중 알 수 없는 오류 발생' });
});

// 서버를 실행합니다.
app.listen(port, '0.0.0.0', () => {
  console.log("안녕! 테스트중");
  console.log(`서버가 http://0.0.0.0:${port} 에서 실행 중입니다.`);  // 괄호 수정!
});

app.get('/test-db', async (req, res) => {
    try {
        const sql = `
            INSERT INTO "user" (email, nickname, social_provider, social_id)
            VALUES ($1, $2, $3, $4)
            RETURNING *;
        `;
        const values = ['test@example.com', '테스터', 'none', 'test_1234'];
        
        const result = await db.query(sql, values);
        res.json({ success: true, data: result.rows[0] });
    } catch (err) {
        console.error(err);
        const errorMessage = err instanceof Error ? err.message : '알 수 없는 오류 발생';
        res.status(500).send(errorMessage);
    }
});

setInterval(() => {
    console.log('Server is keeping alive...');
}, 3600000);