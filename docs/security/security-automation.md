# 보안 자동 검사 정책

배포 전 인증·권한·소유권·결제 점검 결과는
[`pre-deployment-security-checklist.md`](./pre-deployment-security-checklist.md)에 기록합니다.
KISA 항목별 판정과 배포 후 점검 범위는
[`kisa-2026-assessment.md`](./kisa-2026-assessment.md)를 기준으로 관리합니다.

## 목적

이 문서는 YMall 저장소의 정적 분석, 의존성·구성 검사와 컨테이너 이미지 검사 기준을 정의합니다. 이 검사는 프로젝트 자체 점검이며 외부 보안 인증이나 KISA 공식 심사를 의미하지 않습니다.

## 검사 구성

`Security Checks` GitHub Actions 워크플로는 다음 검사를 수행합니다.

- Gitleaks: 전체 Git 이력의 시크릿 패턴 검사
- Semgrep: Java·TypeScript 소스의 기본 보안 규칙 및 OWASP Top 10 정적 분석
- Trivy repository scan: npm·Gradle 의존성, IaC·Docker·Compose 설정과 시크릿 검사
- Trivy image scan: Backend·Frontend 런타임 이미지의 OS·애플리케이션 의존성과 시크릿 검사
- OWASP ZAP Baseline: 격리된 임시 YMall 환경의 공개 화면과 익명 응답을 탐색하고 수동 분석

Private 저장소에서 GitHub Code Scanning을 사용할 수 없는 현재 계정 구성을 고려해 Semgrep OSS가 PR의 SAST 실패 여부를 직접 판정합니다. 저장소 공개 범위나 GitHub 라이선스가 변경되면 CodeQL 전환 또는 병행 여부를 다시 평가합니다.

## 실행 시점

- Pull Request: Gitleaks, Semgrep, Trivy repository scan
- `develop`·`main` push: 위 검사와 Backend·Frontend 이미지 검사
- 수동 실행: 전체 검사
- `OWASP ZAP Baseline` 수동 실행: 격리된 임시 환경을 생성해 익명 동적 검사

이미지 검사는 빌드 비용과 중복 CI 사용량을 줄이기 위해 Pull Request에서는 실행하지 않고 병합 후 한 번 실행합니다.
ZAP은 실행 중인 공용·운영 환경을 실수로 검사하지 않도록 수동 워크플로만 제공합니다. 검사 대상은 별도 Compose 프로젝트와 임시 데이터 볼륨으로 구성하고 검사 후 제거합니다.

## 실패 기준

- Semgrep: 심각도 `ERROR` 발견 시 실패
- Trivy: 수정 버전이 존재하는 `HIGH` 또는 `CRITICAL` 발견 시 실패
- Gitleaks: 실제 시크릿으로 판단되는 탐지 결과가 있으면 실패
- ZAP: 익명 Baseline의 Medium 위험 규칙은 실패, Low는 경고, SPA·정적 자원 캐시 탐지는 정보성으로 분류

`CRITICAL`은 즉시 조치합니다. `HIGH`도 원칙적으로 병합 전에 조치합니다. 수정 버전이 없거나 오탐으로 확인된 경우에만 예외를 허용합니다.

## 예외 관리

예외를 추가할 때는 다음 정보를 PR과 YMALL-79에 기록합니다.

- 탐지 도구, 규칙 또는 취약점 식별자
- 영향 범위와 오탐 또는 미적용 판단 근거
- 보완 통제
- 담당자와 재검토 만료일

광범위한 경로 제외나 심각도 하향은 허용하지 않습니다. 예외는 가능한 한 정확한 규칙·취약점 단위로 제한하고 만료 시 다시 검토합니다.

## 결과 및 시크릿 취급

- 검사 출력과 보고서에 실제 토큰, 개인정보, 결제정보를 복사하지 않습니다.
- Gitleaks 결과 artifact 업로드는 비활성 상태로 유지합니다.
- 실패 원인 기록에는 취약점 식별자와 영향만 남기고 실제 시크릿 값은 남기지 않습니다.
- 시크릿 노출이 확인되면 값 삭제보다 폐기·교체를 먼저 수행하고 Git 이력, CI·애플리케이션 로그와 배포 환경의 노출 범위를 확인합니다.

## 도입 시 기준선 조치

자동 검사를 처음 적용하면서 Backend 이미지에서 수정 가능한 High 취약점 4건을 확인해 함께 조치했습니다.

- PostgreSQL JDBC를 `42.7.12`로 업데이트
- Backend·Frontend 런타임 이미지 빌드 시 Alpine 보안 패키지 업데이트 적용

조치 전후의 상세 식별자와 재검증 결과는 YMALL-79 및 해당 Pull Request에 기록합니다.

2026-08-09 기준 저장소와 Backend·Frontend 이미지에서 수정 가능한 `HIGH`·`CRITICAL`
기준선은 0건이며 Semgrep 발견 사항도 0건입니다. 이 수치는 영구 보증이 아니므로 병합 후
이미지 검사와 배포 전 재검사를 계속 수행합니다.

## ZAP 익명 기준선

ZAP Baseline은 실제 공격 페이로드를 전송하지 않고 페이지 탐색과 응답 분석만 수행합니다. 첫 검사에서 보안 헤더 미설정을 확인해 Frontend Nginx에 다음 통제를 적용했습니다.

- Content Security Policy
- Permissions Policy
- Cross-Origin Opener·Resource Policy

다음 Low 탐지는 기능 호환성을 위해 제한된 범위로 예외 처리하고 2026-11-09에 재검토합니다. 소유자는 YMall 유지보수 담당자입니다.

- ZAP 90004: Google One Tap·OAuth 팝업 흐름을 위해 COOP `same-origin-allow-popups`를 유지하고 COEP는 강제하지 않습니다. CORP는 `same-origin`을 유지합니다.

인증 후 화면과 데이터 변경을 유발할 수 있는 Active Scan은 이 워크플로에 포함하지 않습니다. 해당 검사는 전용 계정·폐기 가능한 데이터·명시적 승인이 준비된 후 별도로 실행합니다.

## 현재 적용 상태

| 검사 | PR | `develop`·`main` Push | 수동 실행 | 현재 기준선 |
| --- | --- | --- | --- | --- |
| Gitleaks | 실행 | 실행 | 실행 | 통과 |
| Semgrep | 실행 | 실행 | 실행 | 0 findings |
| Trivy repository | 실행 | 실행 | 실행 | HIGH·CRITICAL 0건 |
| Trivy image | 생략 | 실행 | 실행 | Backend·Frontend 0건 |
| ZAP Baseline | 생략 | 생략 | 실행 | FAIL 0, 신규 Medium 0 |
