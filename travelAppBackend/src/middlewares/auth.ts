import { Request, Response, NextFunction } from 'express';
import jwt, { TokenExpiredError } from 'jsonwebtoken';

export const authMiddleware = (req: Request, res: Response, next: NextFunction) => {
    const authHeader = req.headers.authorization;
    
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
        return res.status(401).json({ message: '인증 토큰 누락'});
    }

    const token = authHeader.substring(7); // 'Bearer ' 제거

    try {
        const JWT_SECRET_KEY = process.env.JWT_SECRET_KEY;
        if (!JWT_SECRET_KEY) {
            throw new Error('JWT_SECRET_KEY 환경 변수가 설정되지 않았습니다.');
        }
        
        const decoded = jwt.verify(token, JWT_SECRET_KEY) as { userId: number };

        // 사용자 정보 추가해 다음 라우터 전달
        (req as any).user = { id: decoded.userId };

        next();
    } catch(error) {
        // 안드로이드 Authenticator가 401 코드를 감지할 수 있도록 에러 분기 처리
        if (error instanceof TokenExpiredError) {
            return res.status(401).json({ message: '토큰이 만료되었습니다.' });
        }
        // 그 외의 모든 유효하지 않은 토큰 에러는 403으로 처리
        return res.status(403).json({ message: '유효하지 않은 토큰' });
    }
}