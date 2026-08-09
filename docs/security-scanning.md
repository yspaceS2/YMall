# 보안 자동 검사

YMall은 GitHub Actions와 GitHub Dependabot을 이용해 시크릿과 취약 의존성을 지속적으로 확인합니다.

이 문서는 시크릿·Dependabot 운영 절차의 진입점입니다. 검사 실행 시점과 실패 기준은
[`security/security-automation.md`](./security/security-automation.md), 인증·권한·결제·파일 검증은
[`security/pre-deployment-security-checklist.md`](./security/pre-deployment-security-checklist.md),
KISA 항목별 판정은
[`security/kisa-2026-assessment.md`](./security/kisa-2026-assessment.md)를 기준으로 관리합니다.

## 시크릿 검사

`.github/workflows/security.yml`의 `Secret Scan` 작업은 다음 시점에 실행됩니다.

- `develop`, `main` 대상 Pull Request
- `develop`, `main` Push
- GitHub Actions의 수동 실행

Checkout 단계에서 `fetch-depth: 0`을 사용하므로 현재 파일뿐 아니라 전체 Git 기록을 검사합니다. Gitleaks 기본 규칙으로 GitHub 토큰, OAuth 시크릿, JWT, 개인키와 일반 API 키를 검사하고, `.gitleaks.toml`의 추가 규칙으로 Toss Payments 시크릿 키 형식을 검사합니다.

검출 결과에 실제 시크릿 값이 남지 않도록 결과 아티팩트 업로드를 비활성화하고 Gitleaks의 Redact 출력을 사용합니다.

## 의존성 검사

GitHub Dependency Graph와 Dependabot은 다음 의존성을 확인합니다.

- Frontend npm: `frontend/package-lock.json`에서 직접·전이 의존성을 인식합니다.
- Backend Gradle: `.github/workflows/dependency-submission.yml`이 기본 브랜치인 `main` Push 후 해석된 의존성 그래프를 제출합니다. GitHub Dependency Graph는 기본 브랜치의 제출 결과만 저장소 의존성 현황에 반영합니다.

Dependabot Alerts는 알려진 취약점이 있는 의존성을 표시하고, Dependabot Security Updates는 수정 가능한 취약점에 대해 보안 업데이트 Pull Request를 생성합니다. 정기 버전 업데이트는 이번 범위에 포함하지 않으므로 `.github/dependabot.yml`은 사용하지 않습니다.

2026-08-09 기준 프론트엔드 의존성과 Backend·Frontend 컨테이너 이미지에서 수정 가능한
`HIGH`·`CRITICAL` 취약점은 0건으로 조치했습니다. 이 값은 새 취약점 공개에 따라 바뀔 수
있으므로 PR 저장소 검사와 `develop`·`main` 병합 후 이미지 검사를 계속 수행합니다.

## 경고 대응

시크릿 경고가 발생하면 다음 순서로 처리합니다.

1. 노출된 키나 토큰을 즉시 폐기하고 재발급합니다.
2. Git 기록, Actions 로그, 애플리케이션 로그와 배포 환경의 노출 범위를 확인합니다.
3. 새 값은 GitHub Secrets, 배포 환경 변수 또는 승인된 Secret Manager에 등록합니다.
4. 코드와 기록에서 노출 값을 제거하되, 기록 삭제만으로 기존 키가 안전해진다고 간주하지 않습니다.
5. 원인과 재발 방지 조치를 기록하되 실제 시크릿 값은 복사하지 않습니다.

취약 의존성 경고가 발생하면 영향 범위와 수정 버전을 확인하고, Dependabot Pull Request 또는 직접 업데이트로 해결한 뒤 Backend Test와 Frontend Lint·Build를 다시 실행합니다.

## 오탐 처리

오탐은 먼저 해당 값이 실제로 폐기 가능한 테스트 데이터인지 확인합니다. 예외가 반드시 필요하면 넓은 경로나 정규식 허용 규칙 대신 Gitleaks가 제공한 정확한 fingerprint만 `.gitleaksignore`에 추가하고, 이유를 Pull Request에 남깁니다.

실제 시크릿, 운영 데이터, 개인 정보는 오탐으로 처리하지 않습니다.

## 저장소 설정

Repository 관리자 설정에서 다음 항목을 활성화합니다.

- Dependency Graph
- Dependabot Alerts
- Dependabot Security Updates

`Secret Scan`이 한 번 실행된 후 `develop`과 `main`의 브랜치 보호 규칙에서 해당 작업을 필수 상태 검사로 등록합니다.

저장소 플랜에서 브랜치 보호 기능을 사용할 수 있다면 CI와 `Security Checks`의 PR 작업을
`develop`과 `main`의 필수 상태 검사로 등록합니다. 사용할 수 없는 경우에도 병합 전 모든
검사 결과가 성공했는지 수동으로 확인합니다.
