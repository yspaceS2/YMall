# PostgreSQL 통합 테스트

## 목적

YMall은 빠른 테스트와 운영 데이터베이스 정합성 검증을 분리한다. 일반 테스트는 H2의
PostgreSQL 호환 모드를 사용하고, SQL 문법·제약조건·잠금·동시성에 민감한 테스트는
Testcontainers가 기동한 PostgreSQL 16에서 실행한다.

Testcontainers 테스트는 개발용 또는 운영 데이터베이스에 연결하지 않는다. 컨테이너는
테스트 JVM이 시작한 임시 데이터베이스이며 테스트 종료 시 제거된다.

## 실행 계층

| Gradle task | 데이터베이스 | 대상 |
| --- | --- | --- |
| `test` | H2 인메모리 | 단위, Controller, 일반 통합, 외부 어댑터 계약 테스트 |
| `postgresTest` | PostgreSQL 16 Testcontainer | Flyway, 동시성·잠금, Outbox, 정산, 큐레이션 쿼리 |

Windows:

```powershell
cd backend
.\gradlew.bat test postgresTest --no-daemon
```

Linux와 GitHub Actions:

```shell
cd backend
./gradlew test postgresTest --no-daemon
```

`postgresTest`를 실행하려면 Docker 호환 컨테이너 런타임이 실행 중이어야 한다. 별도의
PostgreSQL URL, 계정 또는 비밀번호 환경변수는 필요하지 않다.

## PostgreSQL 테스트 기반

`PostgresIntegrationTestSupport`를 상속한 테스트는 다음 정책을 공유한다.

- `postgres:16-alpine` 이미지를 테스트 JVM당 한 번 기동한다.
- `@DynamicPropertySource`로 임시 JDBC 연결 정보를 Spring에 주입한다.
- Flyway가 운영 migration을 빈 데이터베이스에 적용한다.
- Hibernate는 `ddl-auto=validate`로 migration 결과와 Entity 매핑을 검증한다.
- 각 테스트 전에 application table을 `TRUNCATE ... RESTART IDENTITY CASCADE`로 비운다.
- `flyway_schema_history`는 유지하여 같은 JVM에서 migration을 반복 실행하지 않는다.

트랜잭션 테스트에서도 정리 작업은 테스트 트랜잭션 시작 전에 실행되며, 다른 테스트가 만든
데이터를 관찰하지 않도록 격리한다. 테스트 데이터에는 실제 개인정보나 운영 자격 증명을
사용하지 않는다.

## 현재 PostgreSQL 전환 범위

- 빈 PostgreSQL의 Flyway baseline 및 전체 migration 검증
- 주문 생성 멱등성과 재고 동시성
- 결제 승인, 환불, 환불 실패 복구 및 웹훅 동시성
- 관리자 역할 변경 동시성
- Transactional Outbox 저장·잠금·재처리
- 정산 원장 및 수시 정산 집계
- 홈 상품 큐레이션 집계 쿼리
- 회원 이메일 및 소셜 계정 고유 제약

Redis cache 장애·fallback 테스트와 Kafka DLT 전달 테스트는 데이터베이스 전용 동작이
핵심이 아니므로 H2 계층에 유지한다.

## CI

GitHub Actions의 Backend Test job은 로컬과 동일하게 `test postgresTest jacocoTestReport`를 실행한다.
PostgreSQL service container와 별도 접속 환경변수는 사용하지 않는다. 실패 시 Gradle HTML
report와 XML test result를 artifact로 7일간 보존하며, 컨테이너 로그에 시크릿이나 실제
개인정보를 기록하지 않는다.

## 커버리지 확인

Backend 커버리지는 H2 단위 테스트와 PostgreSQL Testcontainers 통합 테스트의 실행 데이터를 합쳐 JaCoCo로 생성한다.

```powershell
cd backend
.\gradlew.bat test postgresTest jacocoTestReport
```

로컬 HTML 보고서는 `backend/build/reports/jacoco/test/html/index.html`에서 확인한다. GitHub Actions에서는 `backend-coverage-report` artifact를 내려받아 같은 HTML 보고서와 XML 원본을 확인할 수 있으며 14일간 보관한다. 커버리지는 테스트 존재 여부를 확인하는 보조 지표이며, 결제 금액·권한·멱등성·동시성처럼 위험이 큰 정책의 실패 시나리오를 우선한다.

Frontend는 CI에서 `npm run test:coverage`를 실행하고 `frontend-coverage-report` artifact를 14일간 보관한다. 로컬 결과는 `frontend/coverage/index.html`에서 확인한다.

## 실행 시간 기준

2026-08-04 Windows 로컬 실행 기준이며 캐시와 장비 상태에 따라 달라질 수 있다.

| 구성 | 시간 | 결과 |
| --- | ---: | --- |
| 전환 전 전체 H2 `test` | 약 3분 24초 | 통과 |
| 분리 후 H2 `test` | 약 3분 2초 | 통과 |
| PostgreSQL `postgresTest` | 약 2분 | 24개 통과 |

PostgreSQL 계층은 실행 시간이 추가되지만 H2에서 발견되지 않았던 nullable 시간 조건의
PostgreSQL parameter type 오류를 실제로 검출했다.
