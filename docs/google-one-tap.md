# Google One Tap

YMall의 Google One Tap은 브라우저가 받은 Google ID token을 그대로 신뢰하지 않습니다.
백엔드가 Google 공개키 서명, issuer, audience, 만료, 이메일 검증 여부와 일회용 nonce를
확인한 뒤 `provider + providerId(sub)`로 기존 소셜 계정을 조회합니다.

## 로컬 환경 변수

Backend의 `application-local.yaml`에는 기존 Google OAuth 설정과 동일한 Web Client의
ID와 Secret을 둡니다.

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: ${GOOGLE_CLIENT_ID}
            client-secret: ${GOOGLE_CLIENT_SECRET}
```

Frontend는 nonce 발급 응답을 통해 Backend가 사용하는 공개 Web Client ID를 전달받습니다.
따라서 Frontend 환경 파일이나 Docker 빌드 인자에 Client ID를 중복 설정하지 않습니다.
Google Client Secret은 응답이나 `VITE_` 변수에 포함하지 않습니다.

## Google Cloud Console 설정

OAuth 2.0 Web Client의 승인된 JavaScript 원본에 로컬 Frontend 주소를 등록합니다.

```text
http://localhost:5173
```

기존 OAuth 리다이렉션 로그인도 함께 사용할 경우 승인된 리디렉션 URI는 별도로 유지합니다.

```text
http://localhost:8080/login/oauth2/code/google
```

배포 환경에서는 실제 HTTPS Frontend 원본과 Backend OAuth callback URI로 교체합니다.

## 처리 흐름

1. Frontend가 Backend에서 5분 TTL의 일회용 nonce를 발급받습니다.
2. Google Identity Services를 해당 nonce와 Web Client ID로 초기화합니다.
3. Google이 ID token을 callback으로 전달합니다.
4. Frontend는 ID token을 Backend에 전달하며 직접 해석하거나 신뢰하지 않습니다.
5. Backend가 서명, issuer, audience, expiry, `email_verified`, nonce를 검증합니다.
6. 기존 Google 계정이면 YMall access token과 refresh-token cookie를 발급합니다.
7. 신규 Google 계정이면 기존 소셜 추가정보 입력 화면으로 이동합니다.

사용자가 One Tap을 닫거나 브라우저가 표시하지 못해도 기존 이메일 로그인과 Google OAuth
버튼은 계속 사용할 수 있습니다.
