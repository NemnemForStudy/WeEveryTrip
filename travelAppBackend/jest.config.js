require('dotenv').config({ path: './.env' });

/** @type {import('ts-jest').JestConfigWithTsJest} */
module.exports = {
  preset: 'ts-jest',
  testEnvironment: 'node',
  testMatch: ['**/src/tests/**/*.test.ts'], // 루트 디렉토리 기준으로 테스트 파일 검색
  transform: {
    '^.+\\.ts$': 'ts-jest',
  },
  // setupFiles: ['<rootDir>/src/tests/setup.ts'],
};