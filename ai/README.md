# YMall Review Summary AI

이 디렉터리는 상품 리뷰 요약 모델의 데이터 준비, 평가, 추론 서비스를 관리한다.

YMALL-68에서 데이터 계약과 품질 기준을 만들었고, YMALL-69에서 동일한 입력으로
모델 후보를 실행해 비교한다.

- 상품별 다중 리뷰와 기준 요약의 데이터 계약
- 출처와 라이선스 승인 목록
- 개인정보 마스킹과 부적절한 입력 제거
- 상품 단위 학습·검증·테스트 분리
- 자동 지표와 사람 평가 기준

## 데이터 준비

Python 3.11 이상에서 별도 패키지 설치 없이 실행할 수 있다.

```bash
python ai/scripts/prepare_dataset.py \
  --input ai/datasets/raw/synthetic-v1.jsonl \
  --sources ai/datasets/sources.json \
  --output-dir ai/datasets/processed
```

PowerShell에서는 줄바꿈 문자 대신 한 줄로 실행하거나 백틱을 사용한다.

생성되는 `ai/datasets/processed/`는 로컬 산출물이므로 Git에 포함하지 않는다.

## 모델 후보 평가

YMALL-69에서는 동일한 합성 데이터로 `google/mt5-small`과
`Qwen/Qwen3-0.6B-GGUF`를 비교한다.

```bash
python -m venv ai/.venv
ai/.venv/Scripts/python -m pip install -r ai/evaluation/requirements.txt

docker model pull hf.co/Qwen/Qwen3-0.6B-GGUF:Q8_0
docker desktop enable model-runner --tcp=12434

ai/.venv/Scripts/python ai/evaluation/evaluate_models.py \
  --input ai/datasets/raw/synthetic-v1.jsonl \
  --output ai/evaluation/results/baseline.json
```

Linux와 macOS에서는 가상환경 실행 파일 경로를 `ai/.venv/bin/python`으로 바꾼다.
모델은 Hugging Face와 Docker Model Runner 캐시에 저장되며 Git에는 포함되지 않는다.
비교 결과와 선정 근거는
[`docs/ai/model-candidate-evaluation.md`](../docs/ai/model-candidate-evaluation.md)를
참고한다.

## 테스트

```bash
python -m unittest discover -s ai/tests -v
```

데이터 형식과 운영 원칙은
[`docs/ai/review-summary-dataset.md`](../docs/ai/review-summary-dataset.md)를 참고한다.

## LoRA 미세조정

YMALL-70에서는 Qwen3-0.6B 원본 가중치를 고정하고 LoRA 어댑터만 학습하는
재현 가능한 실험 파이프라인을 구성했다. CUDA용 PyTorch는 환경에 맞는 공식
설치 명령으로 먼저 설치하고, 나머지 의존성을 설치한다.

```bash
python -m venv ai/training/.venv
ai/training/.venv/Scripts/python -m pip install \
  torch --index-url https://download.pytorch.org/whl/cu130
ai/training/.venv/Scripts/python -m pip install \
  -r ai/training/requirements.txt

ai/training/.venv/Scripts/python ai/training/prepare_sft_data.py \
  --config ai/training/configs/qwen3-0.6b-lora-v1.json \
  --output-dir ai/training/data/qwen3-0.6b-lora-v1

ai/training/.venv/Scripts/python ai/training/train_lora.py \
  --config ai/training/configs/qwen3-0.6b-lora-v1.json \
  --data-dir ai/training/data/qwen3-0.6b-lora-v1 \
  --result ai/training/results/qwen3-0.6b-lora-v1-training.json

ai/training/.venv/Scripts/python ai/training/evaluate_lora.py \
  --config ai/training/configs/qwen3-0.6b-lora-v1.json \
  --adapter-dir ai/models/qwen3-0.6b-lora-v1/adapter \
  --output ai/training/results/qwen3-0.6b-lora-v1-evaluation.json
```

학습 데이터와 모델 파일은 로컬 산출물이므로 Git에 포함하지 않는다. 설정,
해시, 환경, 정량·정성 평가 및 모델 채택 결정은
[`docs/ai/review-summary-fine-tuning.md`](../docs/ai/review-summary-fine-tuning.md)에
기록한다.
