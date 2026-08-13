# YMall API 개요

## 공통 규칙

- 기본 경로는 `/api`, 인증은 `Authorization: Bearer {accessToken}`입니다.
- JSON은 `camelCase`, 날짜·시간은 ISO-8601 형식을 사용합니다.
- 공통 성공·오류 응답을 사용하고 목록은 필요한 경우 페이지 정보를 포함합니다.
- 주문·결제처럼 중복에 민감한 API는 멱등키와 서버 상태를 함께 검증합니다.

| 영역 | 대표 경로 | 주요 기능 |
| --- | --- | --- |
| 회원·인증 | `/api/members` | 가입, 로그인, Token 갱신, 이메일·비밀번호 변경 |
| OAuth 계정 | `/oauth2`, `/api/members/oauth2`, `/api/members/me/oauth-accounts` | Google One Tap, Google·Naver·Kakao 로그인과 계정 연결 |
| 상품 | `/api/products`, `/api/categories` | 검색·상세, 3단계 카테고리 |
| 홈 | `/api/home` | 판매량·최근 판매·승인 시각 기반 상품 큐레이션 |
| 장바구니·관심상품 | `/api/cart`, `/api/wishlist` | 항목 추가·변경·삭제 |
| 주문·결제 | `/api/orders`, `/api/payments` | 주문, 반품, Toss 승인·취소·환불·웹훅 |
| 리뷰·문의 | `/api/reviews`, `/api/product-questions` | 구매 리뷰, 상품 질문·답변 |
| 파일 | `/api/files` | 목적·소유권·파일 형식을 검증한 업로드와 조회 |
| 알림·지원 | `/api/notifications`, `/api/support` | 알림, 문의, 실시간 상담 |
| 판매자 | `/api/seller` | 상품·주문·반품·정산·대시보드 |
| 관리자 | `/api/admin` | 회원·판매자·상품·주문·정산·권한 운영 |

리뷰 조회 응답에는 상품별 평점과 리뷰 수를 제공하며, `/api/products/{productId}/review-summary`에서 저장된 AI 요약을 조회합니다. 요약 생성은 리뷰 변경 요청과 분리된 비동기 흐름이므로 API는 생성 완료를 기다리지 않습니다.

## 권한 모델

`ROLE_USER`는 본인의 주문·리뷰·알림, `ROLE_SELLER`는 자신의 상품·판매 주문·정산, `ROLE_ADMIN`은 승인된 운영 API에 접근합니다. Frontend 메뉴 숨김은 권한 기준이 아니며 Backend가 역할과 소유권을 최종 검증합니다.

## 변경 체크리스트

1. Request DTO의 필수값·길이·범위·형식을 검증합니다.
2. Entity 대신 필요한 필드만 담은 Response DTO를 반환합니다.
3. 역할과 사용자·판매자 소유권을 확인합니다.
4. 금액, 재고, 상태는 서버 데이터로 다시 계산·검증합니다.
5. API 계약 테스트와 관련 Backend·Frontend 테스트를 갱신합니다.

전체 Endpoint의 기준은 각 Controller이며, 결제 정책은 [Toss Payments](toss-payments.md)와 [웹훅 운영](payment-webhooks.md)을 참고합니다.
