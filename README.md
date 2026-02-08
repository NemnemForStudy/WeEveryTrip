# ModuTrip - 사진으로 기록하는 여행 경로 지도
우리 모두의 여행 어플

🗺️ ModuTrip (모두트립)
사진 한 장으로 완성되는 나만의 여행 경로 기록 서비스
<p align="left"> <img src="https://img.shields.io/badge/Kotlin-7F52FF?style=flat-square&logo=Kotlin&logoColor=white"/> <img src="https://img.shields.io/badge/Jetpack%20Compose-4285F4?style=flat-square&logo=JetpackCompose&logoColor=white"/> <img src="https://img.shields.io/badge/Dagger%20Hilt-AECBFA?style=flat-square&logo=Google&logoColor=black"/> <img src="https://img.shields.io/badge/Node.js-339933?style=flat-square&logo=Node.js&logoColor=white"/> <img src="https://img.shields.io/badge/TypeScript-3178C6?style=flat-square&logo=TypeScript&logoColor=white"/> <img src="https://img.shields.io/badge/PostgreSQL-4169E1?style=flat-square&logo=PostgreSQL&logoColor=white"/> <img src="https://img.shields.io/badge/Supabase-3ECF8E?style=flat-square&logo=Supabase&logoColor=white"/> </p>

🚀 서비스 개요
기획 배경: 여행 후 사진을 정리하며 당시의 경로를 다시 떠올리기 어렵다는 불편함에서 시작되었습니다.

핵심 기능: 업로드된 사진의 Exif(위경도 데이터)를 분석하여 지도 위에 자동으로 여행 루트를 그려주고 기록합니다.

현재 상태: Google Play Store 정식 출시 완료 (2026.02.07)

🛠️ Tech Stack
Client (Android)
Language: Kotlin

UI: Jetpack Compose (Declarative UI)

Architecture: MVVM, Clean Architecture, Dagger Hilt (DI)

Network: Retrofit2, OkHttp3

Library: Naver Map SDK, Coil (Image Loading)

Backend
Environment: Node.js, Express, TypeScript

Database: PostgreSQL (Supabase managed)

Infrastructure: Render (PaaS)

💡 트러블 슈팅
1. Jetpack Compose와 Native Map 간의 터치 간섭 해결
문제: 본문 섹션의 오프셋 영역이 지도의 하단 터치 이벤트를 차단하여 줌 버튼이 작동하지 않는 현상 발생.

해결: 지도를 감싸는 레이어에 **zIndex(1f)**를 부여하고, **contentPadding**을 통해 UI 요소의 물리적 배치를 최적화하여 터치 영역을 확보했습니다.

2. 카메라 제어 로직 충돌 최적화
문제: 화면 진입 시 전체 경로를 보여주는 로직과 현재 마커를 추적하는 카메라 업데이트가 동시에 실행되어 뷰가 충돌하는 문제.

해결: isFirstLoad 플래그를 도입하여 초기 렌더링 시에는 전체 경로를 보여주는 fitBounds 명령에 우선순위를 부여하도록 제어했습니다.

🔗 Links
Play Store : https://play.google.com/store/apps/details?id=com.nemnem.travelapp
