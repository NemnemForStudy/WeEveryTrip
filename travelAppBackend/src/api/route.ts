import { Request, Response, Router } from 'express';
import axios from 'axios';
import * as dotenv from 'dotenv';
console.log('🔥🔥🔥 route.ts 파일 로드됨! 🔥🔥🔥');
dotenv.config();
const routeRouter = Router();

const NAVER_HEADERS = {
    'Content-Type': 'application/json',
    'X-NCP-APIGW-API-KEY-ID': process.env.NAVER_MAPS_CLIENT_ID,
    'X-NCP-APIGW-API-KEY': process.env.NAVER_MAPS_CLIENT_SECRET
};

if (!process.env.NAVER_MAPS_CLIENT_ID || !process.env.NAVER_MAPS_CLIENT_SECRET) {
    console.error('❌ 경고: 네이버 API 키가 .env 파일에 설정되지 않았습니다.');
}

// 길 찾기 API 엔드포인트
routeRouter.post('/route-for-day', async (req: Request, res: Response) => {
    console.log('/route-for-day 호출됨!');
    const { locations }: { locations: { latitude: number, longitude: number }[] } = req.body;

    if(!locations || locations.length < 2) {
        return res.status(400).json({ message: '최소 2개 이상의 위치 정보가 필요합니다.'});
    }

    const EPS = 1e-6;
    function samePoint(a: any, b: any) {
        return Math.abs(a.latitude - b.latitude) < EPS && Math.abs(a.longitude - b.longitude) < EPS;
    }

    function dedupeConsecutive(points: any[]) {
        const out: any[] = [];
        for(const p of points) {
            if(out.length === 0 || !samePoint(out[out.length - 1], p)) out.push(p);
        }
        return out;
    }

    const cleaned = dedupeConsecutive(locations);

    if(!cleaned || cleaned.length < 2) {
        return res.status(400).json({ message: '최소 2개 이상의 위치 정보가 필요합니다.' });
    }

    // start==goal이면 네이버 호출하지 말고 그냥 “원본 연결선” 리턴(앱에서 그리게)
    if(samePoint(cleaned[0], cleaned[cleaned.length -1])) {
        return res.json({ route: cleaned });
    }

    try {
        // 네이버 API 요청 포맷 준비
        const start = `${cleaned[0].longitude},${cleaned[0].latitude}`;
        const goal = `${cleaned[cleaned.length - 1].longitude},${cleaned[cleaned.length - 1].latitude}`;
        // 경유지
        const waypoints = cleaned.slice(1, -1);
        const waypointsStr = waypoints
            .map(loc => `${loc.longitude},${loc.latitude}`)
            .join('|');

        // 네이버 Directions API 호출
        let apiUrl = `https://maps.apigw.ntruss.com/map-direction-15/v1/driving?start=${start}&goal=${goal}&option=trafast`;

        if(waypoints.length > 0) {
            apiUrl += `&waypoints=${encodeURIComponent(waypointsStr)}`;
        }
        console.log(`🗺️ 경로 요청 URL: ${apiUrl}`);

        // 실제 Axios를 통해 네이버 서버에 요청 보냄
        const response = await axios.get(apiUrl, { headers: NAVER_HEADERS });

        // 응답 데이터 처리 응답 코드가 0이면 성공
        if(response.data.code !== 0) {
            console.error('네이버 API 오류:', response.data.message);
            return res.status(500).json({ message: '경로를 찾울 수 없습니다. (API 오류)'})
        }

        // 응답 데이터에서 Polyline 좌표만 추출해 안드로이드에 전달
        const trafast = response.data.route.trafast;

        if(!trafast || trafast.length === 0) {
            return res.status(404).json({ message: '경로를 찾을 수 없습니다.' });
        }

        const rawPath = trafast[0].path;

        // 네이버 API 경로 정보는 'path' 필드에 (경도, 위도) 순으로 들어있다.
        const path = rawPath.map((point: number[]) => ({
            longitude: point[0],
            latitude: point[1]
        }));

        return res.json({ route: path });
    } catch (error: any) {
        console.error('경로 검색 중 오류 발생');
    
        if (error.response) {
            console.error('상태 코드:', error.response.status);
        } else {
            console.error('에러 메시지:', error.message);
        }
        
        return res.status(500).json({ message: '서버 내부 오류 발생' });
    }
});

export default routeRouter;