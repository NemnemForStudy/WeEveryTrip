// .env 파일의 환경 변수를 로드합니다.
require('dotenv').config();

const express = require('express');
const { Pool } = require('pg');

const app = express();
const port = 3000;

// PostgreSQL 연결 풀(Pool) 생성
const pool = new Pool({
  user: process.env.DB_USER,
  host: process.env.DB_HOST,
  database: process.env.DB_DATABASE,
  password: process.env.DB_PASSWORD,
  port: process.env.DB_PORT,
});

// 기본 API
app.get('/', (req, res) => {
  res.send('여행 앱 백엔드 서버에 오신 것을 환영합니다!');
});

// 데이터베이스 연결 테스트를 위한 API
app.get('/test-db', async (req, res) => {
  let client;
  try {
    // 커넥션 풀에서 클라이언트(연결 객체)를 하나 가져옵니다.
    client = await pool.connect();
    console.log("PostgreSQL 데이터베이스에 성공적으로 연결되었습니다!");

    // 간단한 쿼리를 실행하여 현재 시간을 가져옵니다.
    const result = await client.query('SELECT NOW()');
    res.status(200).send(`데이터베이스 현재 시간: ${result.rows[0].now}`);

  } catch (err) {
    console.error('데이터베이스 연결 중 오류 발생:', err.stack);
    res.status(500).send('데이터베이스 연결에 실패했습니다.');

  } finally {
    // 사용이 끝난 클라이언트는 반드시 반환하여 다른 요청이 사용할 수 있도록 합니다.
    if (client) {
      client.release();
    }
  }
});

// 서버 실행
app.listen(port, () => {
  console.log(`서버가 http://localhost:${port} 에서 실행 중입니다.`);
  console.log('데이터베이스 연결을 테스트하려면 http://localhost:3000/test-db 로 접속하세요.');
});
