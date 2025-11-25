// .env 파일의 환경 변수 로드함.
// .env 파일이 상위 폴더에 있으므로 경로 지정
require('dotenv').config({ path: '../.env' });

const { Pool } = require('pg');

// PostgreSQL 연결 풀 생성
const pool = new Pool({
    user: process.env.DB_USER,
    host: process.env.DB_HOST,
    database: process.env.DB_DATABASE,
    password: process.env.DB_PASSWORD,
    port: process.env.DB_PORT,
})

// 다른 파일에서 pool 가져와 쿼리 실행
module.exports = {
    query: (text, params) => pool.query(text, params),
    getClient: () => pool.connect(),
}