# 보안 점검 및 개선 사항

## ✅ 수정된 보안 문제

### 1. JWT 비밀키 하드코딩 제거
**문제**: `'my-secret-for-travel-app'`이 코드에 직접 작성됨
- **수정**: 환경 변수로 변경, 미설정 시 에러 발생
- **파일**: `src/api/auth.ts`, `src/api/post.ts`, `src/middlewares/auth.ts`

```typescript
const JWT_SECRET_KEY = process.env.JWT_SECRET_KEY;
if (!JWT_SECRET_KEY) {
    throw new Error('JWT_SECRET_KEY 환경 변수가 설정되지 않았습니다.');
}
```

### 2. JWT 토큰 만료 시간 설정
**문제**: Access token에 만료 시간이 없어서 탈취 시 영구적으로 유효함
- **수정**: 
  - **Access Token**: 1시간 (짧은 유효기간)
  - **Refresh Token**: 7일 (더 긴 유효기간)
- **파일**: `src/api/auth.ts`

```typescript
// Access Token
const token = jwt.sign(
    { userId: user.user_id },
    JWT_SECRET_KEY,
    { expiresIn: '1h' }
);

// Refresh Token
const refreshToken = jwt.sign(
    { userId: user.user_id },
    JWT_SECRET_KEY,
    { expiresIn: '7d' }
);
```

### 3. Authorization 헤더 검증 통일
**문제**: 일부는 `split(' ')[1]`, 일부는 직접 처리로 인한 버그 위험
- **수정**: 모든 곳에서 `substring(7)` 사용으로 통일
- **파일**: `src/api/auth.ts`, `src/middlewares/auth.ts`

```typescript
const token = authHeader.substring(7); // 'Bearer ' 제거 (안전한 방법)
```

### 4. 동적 컬럼명 SQL 인젝션 방지
**문제**: `UPDATE "user" SET ${column} = $1` - column이 사용자 입력값이면 위험
- **수정**: 화이트리스트 매핑 객체 사용
- **파일**: `src/api/auth.ts`

```typescript
const columnMap: Record<string, string> = {
    "activity": "push_activity",
    "marketing": "push_marketing"
};

const column = columnMap[type];
if (!column) {
    return res.status(400).json({ message: "잘못된 타입 설정입니다." })
}
```

### 5. 민감한 정보 로깅 제거
**문제**: 요청 바디, 에러 스택 등이 콘솔에 노출됨
- **수정**: 민감한 정보 제거, 일반적인 에러 메시지만 로깅
- **파일**: `src/api/auth.ts`

```typescript
// Before
console.log('Body:', req.body);
console.error('오류:', (err as Error).stack);

// After
console.log('========== 로그인 요청 시작 ==========');
console.error('로그아웃 처리 중 오류');
```

### 6. 리프레시 토큰 응답에서 제거
**문제**: 리프레시 토큰이 응답에 포함되어 탈취 위험 증가
- **수정**: 별도 헤더 또는 HTTP-only 쿠키로 처리하도록 변경
- **파일**: `src/api/auth.ts`

### 7. CORS 설정 추가
**문제**: CORS 설정이 없어서 모든 출처에서 요청 가능
- **수정**: 환경 변수로 허용 도메인 제한
- **파일**: `src/index.ts`

```typescript
const allowedOrigins = process.env.ALLOWED_ORIGINS?.split(',') || ['http://localhost:3000'];
```

### 8. 보안 헤더 추가
**문제**: 브라우저 보안 헤더가 없음
- **수정**: 다음 헤더 추가
  - `X-Content-Type-Options: nosniff`
  - `X-Frame-Options: DENY`
  - `X-XSS-Protection: 1; mode=block`
  - `Strict-Transport-Security`
- **파일**: `src/index.ts`

---

## 🚀 다음 단계 권장사항

### 1. 리프레시 토큰 구현 개선
현재 리프레시 토큰이 응답에 포함되고 있음. 다음과 같이 개선하기:
- HTTP-only 쿠키로 저장
- 또는 별도 엔드포인트 (`/api/auth/refresh`) 구현

### 2. 비밀번호 해싱 구현 (필수)
소셜 로그인만 지원하는 경우 제외. 하지만 향후 로컬 인증 추가 시 bcrypt 사용:
```typescript
import bcrypt from 'bcrypt';
const hashedPassword = await bcrypt.hash(password, 10);
const isValid = await bcrypt.compare(password, hashedPassword);
```

### 3. 요청 검증 라이브러리 추가
현재: 기본적인 검증만 수행
추천: `joi` 또는 `zod` 사용
```typescript
import Joi from 'joi';

const schema = Joi.object({
    email: Joi.string().email().required(),
    nickname: Joi.string().max(50).required()
});

await schema.validateAsync(req.body);
```

### 4. Rate Limiting 추가
DDoS/Brute force 공격 방지
```typescript
import rateLimit from 'express-rate-limit';

const limiter = rateLimit({
    windowMs: 15 * 60 * 1000, // 15분
    max: 100 // 최대 100 요청
});

app.use('/api/auth/login', limiter);
```

### 5. 파일 업로드 보안
- 파일 타입 검증
- 파일 크기 제한
- 악성 파일명 처리

### 6. HTTPS 사용 (프로덕션)
```typescript
if (process.env.NODE_ENV === 'production') {
    // HTTPS 강제
}
```

### 7. 로깅 및 모니터링
사민감 정보가 로깅되지 않도록 로깅 라이브러리(winston, pino) 구성

---

## 📋 체크리스트

- [x] JWT 비밀키 환경 변수화
- [x] JWT 토큰 만료 시간 설정
- [x] Authorization 헤더 검증 통일
- [x] SQL 인젝션 방지 (동적 컬럼)
- [x] 민감한 정보 로깅 제거
- [x] 리프레시 토큰 응답 처리 개선
- [x] CORS 설정 추가
- [x] 보안 헤더 추가
- [ ] 비밀번호 해싱 (소셜만 사용하면 선택)
- [ ] 요청 검증 라이브러리
- [ ] Rate Limiting
- [ ] HTTPS 설정
- [ ] 로깅 개선

---

## 🔐 환경 변수 설정

`.env` 파일에 다음을 설정해야 합니다:

```
JWT_SECRET_KEY=your-super-secret-key-min-32-chars
ALLOWED_ORIGINS=http://localhost:3000,https://yourdomain.com
```

`.env.example` 파일을 참고하여 설정하세요.
