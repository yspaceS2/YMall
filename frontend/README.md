# YMall Frontend

Vite, React, TypeScript, Tailwind CSS 기반 YMall 프런트엔드입니다.

## 로컬 실행

```bash
npm ci
npm run dev
```

기본 개발 서버는 `http://localhost:5173`에서 실행됩니다.

## 검증 명령

```bash
npm run test
npm run lint
npm run build
```

- `npm run test`: Vitest와 React Testing Library 컴포넌트 테스트
- `npm run lint`: ESLint 정적 검사
- `npm run build`: TypeScript 검사 및 Vite production build

## E2E 테스트

최초 한 번 Playwright Chromium을 설치합니다.

```bash
npx playwright install chromium
npm run test:e2e
```

E2E 테스트는 전용 Vite 서버를 `http://127.0.0.1:4173`에 띄운 뒤 자동으로 종료합니다.
백엔드, OAuth 공급자, Toss Payments에는 접속하지 않으며 Playwright의 네트워크 가짜
응답을 사용합니다. 따라서 실제 계정, 결제 키, 개인정보가 필요하지 않습니다.

검증 범위는 다음과 같습니다.

- 이메일 중복 확인, 회원가입, 로그인
- 일반 사용자·판매자·관리자 역할별 메뉴
- 상품 상세, 장바구니, 주문 생성, 결제 화면 진입
- 알림 미읽음 배지, 개별 읽음, 모두 읽음

실패 시 `test-results`와 `playwright-report`에 스크린샷과 trace가 생성됩니다.
GitHub Actions에서도 같은 명령을 실행하며 실패 결과를 artifact로 7일간 보관합니다.
