// .env 파일의 환경 변수를 로드합니다.
require('dotenv').config();

const express = require('express');
const app = express();
const port = 3000;

// API 라우터 불러옴
const postsRouter = require('./src/api/post');

// auth 라우터 불러오기
const authRouter = require('./src/api/auth');

// JSON 요청 본물을 파싱하기 위한 미들웨어
// JSON 요청 본문의 크기 제한을 50mb로 늘립니다.
app.use(express.json({ limit: '50mb' }));
app.use(express.urlencoded({ limit: '50mb', extended: true }));

// 기본 API
app.get('/', (req, res) => {
  res.send('여행 앱 백엔드 서버에 오신 것을 환영합니다!');
});

// '/api/posts' 경로로 들어오는 요청은 postsRouter가 처리함.
app.use('/api/posts', postsRouter);
app.use('/api/auth', authRouter);

// 서버 실행
app.listen(port, () => {
  console.log(`서버가 http://0.0.0.0:${port} 에서 실행 중입니다.`);
  console.log('데이터베이스 연결을 테스트하려면 http://localhost:3000/test-db 로 접속하세요.');
});
