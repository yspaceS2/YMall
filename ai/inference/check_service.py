from __future__ import annotations

import json
import os
import sys
import urllib.error
import urllib.request
from typing import Any

from ai.inference.contract import (
    DEFAULT_MAX_TOKENS,
    ReviewInput,
    ReviewSummaryContractError,
    build_chat_completion_request,
    parse_summary_content,
)


DEFAULT_MODEL_URL = "http://localhost:12434/engines/v1"
DEFAULT_REQUEST_TIMEOUT_SECONDS = 180


class InferenceCheckError(RuntimeError):
    """Raised when the model service does not satisfy the runtime contract."""


def endpoint(base_url: str, path: str) -> str:
    return f"{base_url.rstrip('/')}/{path.lstrip('/')}"


def request_json(
    url: str,
    *,
    method: str = "GET",
    payload: dict[str, Any] | None = None,
    timeout_seconds: int = DEFAULT_REQUEST_TIMEOUT_SECONDS,
) -> dict[str, Any]:
    request = urllib.request.Request(
        url,
        data=(
            json.dumps(payload, ensure_ascii=False).encode("utf-8")
            if payload is not None
            else None
        ),
        headers={"Content-Type": "application/json"},
        method=method,
    )
    try:
        with urllib.request.urlopen(request, timeout=timeout_seconds) as response:
            response_payload = json.loads(response.read().decode("utf-8"))
    except (urllib.error.URLError, TimeoutError, json.JSONDecodeError) as error:
        raise InferenceCheckError(f"AI 추론 서비스 요청에 실패했습니다: {url}") from error
    if not isinstance(response_payload, dict):
        raise InferenceCheckError(f"AI 추론 서비스가 JSON 객체를 반환하지 않았습니다: {url}")
    return response_payload


def normalize_model_id(model_id: str) -> str:
    normalized = model_id.strip().casefold()
    if normalized.startswith("hf.co/"):
        return f"huggingface.co/{normalized.removeprefix('hf.co/')}"
    return normalized


def verify_model_ready(
    base_url: str,
    timeout_seconds: int,
    expected_model: str | None = None,
) -> list[str]:
    payload = request_json(
        endpoint(base_url, "models"),
        timeout_seconds=timeout_seconds,
    )
    models = payload.get("data")
    if not isinstance(models, list) or not models:
        raise InferenceCheckError("AI 추론 서버는 응답했지만 준비된 모델이 없습니다.")

    model_ids = [
        model.get("id")
        for model in models
        if isinstance(model, dict) and isinstance(model.get("id"), str)
    ]
    if not model_ids:
        raise InferenceCheckError("준비된 모델의 식별자를 확인할 수 없습니다.")
    if expected_model is not None:
        normalized_expected_model = normalize_model_id(expected_model)
        if all(
            normalize_model_id(model_id) != normalized_expected_model
            for model_id in model_ids
        ):
            raise InferenceCheckError(
                f"요청한 모델이 준비되지 않았습니다: {expected_model}"
            )
    return model_ids


def verify_summary_response(
    base_url: str,
    model: str,
    max_tokens: int,
    timeout_seconds: int,
) -> dict[str, list[str]]:
    reviews = [
        ReviewInput(rating=5, content="키감이 부드럽고 연결이 빠릅니다."),
        ReviewInput(rating=4, content="연결이 빠르고 디자인이 깔끔합니다."),
        ReviewInput(rating=2, content="무게가 무거워서 휴대하기 어렵습니다."),
    ]
    request_payload = build_chat_completion_request(
        reviews=reviews,
        model=model,
        max_tokens=max_tokens,
    )
    response_payload = request_json(
        endpoint(base_url, "chat/completions"),
        method="POST",
        payload=request_payload,
        timeout_seconds=timeout_seconds,
    )
    try:
        content = response_payload["choices"][0]["message"]["content"]
    except (KeyError, IndexError, TypeError) as error:
        raise InferenceCheckError("요약 응답이 OpenAI 호환 JSON 계약과 다릅니다.") from error
    if not isinstance(content, str):
        raise InferenceCheckError("요약 응답 본문은 문자열이어야 합니다.")
    try:
        return parse_summary_content(content)
    except ReviewSummaryContractError as error:
        raise InferenceCheckError(str(error)) from error


def main() -> int:
    base_url = os.getenv("AI_REVIEW_MODEL_URL", DEFAULT_MODEL_URL)
    model = os.getenv(
        "AI_REVIEW_MODEL_NAME",
        "huggingface.co/qwen/qwen3-0.6b-gguf:Q8_0",
    )
    max_tokens = int(os.getenv("AI_REVIEW_MAX_TOKENS", str(DEFAULT_MAX_TOKENS)))
    timeout_seconds = int(
        os.getenv(
            "AI_REVIEW_REQUEST_TIMEOUT_SECONDS",
            str(DEFAULT_REQUEST_TIMEOUT_SECONDS),
        )
    )

    try:
        model_ids = verify_model_ready(base_url, timeout_seconds, model)
        summary = verify_summary_response(
            base_url=base_url,
            model=model,
            max_tokens=max_tokens,
            timeout_seconds=timeout_seconds,
        )
    except (InferenceCheckError, ValueError) as error:
        print(f"[실패] {error}", file=sys.stderr)
        return 1

    print(f"[정상] AI 추론 서버 준비 완료: {', '.join(model_ids)}")
    print(json.dumps(summary, ensure_ascii=False, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
