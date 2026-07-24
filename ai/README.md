# YMall Review Summary AI

이 디렉터리는 상품 리뷰 요약 모델의 데이터 준비, 평가, 추론 서비스를 관리한다.

현재 단계인 YMALL-68에서는 모델을 실행하지 않는다. 먼저 다음 기반을 만든다.

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

## 테스트

```bash
python -m unittest discover -s ai/tests -v
```

데이터 형식과 운영 원칙은
[`docs/ai/review-summary-dataset.md`](../docs/ai/review-summary-dataset.md)를 참고한다.
