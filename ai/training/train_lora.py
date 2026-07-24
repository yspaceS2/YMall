from __future__ import annotations

import argparse
import json
import platform
import random
from pathlib import Path
from typing import Any


try:
    from .prepare_sft_data import (
        file_sha256,
        load_training_config,
        resolve_project_path,
    )
except ImportError:
    from prepare_sft_data import (  # type: ignore[no-redef]
        file_sha256,
        load_training_config,
        resolve_project_path,
    )


class TrainingError(RuntimeError):
    pass


def load_jsonl(path: Path) -> list[dict[str, Any]]:
    records = [
        json.loads(line)
        for line in path.read_text(encoding="utf-8").splitlines()
        if line.strip()
    ]
    if not records:
        raise TrainingError(f"학습 데이터가 비어 있습니다: {path}")
    return records


def set_reproducible_seed(seed: int, torch_module: Any, numpy_module: Any) -> None:
    random.seed(seed)
    numpy_module.random.seed(seed)
    torch_module.manual_seed(seed)
    torch_module.cuda.manual_seed_all(seed)
    torch_module.backends.cudnn.deterministic = True
    torch_module.backends.cudnn.benchmark = False


class ReviewSummaryDataset:

    def __init__(
        self,
        records: list[dict[str, Any]],
        tokenizer: Any,
        max_length: int,
        torch_module: Any,
    ) -> None:
        self.items: list[dict[str, Any]] = []
        eos_token_id = tokenizer.eos_token_id
        if eos_token_id is None:
            raise TrainingError("토크나이저 eos_token_id가 필요합니다.")

        for record in records:
            messages = record["messages"]
            if not messages or messages[-1].get("role") != "assistant":
                raise TrainingError("마지막 메시지는 assistant 기준 요약이어야 합니다.")
            prompt_messages = messages[:-1]
            target = messages[-1]["content"]
            prompt_text = tokenizer.apply_chat_template(
                prompt_messages,
                tokenize=False,
                add_generation_prompt=True,
                enable_thinking=False,
            )
            prompt_ids = tokenizer(
                prompt_text,
                add_special_tokens=False,
            )["input_ids"]
            target_ids = tokenizer(
                target,
                add_special_tokens=False,
            )["input_ids"] + [eos_token_id]
            if len(target_ids) >= max_length:
                raise TrainingError(
                    f"{record['sampleId']} 기준 요약이 maxLength보다 깁니다."
                )
            available_prompt_length = max_length - len(target_ids)
            prompt_ids = prompt_ids[-available_prompt_length:]
            input_ids = prompt_ids + target_ids
            labels = [-100] * len(prompt_ids) + target_ids
            self.items.append({
                "input_ids": torch_module.tensor(input_ids, dtype=torch_module.long),
                "labels": torch_module.tensor(labels, dtype=torch_module.long),
                "attention_mask": torch_module.ones(len(input_ids), dtype=torch_module.long),
            })

    def __len__(self) -> int:
        return len(self.items)

    def __getitem__(self, index: int) -> dict[str, Any]:
        return self.items[index]


class ReviewSummaryCollator:

    def __init__(self, pad_token_id: int, torch_module: Any) -> None:
        self.pad_token_id = pad_token_id
        self.torch = torch_module

    def __call__(self, features: list[dict[str, Any]]) -> dict[str, Any]:
        pad_sequence = self.torch.nn.utils.rnn.pad_sequence
        return {
            "input_ids": pad_sequence(
                [feature["input_ids"] for feature in features],
                batch_first=True,
                padding_value=self.pad_token_id,
            ),
            "labels": pad_sequence(
                [feature["labels"] for feature in features],
                batch_first=True,
                padding_value=-100,
            ),
            "attention_mask": pad_sequence(
                [feature["attention_mask"] for feature in features],
                batch_first=True,
                padding_value=0,
            ),
        }


def adapter_hashes(adapter_dir: Path) -> dict[str, str]:
    artifact_names = (
        "adapter_config.json",
        "adapter_model.safetensors",
    )
    return {
        artifact_name: file_sha256(adapter_dir / artifact_name)
        for artifact_name in artifact_names
    }


def validate_generated_data(
    data_dir: Path,
    data_manifest: dict[str, Any],
) -> None:
    generated = data_manifest.get("generated")
    if not isinstance(generated, dict):
        raise TrainingError("SFT 데이터 manifest의 generated 정보가 없습니다.")

    for split in ("train", "validation", "test"):
        split_metadata = generated.get(split)
        if not isinstance(split_metadata, dict):
            raise TrainingError(f"SFT 데이터 manifest에 {split} 정보가 없습니다.")
        expected_hash = split_metadata.get("sha256")
        if not isinstance(expected_hash, str) or not expected_hash:
            raise TrainingError(f"SFT 데이터 manifest에 {split} 해시가 없습니다.")

        split_path = data_dir / f"{split}.jsonl"
        if not split_path.is_file():
            raise TrainingError(f"SFT {split} 데이터가 없습니다: {split_path}")
        if file_sha256(split_path) != expected_hash:
            raise TrainingError(
                f"SFT {split} 데이터 해시가 manifest와 일치하지 않습니다."
            )


def train(config_path: Path, data_dir: Path) -> dict[str, Any]:
    try:
        import numpy
        import peft
        import torch
        import transformers
        from peft import LoraConfig, get_peft_model
        from transformers import (
            AutoModelForCausalLM,
            AutoTokenizer,
            Trainer,
            TrainingArguments,
        )
    except ImportError as error:
        raise TrainingError(
            "GPU 학습 의존성이 없습니다. ai/training/requirements.txt를 설치해 주세요."
        ) from error

    if not torch.cuda.is_available():
        raise TrainingError("CUDA GPU를 사용할 수 없습니다.")

    config = load_training_config(config_path)
    seed = config["seed"]
    set_reproducible_seed(seed, torch, numpy)
    base_model = config["baseModel"]
    training_config = config["training"]
    lora_config = config["lora"]
    output_dir = resolve_project_path(config["outputDir"])
    adapter_dir = output_dir / "adapter"
    checkpoint_dir = output_dir / "checkpoints"
    output_dir.mkdir(parents=True, exist_ok=True)

    data_manifest_path = data_dir / "manifest.json"
    if not data_manifest_path.exists():
        raise TrainingError(f"SFT 데이터 manifest가 없습니다: {data_manifest_path}")
    data_manifest = json.loads(data_manifest_path.read_text(encoding="utf-8"))
    if data_manifest.get("config", {}).get("sha256") != file_sha256(config_path):
        raise TrainingError("SFT 데이터와 학습 설정의 해시가 일치하지 않습니다.")
    if data_manifest.get("runId") != config["runId"]:
        raise TrainingError("SFT 데이터와 학습 설정의 runId가 일치하지 않습니다.")
    if data_manifest.get("promptVersion") != config["promptVersion"]:
        raise TrainingError("SFT 데이터와 학습 설정의 프롬프트 버전이 일치하지 않습니다.")
    validate_generated_data(data_dir, data_manifest)

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
    model.config.use_cache = False
    model.gradient_checkpointing_enable(
        gradient_checkpointing_kwargs={"use_reentrant": False}
    )
    model.enable_input_require_grads()
    model = get_peft_model(
        model,
        LoraConfig(
            task_type="CAUSAL_LM",
            r=lora_config["rank"],
            lora_alpha=lora_config["alpha"],
            lora_dropout=lora_config["dropout"],
            target_modules=lora_config["targetModules"],
            bias="none",
        ),
    )
    trainable_parameters = sum(
        parameter.numel()
        for parameter in model.parameters()
        if parameter.requires_grad
    )
    total_parameters = sum(parameter.numel() for parameter in model.parameters())

    train_dataset = ReviewSummaryDataset(
        load_jsonl(data_dir / "train.jsonl"),
        tokenizer,
        training_config["maxLength"],
        torch,
    )
    validation_dataset = ReviewSummaryDataset(
        load_jsonl(data_dir / "validation.jsonl"),
        tokenizer,
        training_config["maxLength"],
        torch,
    )
    collator = ReviewSummaryCollator(tokenizer.pad_token_id, torch)

    arguments = TrainingArguments(
        output_dir=str(checkpoint_dir),
        num_train_epochs=training_config["epochs"],
        learning_rate=training_config["learningRate"],
        weight_decay=training_config["weightDecay"],
        per_device_train_batch_size=training_config["batchSize"],
        per_device_eval_batch_size=1,
        gradient_accumulation_steps=training_config["gradientAccumulationSteps"],
        warmup_steps=training_config["warmupSteps"],
        max_grad_norm=training_config["maxGradientNorm"],
        fp16=True,
        bf16=False,
        gradient_checkpointing=True,
        gradient_checkpointing_kwargs={"use_reentrant": False},
        eval_strategy="epoch",
        save_strategy="epoch",
        save_total_limit=2,
        load_best_model_at_end=True,
        metric_for_best_model="eval_loss",
        greater_is_better=False,
        logging_steps=1,
        report_to="none",
        seed=seed,
        data_seed=seed,
        optim="adamw_torch",
        remove_unused_columns=False,
    )
    trainer = Trainer(
        model=model,
        args=arguments,
        train_dataset=train_dataset,
        eval_dataset=validation_dataset,
        data_collator=collator,
        processing_class=tokenizer,
    )
    train_result = trainer.train()
    evaluation_result = trainer.evaluate()
    trainer.save_model(str(adapter_dir))
    tokenizer.save_pretrained(adapter_dir)

    gpu_properties = torch.cuda.get_device_properties(0)
    manifest = {
        "schemaVersion": "1.0",
        "runId": config["runId"],
        "baseModel": base_model,
        "promptVersion": config["promptVersion"],
        "seed": seed,
        "config": {
            "path": str(config_path),
            "sha256": file_sha256(config_path),
        },
        "dataManifest": {
            "path": str(data_manifest_path),
            "sha256": file_sha256(data_manifest_path),
        },
        "adapter": {
            "path": str(Path(config["outputDir"]) / "adapter"),
            "files": adapter_hashes(adapter_dir),
        },
        "parameters": {
            "total": total_parameters,
            "trainable": trainable_parameters,
            "trainableRatio": round(trainable_parameters / total_parameters, 8),
        },
        "metrics": {
            "train": train_result.metrics,
            "validation": evaluation_result,
        },
        "environment": {
            "python": platform.python_version(),
            "torch": torch.__version__,
            "transformers": transformers.__version__,
            "peft": peft.__version__,
            "gpu": gpu_properties.name,
            "gpuMemoryMiB": round(gpu_properties.total_memory / 2**20, 2),
            "peakGpuMemoryMiB": round(torch.cuda.max_memory_allocated() / 2**20, 2),
        },
    }
    manifest_path = output_dir / "run-manifest.json"
    manifest_path.write_text(
        json.dumps(manifest, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    return manifest


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Qwen3 리뷰 요약 LoRA를 학습합니다.")
    parser.add_argument("--config", type=Path, required=True)
    parser.add_argument("--data-dir", type=Path, required=True)
    parser.add_argument("--result", type=Path)
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    manifest = train(args.config, args.data_dir)
    if args.result:
        args.result.parent.mkdir(parents=True, exist_ok=True)
        args.result.write_text(
            json.dumps(manifest, ensure_ascii=False, indent=2) + "\n",
            encoding="utf-8",
        )
    print(json.dumps(manifest, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
