from __future__ import annotations

import argparse
import json
import platform
import statistics
import sys
import time
from contextlib import nullcontext
from pathlib import Path
from typing import Any

AI_DIR = Path(__file__).resolve().parents[1]
COMMON_DIR = AI_DIR / "common"
EVALUATION_DIR = AI_DIR / "evaluation"
for module_dir in (COMMON_DIR, EVALUATION_DIR):
    if str(module_dir) not in sys.path:
        sys.path.insert(0, str(module_dir))

try:
    from .prepare_sft_data import (
        file_sha256,
        load_training_config,
        read_jsonl,
        resolve_project_path,
    )
except ImportError:
    from prepare_sft_data import (  # type: ignore[no-redef]
        file_sha256,
        load_training_config,
        read_jsonl,
        resolve_project_path,
    )

from review_summary import (  # noqa: E402
    PROMPT_VERSION,
    SUMMARY_FIELDS,
    build_training_messages,
)
from evaluate_models import (  # noqa: E402
    flatten_summary,
    parse_summary,
    percentile,
    rouge_l_f1,
)


MAX_NEW_TOKENS = 192


class LoraEvaluationError(RuntimeError):
    pass


def reference_summary(sample: dict[str, Any]) -> dict[str, list[str]]:
    return {
        field: sample["referenceSummary"].get(field, [])
        for field in SUMMARY_FIELDS
    }


def build_sample_result(
    sample: dict[str, Any],
    raw_output: str,
    latency_seconds: float,
    input_tokens: int,
    output_tokens: int,
) -> dict[str, Any]:
    parsed = parse_summary(raw_output)
    expected = reference_summary(sample)
    completed_sections = sum(
        1 for field in SUMMARY_FIELDS if parsed.summary.get(field)
    )
    return {
        "sampleId": sample["sampleId"],
        "latencySeconds": round(latency_seconds, 4),
        "strictJsonObject": parsed.strict_json_object,
        "recoverableJsonObject": parsed.recoverable_json_object,
        "schemaValid": parsed.schema_valid,
        "sectionCompleteness": round(completed_sections / len(SUMMARY_FIELDS), 4),
        "rougeL": round(
            rouge_l_f1(
                flatten_summary(expected),
                flatten_summary(parsed.summary),
            ),
            4,
        ),
        "usage": {
            "inputTokens": input_tokens,
            "outputTokens": output_tokens,
        },
        "output": raw_output,
        "parsedSummary": parsed.summary,
        "referenceSummary": expected,
    }


def aggregate_results(results: list[dict[str, Any]]) -> dict[str, Any]:
    if not results:
        raise LoraEvaluationError("평가 결과가 비어 있습니다.")
    latencies = [result["latencySeconds"] for result in results]
    return {
        "sampleCount": len(results),
        "latencyMedianSeconds": round(statistics.median(latencies), 4),
        "latencyP95Seconds": round(percentile(latencies, 0.95), 4),
        "strictJsonObjectRate": round(
            sum(result["strictJsonObject"] for result in results) / len(results),
            4,
        ),
        "recoverableJsonObjectRate": round(
            sum(result["recoverableJsonObject"] for result in results) / len(results),
            4,
        ),
        "schemaValidRate": round(
            sum(result["schemaValid"] for result in results) / len(results),
            4,
        ),
        "sectionCompletenessAverage": round(
            statistics.mean(result["sectionCompleteness"] for result in results),
            4,
        ),
        "rougeLAverage": round(
            statistics.mean(result["rougeL"] for result in results),
            4,
        ),
    }


def generate_summary(
    model: Any,
    tokenizer: Any,
    sample: dict[str, Any],
    torch_module: Any,
    adapter_enabled: bool,
) -> tuple[str, float, int, int]:
    messages = build_training_messages(sample)[:-1]
    prompt = tokenizer.apply_chat_template(
        messages,
        tokenize=False,
        add_generation_prompt=True,
        enable_thinking=False,
    )
    inputs = tokenizer(
        prompt,
        add_special_tokens=False,
        return_tensors="pt",
    ).to(model.device)
    input_length = inputs["input_ids"].shape[-1]
    adapter_context = nullcontext() if adapter_enabled else model.disable_adapter()
    torch_module.cuda.synchronize()
    started_at = time.perf_counter()
    with adapter_context, torch_module.inference_mode():
        output = model.generate(
            **inputs,
            max_new_tokens=MAX_NEW_TOKENS,
            do_sample=False,
            pad_token_id=tokenizer.pad_token_id,
            eos_token_id=tokenizer.eos_token_id,
        )
    torch_module.cuda.synchronize()
    latency_seconds = time.perf_counter() - started_at
    generated_ids = output[0, input_length:]
    text = tokenizer.decode(generated_ids, skip_special_tokens=True).strip()
    return text, latency_seconds, input_length, generated_ids.shape[-1]


def evaluate(config_path: Path, adapter_dir: Path) -> dict[str, Any]:
    try:
        import peft
        import torch
        import transformers
        from peft import PeftModel
        from transformers import AutoModelForCausalLM, AutoTokenizer
    except ImportError as error:
        raise LoraEvaluationError(
            "평가 의존성이 없습니다. ai/training/requirements.txt를 설치해 주세요."
        ) from error

    if not torch.cuda.is_available():
        raise LoraEvaluationError("CUDA GPU를 사용할 수 없습니다.")

    config = load_training_config(config_path)
    if config["promptVersion"] != PROMPT_VERSION:
        raise LoraEvaluationError("학습 설정과 평가 프롬프트 버전이 다릅니다.")
    test_path = resolve_project_path(config["dataset"]["test"])
    samples = read_jsonl(test_path)
    base_model = config["baseModel"]

    tokenizer = AutoTokenizer.from_pretrained(
        base_model["id"],
        revision=base_model["revision"],
    )
    if tokenizer.pad_token_id is None:
        tokenizer.pad_token = tokenizer.eos_token
    model = AutoModelForCausalLM.from_pretrained(
        base_model["id"],
        revision=base_model["revision"],
        dtype=torch.float16,
        attn_implementation="eager",
    )
    model = PeftModel.from_pretrained(model, adapter_dir)
    model.eval()
    model.config.use_cache = True

    candidates = {
        "base": False,
        "lora": True,
    }
    results: dict[str, Any] = {}
    for candidate, adapter_enabled in candidates.items():
        sample_results: list[dict[str, Any]] = []
        for sample in samples:
            raw_output, latency, input_tokens, output_tokens = generate_summary(
                model,
                tokenizer,
                sample,
                torch,
                adapter_enabled,
            )
            sample_results.append(
                build_sample_result(
                    sample,
                    raw_output,
                    latency,
                    input_tokens,
                    output_tokens,
                )
            )
        results[candidate] = {
            "aggregate": aggregate_results(sample_results),
            "samples": sample_results,
        }

    gpu_properties = torch.cuda.get_device_properties(0)
    return {
        "schemaVersion": "1.0",
        "runId": config["runId"],
        "baseModel": base_model,
        "adapter": {
            "path": str(Path(config["outputDir"]) / "adapter"),
            "configSha256": file_sha256(adapter_dir / "adapter_config.json"),
            "weightsSha256": file_sha256(adapter_dir / "adapter_model.safetensors"),
        },
        "dataset": {
            "version": config["dataset"]["version"],
            "split": "test",
            "path": config["dataset"]["test"],
            "sha256": file_sha256(test_path),
            "sampleCount": len(samples),
        },
        "promptVersion": config["promptVersion"],
        "generation": {
            "doSample": False,
            "maxNewTokens": MAX_NEW_TOKENS,
        },
        "environment": {
            "python": platform.python_version(),
            "torch": torch.__version__,
            "transformers": transformers.__version__,
            "peft": peft.__version__,
            "gpu": gpu_properties.name,
            "gpuMemoryMiB": round(gpu_properties.total_memory / 2**20, 2),
        },
        "candidates": results,
    }


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="동일 Qwen3 모델에서 LoRA 적용 전후 리뷰 요약을 비교합니다."
    )
    parser.add_argument("--config", type=Path, required=True)
    parser.add_argument("--adapter-dir", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    report = evaluate(args.config, args.adapter_dir)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(
        json.dumps(report, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    print(json.dumps(
        {
            candidate: result["aggregate"]
            for candidate, result in report["candidates"].items()
        },
        ensure_ascii=False,
        indent=2,
    ))


if __name__ == "__main__":
    main()
