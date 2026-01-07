// postgreSQL 연결 풀을 관리한다.
// .env 파일의 환경 변수를 사용해 DB에 연결
import dotenv from 'dotenv';
import { Pool, PoolClient } from 'pg';

// 자동으로 찾아줌.
dotenv.config();

// postgreSQL 연결 풀 생성
// 여러 DB 연결을 효율적으로 관리.
console.log("🚀 DATABASE_URL:", process.env.DATABASE_URL);
const pool = new Pool({
    connectionString: process.env.DATABASE_URL,
    ssl: {
        rejectUnauthorized: false // Supabase 연결 위한 SSL 설정. (필수임)
    }
});

// DB 쿼리 함수
// 파라미터화된 쿼리로 SQL 인젝션 방지
export const query = (text: string, params?: any[]) => {
    return pool.query(text, params);
};

// 트랜잭션 용 클라이언트 획득 함수
// BEGIN, COMMIT, ROLLBACK 쓸 때 사용.
export const getClient = (): Promise<PoolClient> => {
    return pool.connect();
};

export default { query, getClient };