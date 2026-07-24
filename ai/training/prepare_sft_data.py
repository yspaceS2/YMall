from __future__ import annotations

import argparse
import hashlib
import json
import sys
from pathlib import Path
from typing import Any


AI_DIR = Path(__file__).resolve().parents[1]
PROJECT_ROOT = AI_DIR.parent
COMMON_DIR = AI_DIR / "common"
if str(COMMON_DIR) not in sys.path:
    sys.path.insert(0, str(COMMON_DIR))

from review_summary import (  # noqa: E402
    PROMPT_VERSION,
    build_training_messages,
)


class TrainingDataError(ValueError):
    pass


def file_sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as file:
        for chunk in iter(lambda: file.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def resolve_project_path(value: str) -> Path:
    path = Path(value)
    return path if path.is_absolute() else PROJECT_ROOT / path


def read_jsonl(path: Path) -> list[dict[str, Any]]:
    records: list[dict[str, Any]] = []
    for line_number, line in enumerate(path.read_text(encoding="utf-8").splitlines(), start=1):
        if not line.strip():
            continue
        try:
            record = json.loads(line)
        except json.JSONDecodeError as error:
            raise TrainingDataError(f"{path}:{line_number} JSON 형식 오류") from error
        if not isinstance(record, dict):
            raise TrainingDataError(f"{path}:{line_number} JSON 객체여야 합니다.")
        records.append(record)
    if not records:
        raise TrainingDataError(f"비어 있는 데이터 분할입니다: {path}")
    return records


def load_training_config(path: Path) -> dict[str, Any]:
    payload = json.loads(path.read_text(encoding="utf-8"))
    if payload.get("schemaVersion") != "1.0":
        raise TrainingDataError("지원하지 않는 학습 설정 schemaVersion입니다.")
    if payload.get("promptVersion") != PROMPT_VERSION:
        raise TrainingDataError(
            f"프롬프트 버전이 일치하지 않습니다: {payload.get('promptVersion')}"
        )
    if not payload.get("runId") or not isinstance(payload.get("seed"), int):
        raise TrainingDataError("runId와 정수 seed가 필요합니다.")
    base_model = payload.get("baseModel", {})
    for field in ("id", "revision", "license"):
        if not base_model.get(field):
            raise TrainingDataError(f"baseModel.{field}가 필요합니다.")
    dataset = payload.get("dataset", {})
    for split in ("train", "validation", "test"):
        if not dataset.get(split):
            raise TrainingDataError(f"dataset.{split} 경로가 필요합니다.")
    return payload


def assert_split_isolation(splits: dict[str, list[dict[str, Any]]]) -> None:
    groups = {
        split: {record["productGroupId"] for record in records}
        for split, records in splits.items()
    }
    split_names = tuple(groups)
    for index, left_name in enumerate(split_names):
        for right_name in split_names[index + 1:]:
            overlap = groups[left_name] & groups[right_name]
            if overlap:
                raise TrainingDataError(
                    f"{left_name}/{right_name} 상품 그룹이 겹칩니다: {sorted(overlap)}"
                )


def convert_record(record: dict[str, Any]) -> dict[str, Any]:
    for field in ("sampleId", "productGroupId", "reviews", "referenceSummary"):
        if field not in record:
            raise TrainingDataError(f"학습 레코드에 {field}가 필요합니다.")
    return {
        "sampleId": record["sampleId"],
        "productGroupId": record["productGroupId"],
        "messages": build_training_messages(record),
    }


def write_jsonl(path: Path, records: list[dict[str, Any]]) -> None:
    path.write_text(
        "".join(
            json.dumps(record, ensure_ascii=False, separators=(",", ":")) + "\n"
            for record in records
        ),
        encoding="utf-8",
    )


def prepare_sft_data(config_path: Path, output_dir: Path) -> dict[str, Any]:
    config = load_training_config(config_path)
    dataset_config = config["dataset"]
    source_paths = {
        split: resolve_project_path(dataset_config[split])
        for split in ("train", "validation", "test")
    }
    source_records = {
        split: read_jsonl(path)
        for split, path in source_paths.items()
    }
    assert_split_isolation(source_records)

    output_dir.mkdir(parents=True, exist_ok=True)
    output_paths: dict[str, Path] = {}
    for split, records in source_records.items():
        output_path = output_dir / f"{split}.jsonl"
        write_jsonl(output_path, [convert_record(record) for record in records])
        output_paths[split] = output_path

    manifest = {
        "schemaVersion": "1.0",
        "runId": config["runId"],
        "promptVersion": config["promptVersion"],
        "seed": config["seed"],
        "config": {
            "path": str(config_path),
            "sha256": file_sha256(config_path),
        },
        "source": {
            split: {
                "path": str(source_paths[split]),
                "sha256": file_sha256(source_paths[split]),
                "sampleCount": len(source_records[split]),
            }
            for split in source_paths
        },
        "generated": {
            split: {
                "path": str(output_paths[split]),
                "sha256": file_sha256(output_paths[split]),
                "sampleCount": len(source_records[split]),
            }
            for split in output_paths
        },
    }
    manifest_path = output_dir / "manifest.json"
    manifest_path.write_text(
        json.dumps(manifest, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    return manifest


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="리뷰 요약 SFT 데이터를 생성합니다.")
    parser.add_argument("--config", type=Path, required=True)
    parser.add_argument("--output-dir", type=Path, required=True)
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    manifest = prepare_sft_data(args.config, args.output_dir)
    print(json.dumps(manifest, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
