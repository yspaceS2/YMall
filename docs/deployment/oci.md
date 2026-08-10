# OCI 배포 가이드

## 배포 구조

YMall 운영 환경은 OCI Japan East(Tokyo)의 Ampere A1 인스턴스 한 대에서 Docker Compose로 실행한다.
Caddy만 80, 443 포트를 외부에 공개하고 Frontend, Backend, PostgreSQL, Redis, Kafka는 Compose 내부
네트워크에서 통신한다. Caddy는 `ymall.cloud` 인증서를 자동 발급하고 HTTPS를 종료한다.

운영 서버에서는 Docker Desktop 전용 Docker Model Runner를 사용하지 않는다. AI 리뷰 요약 기능은
`compose.prod.yaml`에서 비활성화하며, 별도 추론 서버를 준비한 뒤에만 다시 활성화한다.

## OCI 네트워크 준비

1. 인스턴스에 공인 IPv4를 할당한다.
2. OCI 보안 목록 또는 NSG에서 TCP 22, 80, 443만 허용한다.
3. SSH 22 포트는 가능하면 관리자 공인 IP로 제한한다.
4. PostgreSQL 5432, Redis 6379, Kafka 9092는 인터넷에 공개하지 않는다.
5. `ymall.cloud`와 `www.ymall.cloud`의 A 레코드를 인스턴스 공인 IPv4로 지정한다.

## 서버 최초 준비

Ubuntu ARM64 서버에 Git과 Docker Engine, Docker Compose 플러그인을 설치한 뒤 저장소를 배치한다.
서버의 배포 디렉터리에는 Git에 포함되지 않는 `.env`를 직접 만든다.

```bash
sudo install -d -o "$USER" -g "$USER" /opt/ymall
git clone https://github.com/yspaceS2/YMall.git /opt/ymall
cd /opt/ymall
cp deploy/oci/.env.example .env
chmod 600 .env
```

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

| Secret | 설명 |
| --- | --- |
| `OCI_HOST` | 인스턴스 공인 IP 또는 SSH 호스트명 |
| `OCI_USER` | Ubuntu 이미지의 SSH 사용자 |
| `OCI_DEPLOY_PATH` | 서버 저장소 경로(예: `/opt/ymall`) |
| `OCI_SSH_PRIVATE_KEY` | 배포 전용 SSH 개인키 |
| `OCI_SSH_KNOWN_HOSTS` | 사전에 확인한 서버 host key 한 줄 |

워크플로는 최신 `main`을 가져와 컨테이너를 다시 빌드하고, 서비스 상태와 HTTPS 헬스체크를 검증한다.
배포 또는 헬스체크가 실패하면 직전 서버 커밋으로 돌아가 컨테이너를 다시 기동한다. 자동 push 배포가
아니므로 불필요한 Actions 사용 시간을 소비하지 않는다.

## 중지와 비용 확인

컨테이너만 중지해도 Compute 인스턴스와 부트 볼륨은 유지된다. OCI 비용을 완전히 피해야 할 때는
Always Free 범위와 현재 비용 분석을 확인하고 인스턴스 및 불필요한 볼륨의 종료 여부를 별도로 결정한다.
예산 알림은 비용을 차단하지 않으므로 배포 후 비용 분석도 함께 확인한다.
