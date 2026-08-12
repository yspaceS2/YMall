# KISA 2026 기술적 취약점 분석·평가 자체 점검

## 목적과 판정 원칙

이 문서는 KISA의 `2026 주요정보통신기반시설 기술적 취약점 분석·평가 방법 상세가이드`를
YMall 코드와 배포 구성에 매핑한 1차 점검 기록이다. 공식 인증이나 모의해킹 결과가 아니며,
정적 코드 확인만으로 단정할 수 없는 항목은 `동적 점검 필요` 또는 `배포 후 점검`으로 남긴다.

판정 상태는 다음과 같다.

- `양호`: 저장소에서 구현과 검증 근거를 확인했다.
- `보완 필요`: 적용 대상이며 현재 구현에 보완할 내용이 확인됐다.
- `동적 점검 필요`: 정적 코드 검토 외에 실행 중인 애플리케이션에 대한 검증이 필요하다.
- `배포 후 점검`: 도메인, TLS, 방화벽, 운영 계정처럼 배포 환경이 정해져야 판정할 수 있다.
- `해당 없음`: YMall 기술 구성에 적용되지 않는다.

기준일은 2026-08-12이며 기준 브랜치는 `develop`이다. 상세 테스트 근거는
[`pre-deployment-security-checklist.md`](./pre-deployment-security-checklist.md), 자동 검사 정책은
[`security-automation.md`](./security-automation.md)를 참조한다.

## 점검 결과 요약

| 영역 | 현재 결과 | 남은 작업 |
| --- | --- | --- |
| 웹 애플리케이션 | 배포 전 대상 항목 양호 | 운영 HTTPS·Cookie·CORS·응답 헤더 재검증 |
| 인증·인가 | 역할·소유권·토큰·비밀번호 정책 검증 완료 | 운영 관리자 접근망 정책 결정 |
| 결제·파일·로그 | 금액 검증, 멱등성, 파일 구조 검증과 로그 축소 완료 | 실제 운영 로그 표본 확인 |
| 자동 보안 검사 | Gitleaks·Semgrep·Trivy·익명 ZAP 구성 완료 | 배포 이미지와 운영 URL 재검사 |
| PostgreSQL | 외부 포트 차단과 역할 분리 구성 완료 | 운영 배포 후 실제 역할·감사 로그 재검증 |
| 컨테이너 | 비루트 실행, 필수 시크릿, 내부 네트워크 적용 | 운영 호스트 OS·방화벽·SSH 점검 |

## Web Application 점검

| 코드 | 점검 항목 | 1차 판정 | YMall 근거와 후속 확인 |
| --- | --- | --- | --- |
| CI | 코드 인젝션 | 양호 | OS 명령 실행과 동적 코드 평가 사용이 확인되지 않았다. 외부 API 주소는 서버 설정으로 주입한다. |
| SI | SQL 인젝션 | 양호 | JPA 바인딩과 `NamedParameterJdbcTemplate`을 사용한다. 문자열 조합 SQL은 확인되지 않았다. |
| DI | 디렉터리 인덱싱 | 양호 | Nginx는 `try_files`로 정적 파일을 제공하며 `autoindex`를 사용하지 않는다. |
| EP | 에러 페이지 적용 미흡 | 동적 점검 필요 | 공통 예외 응답은 존재한다. 처리되지 않은 500 응답에 스택·내부 정보가 노출되지 않는지 운영 프로필로 확인한다. |
| IL | 정보 누출 | 동적 점검 필요 | 운영 SQL 바인딩 로그는 WARN이며 Actuator 상세 정보는 숨긴다. 응답 헤더·소스맵·로그를 실행 환경에서 추가 확인한다. |
| XS | 크로스사이트 스크립트 | 양호 | React 기본 이스케이프를 사용하며 `dangerouslySetInnerHTML` 사용이 확인되지 않았다. CSP도 설정되어 있다. |
| CF | 크로스사이트 요청 위조 | 양호 | JWT API는 무상태이며 Refresh Token은 SameSite 쿠키다. 허용되지 않은 Origin의 Refresh·Logout 요청이 Spring CORS에서 차단되는 것을 통합 테스트로 검증한다. |
| SF | 서버사이드 요청 위조 | 양호 | Toss·AI 호출 대상은 서버 설정의 고정 base URL이며 사용자 입력 URL을 직접 요청하지 않는다. |
| BF | 약한 비밀번호 정책 | 양호 | 2종류 10자 이상 또는 3종류 8자 이상을 요구한다. 취약 문자열, 4자 연속·반복 패턴, 이메일 아이디 유사값과 현재 비밀번호 재사용을 서버에서 차단한다. |
| IA | 불충분한 인증 절차 | 양호 | JWT 서명·만료, Refresh Token 회전·폐기, 역할 변경 시 기존 토큰 무효화를 검증한다. |
| IN | 불충분한 권한 검증 | 양호 | Spring Security 역할·관리자 세부 권한과 서비스의 리소스 소유권 검증 테스트가 있다. |
| PR | 취약한 비밀번호 복구 절차 | 양호 | 일회용 코드·Reset Token 만료, 시도 횟수와 요청 횟수 제한, 계정 존재 여부 비노출을 적용했다. |
| PV | 프로세스 검증 누락 | 양호 | 주문 금액 서버 재계산, 결제·환불·Webhook 상태와 중복 요청을 서버에서 검증한다. |
| FU | 악성 파일 업로드 | 양호 | 크기, 허용 MIME, 파일 시그니처, 이미지 디코딩, WebP 컨테이너와 안전한 저장 경로를 검증한다. |
| FD | 파일 다운로드 | 양호 | 문의 첨부파일 소유권을 검사하고 정규화된 저장 경로가 루트 밖으로 벗어나지 못하게 한다. |
| IS | 불충분한 세션 관리 | 양호 | 짧은 Access Token, 서버 저장 Refresh Token, 회전·로그아웃·권한 변경 무효화를 적용했다. |
| SN | 데이터 평문 전송 | 배포 후 점검 | 운영 도메인에 HTTPS와 TLS 1.2 이상을 적용하고 HTTP를 HTTPS로 강제 전환해야 한다. |
| CC | 쿠키 변조 | 배포 후 점검 | Refresh Token 쿠키는 HttpOnly·SameSite를 사용한다. 운영에서 Secure 적용 여부를 확인한다. |
| AE | 관리자 페이지 노출 | 배포 후 점검 | `/admin`과 `/api/admin` 경로는 예측 가능하지만 익명·비관리자 요청은 백엔드에서 차단한다. 경로 은닉은 보안 통제로 사용하지 않으며 운영 환경에서 추가 인증 또는 접근망 제한을 결정한다. |
| AU | 자동화 공격 | 양호 | 이메일 인증·비밀번호 재설정과 일반 로그인 반복 시도 제한을 적용했다. 로그인 제한 키에는 이메일 원문 대신 SHA-256 해시를 사용한다. |
| WM | 불필요한 Method 악용 | 양호 | GET·HEAD·POST·PUT·PATCH·DELETE·OPTIONS만 허용한다. 백엔드 통합 테스트와 Nginx 설정 검증으로 TRACE·CONNECT·WebDAV Method 차단을 확인한다. |

## PostgreSQL DBMS 점검

| 범위 | 1차 판정 | 내용 |
| --- | --- | --- |
| D-01~D-09 계정 관리 | 동적 점검 필요 | 초기 관리자, Flyway, 애플리케이션, 백업 역할을 분리하고 서로 다른 필수 비밀번호를 사용한다. 운영 배포 후 실제 역할 속성을 재확인한다. |
| D-10 원격 접속 제한 | 양호 | 운영 Compose에서 PostgreSQL 호스트 포트를 노출하지 않고 내부 Docker 네트워크만 사용한다. |
| D-11, D-14, D-18, D-20, D-21 접근·권한 | 양호 | Flyway가 스키마를 소유하고 애플리케이션은 CRUD·시퀀스 사용, 백업은 조회만 허용한다. 격리 PostgreSQL에서 애플리케이션 DDL과 백업 쓰기 거부를 검증했다. |
| D-12, D-15, D-16, D-19, D-23, D-24 | 해당 없음 | Oracle 또는 Microsoft SQL Server 전용 항목이다. |
| D-13 ODBC/OLE-DB | 해당 없음 | YMall은 PostgreSQL JDBC 드라이버를 사용한다. |
| D-25 보안 패치 | 배포 후 점검 | PostgreSQL·컨테이너 이미지의 정기 업데이트와 취약점 스캔 결과를 운영 절차로 확인한다. |
| D-26 감사 로그 | 동적 점검 필요 | 운영 PostgreSQL에 접속·종료와 DDL 기록을 적용했다. 역할 비밀번호 설정 세션은 구문 기록을 중단해 Secret 노출을 방지하며 운영 배포 후 로그 표본을 확인한다. |

## 서버와 컨테이너 점검

- 백엔드와 프론트엔드 런타임 컨테이너는 비루트 사용자로 실행한다.
- 운영 Compose는 PostgreSQL, Redis, Kafka를 호스트에 직접 노출하지 않는다.
- 운영 비밀번호와 암호화 키는 기본값 없이 필수 환경 변수로 받는다.
- 이미지 패키지 업데이트와 고정 digest를 사용하지만, 배포 시점의 이미지 취약점 스캔은 계속 수행해야 한다.
- 호스트 OS 계정, SSH, 파일 권한, 방화벽과 시간 동기화 항목은 배포 인프라가 정해진 후 별도 점검한다.

## 수정 작업 묶음

### 1. 로그인 자동화 공격 방어 (구현 완료)

- 로그인 인증 시도 횟수를 Redis에 제한 시간 동안 기록한다.
- 정규화한 이메일의 SHA-256 해시를 Redis 키로 사용해 개인정보 원문을 남기지 않는다.
- 성공 시 실패 상태를 초기화하고 제한 초과 시 `429 Too Many Requests`를 반환한다.
- 존재하지 않는 계정과 비밀번호 오류가 동일하게 동작하도록 유지한다.
- 단위 테스트와 PostgreSQL·Redis 통합 테스트를 추가한다.

### 2. 관리자 접근과 HTTP 경계 강화 (코드 경계 구현 완료)

- 관리자 추가 인증 또는 운영 접근망 제한 방식을 결정한다.
- TRACE·CONNECT와 WebDAV Method 차단을 통합 테스트로 검증한다.
- Refresh·Logout의 허용되지 않은 Origin 요청 차단을 통합 테스트로 검증한다.

### 3. 운영 DB·TLS 점검 자동화

- PostgreSQL 최소 권한 역할과 감사 로그 정책을 배포 문서 및 반복 실행 가능한 역할 구성 스크립트로 관리한다.
- HTTPS, TLS 1.2 이상, Secure 쿠키와 보안 헤더를 실제 배포 URL에서 검증한다.
- 실제 배포 URL에 ZAP Baseline 또는 Passive Scan을 실행한다.

## 자동·수동 검증 근거

| 구분 | 결과 | 근거 |
| --- | --- | --- |
| 시크릿 검사 | 통과 | Gitleaks 전체 Git 이력 검사 및 redact 출력 |
| 정적 분석 | 통과 | Semgrep Java·TypeScript 보안 규칙, 발견 0건 기준선 |
| 의존성·구성 검사 | 통과 | Trivy repository HIGH·CRITICAL 0건 |
| 컨테이너 이미지 검사 | 통과 | Backend·Frontend 이미지 HIGH·CRITICAL 0건 |
| 익명 동적 검사 | 통과 | ZAP Baseline FAIL 0, 신규 Medium 0, PASS 63 |
| 인증·인가·결제 | 통과 | Backend 통합 테스트와 배포 전 체크리스트 |
| 파일·로그·Compose | 통과 | 공격 표본 테스트, 제한 로그 확인, 운영 Compose 실패·성공 경계 검증 |

관련 작업 기록:

- [PR #132 프론트엔드 High 의존성 취약점 해소](https://github.com/yspaceS2/YMall/pull/132)
- [PR #133 Semgrep 및 Trivy 보안 검사 자동화](https://github.com/yspaceS2/YMall/pull/133)
- [PR #134 OWASP ZAP 동적 보안 검사 추가](https://github.com/yspaceS2/YMall/pull/134)
- [PR #135 관리자 권한 경계 보안 검증 보강](https://github.com/yspaceS2/YMall/pull/135)
- [PR #136 배포 전 보안 검증 강화](https://github.com/yspaceS2/YMall/pull/136)
- [PR #137 로그인 반복 시도 제한 적용](https://github.com/yspaceS2/YMall/pull/137)
- [PR #138 HTTP 보안 경계 강화](https://github.com/yspaceS2/YMall/pull/138)
- [PR #139 KISA 비밀번호 정책 적용](https://github.com/yspaceS2/YMall/pull/139)
- [Jira YMALL-79](https://yspace-labs.atlassian.net/browse/YMALL-79)

## 운영 배포 후 점검 결과

2026-08-11 `https://ymall.cloud`를 대상으로 공개 응답과 비파괴적인 ZAP Passive Baseline을
점검했습니다. 이 검사는 공격 페이로드를 사용하는 Active Scan이 아니며 운영 데이터를
변경하지 않습니다.

| 점검 항목 | 결과 | 근거와 후속 조치 |
| --- | --- | --- |
| HTTPS와 강제 전환 | 양호 | HTTP 요청은 HTTPS로 `308 Permanent Redirect`되며 HSTS `max-age=31536000; includeSubDomains`가 적용된다. |
| 보안 응답 헤더 | 양호 | CSP, COOP, CORP, Permissions Policy, Referrer Policy와 `X-Content-Type-Options`를 확인했다. Backend API와 이미지에도 CORP `same-origin` 적용을 확인했다. |
| ZAP Passive Baseline | 양호(예외 2종) | High 0건, Medium 0건, Low 2종, Informational 5종이다. Low 2종은 외부 로그인·결제 호환성을 위한 COEP·COOP 예외다. |

COEP `require-corp`는 Google One Tap, OAuth와 Toss 결제에 필요한 교차 출처 자원의 로딩을
차단할 수 있어 강제하지 않습니다. COOP는 팝업 로그인 완료 흐름을 위해
`same-origin-allow-popups`를 유지합니다. 두 항목은 제거가 아니라 호환성과 위험을 검토한
예외이며, ZAP 90004 경고의 정기 재검토 대상으로 관리합니다. CORP는 동일 출처에서만 API와
이미지를 소비하는 운영 구조에 맞춰 Backend 응답에도 `same-origin`을 적용합니다.

CORP 적용 후 Passive Scan 재검사에서 기존 CORP Low 경고가 제거됐습니다. Caddyfile은 단일 파일
bind mount이므로 Git checkout이 파일 inode를 교체해도 실행 중 컨테이너가 이전 파일을 계속 볼 수
있습니다. 배포 스크립트는 매 배포와 롤백 시 Caddy 컨테이너를 강제 재생성해 현재 저장소의 설정을
확실히 읽도록 구성합니다.

## 배포 전 결론

저장소와 로컬 격리 환경에서 판정 가능한 배포 전 항목은 검증됐다. 현재 확인된 코드 수준의
HIGH·CRITICAL 취약점이나 미조치 보안 결함은 없다. 다만 PostgreSQL 감사 로그는 운영 구성
작업으로 남아 있으며 아래 항목은 실제 배포 환경에서 증적을 남긴 뒤 최종 판정한다.

- ~~운영 HTTPS/TLS와 인증서, HTTP 강제 전환~~ (2026-08-11 확인 완료)
- Refresh Token Cookie의 실제 `Secure`·`HttpOnly`·`SameSite` 값
- ~~운영 CSP·COOP·CORP·Permissions Policy 응답~~ (2026-08-11 확인 완료)
- 운영 CORS 허용 출처
- PostgreSQL·Redis·Kafka·Actuator·모니터링 포트의 외부 노출 여부
- 운영 PostgreSQL 최소 권한 역할과 감사 로그
- 호스트 OS 계정, SSH, 방화벽, 파일 권한과 시간 동기화
- ~~실제 배포 URL에 대한 ZAP Baseline 또는 Passive Scan~~ (2026-08-11 CORP 보완 후 재검증 완료)

위 항목을 배포 후 갱신한 뒤 YMALL-79를 완료한다.
