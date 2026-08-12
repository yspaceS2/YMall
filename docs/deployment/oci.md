# OCI 배포 가이드

## 배포 구조

YMall 운영 환경은 OCI Japan East(Tokyo)의 Ampere A1 인스턴스 한 대에서 Docker Compose로 실행한다.
Caddy만 80, 443 포트를 외부에 공개하고 Frontend, Backend, PostgreSQL, Redis, Kafka는 Compose 내부
네트워크에서 통신한다. Caddy는 `ymall.cloud` 인증서를 자동 발급하고 HTTPS를 종료한다.

운영 환경은 `Qwen3-4B-GGUF Q4_K_M` 4,096 컨텍스트를 CPU로 순차 실행한다. 모델 파일은
2.32GiB이며 리뷰 변경 이벤트를 Kafka로 전달해 요약 생성을 사용자 응답과 분리한다. 2 OCPU·12GB
Ampere A1 인스턴스에서 실제 모델 계약 검사와 운영 DB 요약 갱신을 완료했으며, 추론 중에도 기존
요약을 제공한다.

Docker Model Runner와 모델은 다음 조건으로 실행한다.

- `AI_REVIEW_PARALLEL=1`
- `AI_REVIEW_CONTEXT_SIZE=4096`
- `AI_REVIEW_MAX_TOKENS=192`
- 모델: `Qwen3-4B-GGUF Q4_K_M`
- PostgreSQL, Redis, Kafka를 외부에 공개하지 않음
- 부하 중 메모리 부족이나 장시간 CPU 포화가 발생하면 `AI_REVIEW_ENABLED=false`로 전환하고 저장된 요약만 제공

## OCI 네트워크 준비

1. 인스턴스에 공인 IPv4를 할당한다.
2. OCI 보안 목록 또는 NSG에서 TCP 22, 80, 443만 허용한다.
3. SSH 22 포트는 가능하면 관리자 공인 IP로 제한한다.
4. PostgreSQL 5432, Redis 6379, Kafka 9092는 인터넷에 공개하지 않는다.
5. `ymall.cloud`와 `www.ymall.cloud`의 A 레코드를 인스턴스 공인 IPv4로 지정한다.

## 서버 최초 준비

Ubuntu ARM64 서버에 Git과 Docker Engine, Docker Compose 플러그인, Docker Model Runner를 설치한 뒤 저장소를 배치한다.
서버의 배포 디렉터리에는 Git에 포함되지 않는 `.env`를 직접 만든다.

```bash
sudo apt-get update
sudo apt-get install -y docker-model-plugin
docker model status
docker model pull hf.co/Qwen/Qwen3-4B-GGUF:Q4_K_M

sudo install -d -o "$USER" -g "$USER" /opt/ymall
git clone https://github.com/yspaceS2/YMall.git /opt/ymall
cd /opt/ymall
cp deploy/oci/.env.example .env
chmod 600 .env
```

`deploy/oci/bootstrap.sh`는 Docker Engine과 함께 `docker-model-plugin`을 설치한다. 이미
Docker Engine을 설치한 서버라면 위 명령으로 Model Runner만 추가할 수 있다.

배포 스크립트는 애플리케이션 컨테이너를 재생성하기 전에 Model Runner 실행 상태와
`AI_REVIEW_MODEL`에 지정된 모델의 로컬 등록 여부를 검사한다. AI가 활성화된 상태에서
둘 중 하나라도 준비되지 않았다면 배포를 시작하지 않고 실패 원인을 출력하므로, Backend
헬스체크만 통과한 채 AI 기능을 사용할 수 없는 상태로 배포되는 것을 방지한다. 전체 추론
계약 검사는 약 1분이 걸리므로 매 배포에 포함하지 않고 운영 변경 후 수동 검증으로 유지한다.

`.env`의 모든 `replace_me` 및 `replace_with_...` 값을 실제 운영값으로 교체한다. 환경변수 값은 Jira,
PR, 저장소, 배포 로그에 기록하지 않는다.

## 수동 최초 배포

DNS 전파가 완료되고 80, 443 포트가 열려 있는지 확인한 후 실행한다.

```bash
cd /opt/ymall
docker compose -f compose.yaml -f compose.prod.yaml config --quiet
docker compose -f compose.yaml -f compose.prod.yaml up -d --build
docker compose -f compose.yaml -f compose.prod.yaml ps
curl --fail https://ymall.cloud/health
```

SMTP를 연결하지 않은 초기 스모크 배포에서는 `.env`의
`MAIL_HEALTH_ENABLED=false`로 메일 health indicator만 비활성화할 수 있다.
실제 SMTP 자격 증명을 등록한 운영 환경에서는 기본값인 `true`를 사용한다.

애플리케이션 기동에 실패하면 비밀값을 출력하지 말고 서비스별 로그를 확인한다.

```bash
docker compose -f compose.yaml -f compose.prod.yaml logs --tail=200 postgres redis kafka backend frontend caddy
```

## GitHub Actions 수동 배포

`Deploy to OCI` 워크플로는 `main`에서 수동 실행할 때만 동작한다. GitHub의 `production` Environment에
다음 Secrets를 등록한다.

이 프로젝트는 운영 배포를 **수동 승인형 CD**로 유지한다. `develop → main` PR의 CI와 보안 검사를
통과한 뒤 운영자가 배포 시작 시점을 선택하고, 실행 이후의 서버 갱신, Compose 재빌드, 헬스체크,
실패 시 직전 커밋 롤백과 Slack 결과 알림은 자동으로 처리한다. 단일 포트폴리오 인스턴스에서 불필요한
재배포와 Actions 사용을 줄이고, 운영 변경 시점을 명시적으로 통제하기 위한 선택이다.

| Secret | 설명 |
| --- | --- |
| `OCI_HOST` | 인스턴스 공인 IP 또는 SSH 호스트명 |
| `OCI_USER` | Ubuntu 이미지의 SSH 사용자 |
| `OCI_DEPLOY_PATH` | 서버 저장소 경로(예: `/opt/ymall`) |
| `OCI_SSH_PRIVATE_KEY` | 배포 전용 SSH 개인키 |
| `OCI_SSH_KNOWN_HOSTS` | 사전에 확인한 서버 host key 한 줄 |
| `SLACK_WEBHOOK_URL` | `#ymall-deploy` 채널의 Incoming Webhook URL |

워크플로는 최신 `main`을 가져와 컨테이너를 다시 빌드하고, 서비스 상태와 HTTPS 헬스체크를 검증한다.
배포 또는 헬스체크가 실패하면 직전 서버 커밋으로 돌아가 컨테이너를 다시 기동한다. 자동 push 배포가
아니므로 불필요한 Actions 사용 시간을 소비하지 않는다.

배포를 시작할 때와 종료됐을 때 `#ymall-deploy` 채널로 실행자, 커밋, 결과 및 Actions 로그 링크를 알린다.
Slack 또는 네트워크 장애가 실제 배포 결과에 영향을 주지 않도록 알림 단계에는 `continue-on-error`를 적용한다.

### 운영 배포 검증 결과

2026년 8월 11~12일 다음 항목을 운영 환경에서 재검증했다.

- `develop → main` 릴리스 PR 병합 후 `main` CI 전체 통과
- `Deploy to OCI` 수동 실행으로 최신 `main` 배포 성공
- Backend·Frontend·인프라 서비스 기동과 `https://ymall.cloud/health` HTTP 200 확인
- 배포 시작·성공 결과와 Actions 실행 링크의 Slack 전달 확인
- Toss Payments 테스트 결제창 진입, 결제 승인과 전체 환불 완료 확인
- 결제용 CSP 공식 도메인이 운영 응답 Header에 반영된 것을 확인
- Qwen3 4B 모델 계약 검사와 Kafka 기반 운영 리뷰 요약 갱신 확인
- 추론 중 전체 서비스 healthy, HTTPS 200 및 사용 가능 메모리 약 6.2GiB 확인

검증에 사용한 실제 시크릿, 결제 키, 거래 식별자와 회원 정보는 문서와 배포 로그에 기록하지 않는다.
최신 결제 CSP 변경은 [PR #162](https://github.com/yspaceS2/YMall/pull/162), 운영 릴리스는
[PR #163](https://github.com/yspaceS2/YMall/pull/163)에서 확인할 수 있다.

## 자동 백업과 복원 검증

운영 백업은 PostgreSQL custom format과 `ymall_backend-uploads` 볼륨 압축 파일을 하나의 백업 세트로
생성한다. 완성 전 파일은 `.partial` 디렉터리에 기록하고 두 파일과 SHA-256 체크섬 생성이 모두 성공한
뒤에만 최종 디렉터리로 전환한다. 기본 경로는 `/opt/ymall-backups`이며 최근 7일을 보관한다.

시스템 타이머를 설치한다.

```bash
cd /opt/ymall
chmod +x deploy/oci/*.sh
./deploy/oci/install-backup-timer.sh
systemctl list-timers ymall-backup.timer --no-pager
```

타이머는 매일 00:00 KST에 실행되고 서버가 꺼져 실행 시점을 놓친 경우 다음 기동 시 한 번 실행한다.
백업은 `flock`으로 중복 실행을 차단하며 실행 로그와 실패 상태는 systemd journal에 남는다.

```bash
sudo systemctl start ymall-backup.service
sudo systemctl status ymall-backup.service --no-pager
sudo journalctl -u ymall-backup.service --since today --no-pager
```

백업 직후 또는 정기 운영 점검에서 체크섬, 업로드 압축 파일과 PostgreSQL 복원을 검증한다. 검증 스크립트는
운영 DB를 덮어쓰지 않고 임시 데이터베이스에 복원한 후 자동 삭제한다. 인자를 생략하면 최신 백업을 사용한다.

```bash
sudo /opt/ymall/deploy/oci/verify-backup.sh
sudo /opt/ymall/deploy/oci/verify-backup.sh /opt/ymall-backups/20260812000000
```

실제 장애 복구에서는 먼저 현재 데이터를 별도 백업하고 Backend의 DB 접근을 중지한 뒤
`pg_restore --clean --if-exists`로 대상 DB를 교체한다. 업로드 볼륨도 같은 시각의 `uploads.tar.gz`로 함께 복원해야
DB의 이미지 경로와 파일이 일치한다. 운영 복원은 파괴적 작업이므로 자동화하지 않고, 복원할 백업 시각과
현재 데이터 보존 여부를 이중 확인한 뒤 수행한다.

현재 백업은 OCI 인스턴스의 같은 부트 볼륨에 저장되므로 운영자 실수와 논리적 데이터 손상에는 대응하지만,
인스턴스 또는 부트 볼륨 자체의 유실에는 대응하지 못한다. 장기 운영으로 전환할 때는 OCI Object Storage 등
별도 장애 도메인으로 암호화 복제하고 복제본의 보존·삭제 정책도 함께 관리한다.

기본값은 환경변수로 조정할 수 있다.

| 환경변수 | 기본값 | 설명 |
| --- | --- | --- |
| `YMALL_PROJECT_DIR` | `/opt/ymall` | 운영 저장소 경로 |
| `YMALL_BACKUP_ROOT` | `/opt/ymall-backups` | 백업 보관 경로 |
| `YMALL_BACKUP_RETENTION_DAYS` | `7` | 백업 보관 일수 |
| `YMALL_UPLOAD_VOLUME` | `ymall_backend-uploads` | 업로드 Docker 볼륨 |

## 중지와 비용 확인

컨테이너만 중지해도 Compute 인스턴스와 부트 볼륨은 유지된다. OCI 비용을 완전히 피해야 할 때는
Always Free 범위와 현재 비용 분석을 확인하고 인스턴스 및 불필요한 볼륨의 종료 여부를 별도로 결정한다.
예산 알림은 비용을 차단하지 않으므로 배포 후 비용 분석도 함께 확인한다.
