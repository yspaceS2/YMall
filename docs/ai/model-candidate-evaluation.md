# 리뷰 요약 모델 후보 평가

## 결론

YMALL-69의 다음 단계 후보로 `Qwen3-0.6B-GGUF Q8_0`을 선정한다.
이 결정은 곧바로 운영 품질을 충족했다는 의미가 아니다. 현재 합성 샘플에서
`google/mt5-small`은 지시를 따르거나 JSON 요약을 만들지 못한 반면, Qwen은 모든
샘플에서 지정한 구조로 한국어 요약을 생성했다. YMALL-70에서는 Qwen을 기준
모델로 삼아 프롬프트 개선 또는 경량 튜닝의 필요성을 판단한다.

## 비교 대상

| 후보 | 성격 | 라이선스 | 실행 방식 | 선정 판단 |
| --- | --- | --- | --- | --- |
| `google/mt5-small` | 300M 규모 다국어 seq2seq 사전학습 모델 | Apache-2.0 | Transformers, PyTorch CPU | 제외 |
| `Qwen/Qwen3-0.6B-GGUF` Q8_0 | 약 596M 규모 지시형 decoder LLM | Apache-2.0 | Docker Model Runner, llama.cpp CPU | 후속 기준 모델 |

모델 정보와 라이선스는
[google/mt5-small 공식 모델 카드](https://huggingface.co/google/mt5-small)와
[Qwen3-0.6B-GGUF 공식 모델 카드](https://huggingface.co/Qwen/Qwen3-0.6B-GGUF)를
기준으로 확인했다. Qwen은 로컬 CPU 추론 경로를 단순화하기 위해
[Docker Model Runner](https://docs.docker.com/ai/model-runner/)로 실행했다.

## 평가 조건

- 평가일: 2026-07-24
- 운영체제: Windows 11
- CPU: Intel Core i7-9700KF, 논리 CPU 8개
- 메모리: 약 15.9 GiB
- GPU: 사용하지 않음
- Python: 3.12.13
- Docker Engine: 29.2.1
- Docker Model Runner: 1.1.1, llama.cpp CPU 엔진
- 데이터: `ai/datasets/raw/synthetic-v1.jsonl`의 합성 상품 6개
- 입력: 후보마다 동일한 리뷰, 프롬프트, 최대 출력 토큰 192
- 생성: 샘플링을 사용하지 않는 결정적 설정
- 결과 파일: `ai/evaluation/results/baseline.json`

모델 파일을 내려받는 시간은 측정에서 제외했다. 아래 로딩 시간은 모델 파일이 로컬
캐시에 있는 상태에서 측정한 값이다. 시스템 메모리 증가는 전체 시스템 사용량의
전후 차이이므로, 이미 별도 프로세스에 적재된 Docker 모델의 실제 최대 메모리를
뜻하지 않는다.

## 측정 결과

| 지표 | mT5-small | Qwen3-0.6B Q8_0 |
| --- | ---: | ---: |
| 파라미터 수 | 300,176,768 | 약 596,050,000 |
| 모델 파일·메모리 표기 | PyTorch 모델 메모리 1,145.08 MiB | GGUF 파일 604.15 MiB |
| 캐시 상태 로딩 | 1.5101초 | 2.3871초 |
| 샘플 생성 p50 | 0.4579초 | 2.6855초 |
| 샘플 생성 p95 | 0.6945초 | 2.8171초 |
| JSON 형식 준수율 | 0% | 100% |
| 장점·단점·공통 의견 완성률 | 0% | 100% |
| 평균 ROUGE-L | 0.0000 | 0.1496 |

mT5는 모든 샘플에서 `<extra_id_0>하세요.`만 출력했다. 속도는 더 빨랐지만 리뷰
요약 결과가 아니므로 서비스 후보로 볼 수 없다. mT5는 다국어 사전학습 모델이며
현재 작업에서는 리뷰 요약 파인튜닝을 적용하지 않았다.

Qwen은 모든 샘플에서 JSON 구조와 세 섹션을 만들고, 출력 내용도 대체로 원문 리뷰
안에서 생성했다. ROUGE-L이 낮은 이유에는 기준 문구와 다른 자연스러운 표현을
사용한 점도 포함되므로 이 수치만으로 사실성을 판정하지 않는다.

## 정성 확인과 한계

Qwen 출력에는 다음 개선점이 확인됐다.

- 키보드 샘플에서 빠른 블루투스 연결이라는 장점을 단점 문장에 함께 넣었다.
- 공통 의견에 여러 리뷰의 경향보다 한 리뷰의 개별 사실을 넣는 경우가 있었다.
- 일부 문장은 의미는 전달되지만 표현이 어색했다.
- 리뷰별 근거 ID를 출력하지 않으므로 자동으로 근거 연결률을 계산하지 못했다.

따라서 현재 결과로 운영 품질 목표 달성을 주장하지 않는다. 샘플이 6개뿐이고 모두
합성 데이터이며, 독립 평가자의 블라인드 사람 평가와 BERTScore도 수행하지 않았다.
이번 비교의 목적은 제한된 로컬 CPU 환경에서 실제 실행 가능한 기준 후보를 고르는
것이다.

## 선정 근거와 다음 단계

Qwen을 선택한 이유는 다음과 같다.

1. 별도 학습 없이 한국어 리뷰 요약 지시를 수행했다.
2. API에서 요구하는 JSON 구조를 모든 평가 샘플에서 지켰다.
3. 약 604 MiB의 양자화 모델로 로컬 CPU 실행이 가능했다.
4. 후속 단계에서 프롬프트 개선과 경량 튜닝을 같은 기준 결과와 비교할 수 있다.

YMALL-70에서는 먼저 프롬프트와 출력 검증으로 장단점 혼합 및 공통 의견 왜곡을
줄인다. 그래도 품질 목표를 충족하지 못할 때만 LoRA 등 경량 튜닝을 검토한다.
운영 연동 전에는 더 큰 승인 데이터셋, 독립 사람 평가, 최대 메모리 측정, 입력 길이별
부하 측정과 실패 응답 정책을 추가해야 한다.

## 재현 방법

```bash
python -m venv ai/.venv
ai/.venv/Scripts/python -m pip install -r ai/evaluation/requirements.txt

docker model pull hf.co/Qwen/Qwen3-0.6B-GGUF:Q8_0
docker desktop enable model-runner --tcp=12434

ai/.venv/Scripts/python ai/evaluation/evaluate_models.py \
  --input ai/datasets/raw/synthetic-v1.jsonl \
  --output ai/evaluation/results/baseline.json \
  --models mt5 qwen

python -m unittest discover -s ai/tests -v
```
