# 시간대 운영 기준

YMall은 서버·DB 내부 시간을 UTC로 통일하고 사용자 화면에서만 한국 시간으로 변환합니다.

## 기준

- JVM 기본 시간대: `UTC`
- 애플리케이션 `Clock`: `Clock.systemUTC()`
- Hibernate JDBC 시간대: `UTC`
- PostgreSQL 세션 시간대: `UTC`
- API 날짜·시간: ISO-8601과 명시적 UTC offset(`Z`)
- 사용자 화면: `Asia/Seoul`

기존 엔티티의 `LocalDateTime` 필드는 UTC wall-clock 값으로 취급합니다. API 직렬화 시 UTC
offset을 붙이므로 브라우저가 로컬 시간으로 오해하지 않습니다. 신규 도메인에서 절대적인 순간을
저장할 때는 가능하면 `Instant`를 사용합니다.

## 로컬 실행

IntelliJ에서 실행해도 `TimeConfig`가 JVM 기본 시간대와 애플리케이션 `Clock`을 UTC로 맞춥니다.
현재 시간을 사용하는 예약·TTL·만료 로직은 `Clock`을 주입받아야 하며 직접 시스템 시간에
의존하지 않습니다.

Docker 이미지와 Compose에도 다음 설정이 적용됩니다.

```text
TZ=UTC
JAVA_TOOL_OPTIONS=-Duser.timezone=UTC
```

PostgreSQL은 `timezone=UTC` 서버 옵션으로 실행됩니다. 다음 명령으로 확인할 수 있습니다.

```bash
docker compose exec postgres psql -U ymall_user -d ymall -c "SHOW timezone;"
docker compose exec backend date
```

## API와 프런트엔드

API의 `LocalDateTime` 값은 다음처럼 UTC offset을 포함합니다.

```json
{
  "createdAt": "2026-07-26T12:00:00Z"
}
```

프런트엔드는 `src/utils/dateTime.ts`의 공통 formatter를 사용해 `Asia/Seoul` 기준으로
표시합니다. 화면 컴포넌트에서 `toLocaleString()`을 직접 호출하지 않습니다.

## 테스트 기준

- 고정된 `Clock`으로 만료 cutoff를 검증합니다.
- UTC 자정과 한국 날짜 경계를 검증합니다.
- API 직렬화 결과에 `Z`가 포함되는지 검증합니다.
- 로컬과 Docker에서 같은 UTC 입력이 같은 한국 시간으로 표시되는지 확인합니다.
