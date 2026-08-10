# 리뷰 요약 추론 실행 환경

## 구성

YMall 리뷰 요약은 `Qwen3-4B-GGUF:Q4_K_M` 모델과 Docker Model Runner의
llama.cpp 엔진을 사용한다. 기존 0.6B 모델은 합성 평가에서는 계약을 통과했지만 실제
한국어 리뷰 10개 입력에서 빈 응답과 비정상 JSON이 반복되어 운영 후보에서 제외했다.
LoRA 어댑터는 YMALL-70 평가에서 감성 왜곡과 환각이 확인되어 적용하지 않는다.

별도 Python 웹 서버를 운영하지 않는다. Docker Model Runner가 제공하는
OpenAI 호환 API를 추론 경계로 사용하며, `ai/inference`의 Python 코드는
입력 제한과 API 계약을 검증하는 도구일 뿐 상시 실행 서비스가 아니다.

## 요구 사항

- Docker Desktop 4.41 이상
- Docker Compose 2.38 이상
- Docker Model Runner 활성화
- 모델을 실행할 수 있는 약 4GB 이상의 여유 메모리

모델 파일은 Docker의 모델 캐시에 저장되며 Git 저장소와 Docker 이미지에는
포함되지 않는다.

## 최초 준비

```powershell
docker desktop enable model-runner --tcp=12434
docker model pull hf.co/Qwen/Qwen3-4B-GGUF:Q4_K_M
docker model package --from hf.co/Qwen/Qwen3-4B-GGUF:Q4_K_M `
  --context-size 4096 ymall/qwen3-4b-review:Q4_K_M
```

Compose가 모델을 선언하므로 지원되는 환경에서는 첫 실행 때 자동으로 내려받을
수 있다. 다만 최초 다운로드 시간과 실패 원인을 분리하기 위해 명시적인 pull을
권장한다.

## 계약 검사

```powershell
docker compose --profile ai-check run --rm ai-contract-check
```

검사는 다음 순서로 동작한다.

1. OpenAI 호환 `GET /models`로 추론 서버와 모델 준비 상태를 확인한다.
2. 고정된 리뷰 세 건을 `POST /chat/completions`로 전송한다.
3. 응답 본문에서 JSON 객체를 추출한다.
4. `pros`, `cons`, `commonOpinions` 문자열 배열을 모두 포함하는지 확인한다.

모델 준비 전에는 모델 목록 검사에서 실패하고, 준비됐더라도 응답 계약이
다르면 요약 검사에서 실패한다.

호스트에서 이미 실행 중인 Model Runner를 직접 검사할 수도 있다.

```powershell
$env:AI_REVIEW_MODEL_URL = "http://localhost:12434/engines/v1"
$env:AI_REVIEW_MODEL_NAME = "ymall/qwen3-4b-review:Q4_K_M"
python -m ai.inference.check_service
```

## 입력 제한

| 항목 | 제한 |
| --- | ---: |
| 요청당 리뷰 | 1~100개 |
| 개별 리뷰 내용 | 최대 1,000자 |
| 전체 리뷰 내용 | 최대 6,000자 |
| 평점 | 1~5 |
| 생성 토큰 | 최대 512 |

리뷰가 10개 이상일 때만 실제 요약을 생성하는 서비스 정책은 Spring Boot가
연동되는 YMALL-72에서 적용한다. 이 계층은 추론 서버를 보호하기 위한 기술적
상한만 담당한다.

## 환경변수

| 변수 | 기본값 | 설명 |
| --- | --- | --- |
| `AI_REVIEW_MODEL` | `hf.co/Qwen/Qwen3-4B-GGUF:Q4_K_M` | OCI/Hugging Face 모델 식별자 |
| `AI_REVIEW_CONTEXT_SIZE` | `4096` | 모델 컨텍스트 크기 |
| `AI_REVIEW_MAX_TOKENS` | `512` | 요청당 최대 생성 토큰 |
| `AI_REVIEW_THREADS` | `4` | CPU 추론 스레드 |
| `AI_REVIEW_PARALLEL` | `1` | 병렬 처리 슬롯 |
| `AI_REVIEW_GPU_LAYERS` | `0` | GPU로 올릴 레이어 수. `0`이면 CPU 기본 실행 |
| `AI_REVIEW_REQUEST_TIMEOUT_SECONDS` | `180` | 계약 검사 요청 제한 시간 |

로컬 장비의 코어와 메모리에 맞춰 값을 조정한다. GPU 사용은 Docker Model
Runner가 해당 GPU와 드라이버를 지원하는 환경에서만 활성화한다.

## API 계약

요약 호출은 OpenAI 호환 `POST /chat/completions`를 사용한다.

```json
{
    "model": "huggingface.co/qwen/qwen3-4b-gguf:Q4_K_M",
    "messages": [
        {
            "role": "system",
            "content": "한국어 쇼핑 리뷰 요약 지침"
        },
        {
            "role": "user",
            "content": "검증된 리뷰 목록"
        }
    ],
    "temperature": 0,
    "max_tokens": 512,
    "stream": false,
    "response_format": {
        "type": "json_object"
    }
}
```

Docker Model Runner가 공식 지원하는 JSON 객체 모드를 사용한다. `json_schema`는 현재
지원 대상이 아니므로 사용하지 않는다. 정상 결과의 `choices[0].message.content`는 다음
구조의 JSON 문자열이어야 하며 Backend가 필드·타입과 항목 수를 검증한다.

```json
{
    "pros": ["연결이 빠르고 디자인이 깔끔합니다."],
    "cons": ["무게가 무거워 휴대하기 어렵습니다."],
    "commonOpinions": ["연결 속도가 빠르다는 의견이 반복됩니다."]
}
```

Spring Boot 연동, 타임아웃·재시도, Redis 캐시, Kafka 비동기 생성과 장애 시
기존 요약 제공은 후속 작업 YMALL-72에서 구현한다.
