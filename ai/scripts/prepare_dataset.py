from __future__ import annotations

import argparse
import hashlib
import json
import re
import unicodedata
from collections import Counter
from pathlib import Path
from typing import Any


SCHEMA_VERSION = "1.0"
DEFAULT_MIN_REVIEWS = 3
DEFAULT_MIN_CONTENT_LENGTH = 10
DEFAULT_MAX_CONTENT_LENGTH = 2000

EMAIL_PATTERN = re.compile(r"[\w.+-]+@[\w-]+(?:\.[\w-]+)+", re.IGNORECASE)
PHONE_PATTERN = re.compile(r"(?<!\d)(?:\+?82[-.\s]?)?0?1[016789][-.\s]?\d{3,4}[-.\s]?\d{4}(?!\d)")
PERSON_ID_PATTERN = re.compile(r"(?<!\d)\d{6}[-\s]?[1-4]\d{6}(?!\d)")
URL_PATTERN = re.compile(r"(?:https?://|www\.)\S+", re.IGNORECASE)
WHITESPACE_PATTERN = re.compile(r"\s+")

AD_MARKERS = (
    "협찬",
    "체험단",
    "광고입니다",
    "문의주세요",
    "오픈채팅",
    "카톡문의",
)
ABUSIVE_MARKERS = (
    "씨발",
    "ㅅㅂ",
    "병신",
)


class DatasetError(ValueError):
    pass


def normalize_text(value: str) -> str:
    normalized = unicodedata.normalize("NFKC", value)
    return WHITESPACE_PATTERN.sub(" ", normalized).strip()


def mask_personal_data(value: str) -> tuple[str, set[str]]:
    masked = value
    found: set[str] = set()
    for name, pattern, replacement in (
        ("email", EMAIL_PATTERN, "[EMAIL]"),
        ("phone", PHONE_PATTERN, "[PHONE]"),
        ("person_id", PERSON_ID_PATTERN, "[PERSON_ID]"),
        ("url", URL_PATTERN, "[URL]"),
    ):
        masked, count = pattern.subn(replacement, masked)
        if count:
            found.add(name)
    return masked, found


def has_marker(value: str, markers: tuple[str, ...]) -> bool:
    lowered = value.casefold()
    return any(marker.casefold() in lowered for marker in markers)


def content_fingerprint(value: str) -> str:
    canonical = re.sub(r"[\W_]+", "", value.casefold())
    return hashlib.sha256(canonical.encode("utf-8")).hexdigest()


def load_sources(path: Path) -> dict[str, dict[str, Any]]:
    payload = json.loads(path.read_text(encoding="utf-8"))
    sources: dict[str, dict[str, Any]] = {}
    for source in payload.get("sources", []):
        source_id = source.get("sourceId")
        if not source_id:
            raise DatasetError("출처 항목에 sourceId가 필요합니다.")
        if source_id in sources:
            raise DatasetError(f"중복된 sourceId입니다: {source_id}")
        if not source.get("licenseId") or not source.get("reviewedAt"):
            raise DatasetError(f"출처의 라이선스와 검토일이 필요합니다: {source_id}")
        sources[source_id] = source
    return sources


def read_jsonl(path: Path) -> list[dict[str, Any]]:
    records: list[dict[str, Any]] = []
    for line_number, line in enumerate(path.read_text(encoding="utf-8").splitlines(), start=1):
        if not line.strip():
            continue
        try:
            records.append(json.loads(line))
        except json.JSONDecodeError as error:
            raise DatasetError(f"{path}:{line_number} JSON 형식이 올바르지 않습니다.") from error
    return records


def validate_required_fields(sample: dict[str, Any]) -> None:
    required = (
        "schemaVersion",
        "sampleId",
        "productGroupId",
        "category",
        "sourceId",
        "reviews",
        "referenceSummary",
        "annotation",
    )
    missing = [field for field in required if field not in sample]
    if missing:
        raise DatasetError(f"필수 필드가 없습니다: {', '.join(missing)}")
    if sample["schemaVersion"] != SCHEMA_VERSION:
        raise DatasetError(f"지원하지 않는 schemaVersion입니다: {sample['schemaVersion']}")
    for field in ("sampleId", "productGroupId", "category", "sourceId"):
        if not isinstance(sample[field], str) or not sample[field].strip():
            raise DatasetError(f"{field}는 비어 있지 않은 문자열이어야 합니다.")
    if not isinstance(sample["reviews"], list):
        raise DatasetError("reviews는 배열이어야 합니다.")
    if not isinstance(sample["referenceSummary"], dict):
        raise DatasetError("referenceSummary는 객체여야 합니다.")
    if not isinstance(sample["annotation"], dict):
        raise DatasetError("annotation은 객체여야 합니다.")
    if sample["annotation"].get("status") not in {"draft", "reviewed", "approved"}:
        raise DatasetError("annotation.status가 올바르지 않습니다.")


def prepare_sample(
    sample: dict[str, Any],
    sources: dict[str, dict[str, Any]],
    minimum_reviews: int = DEFAULT_MIN_REVIEWS,
) -> tuple[dict[str, Any] | None, Counter[str]]:
    validate_required_fields(sample)
    stats: Counter[str] = Counter()
    source_id = sample["sourceId"]
    source = sources.get(source_id)
    if source is None:
        raise DatasetError(f"등록되지 않은 데이터 출처입니다: {source_id}")
    if not source.get("approvedForTraining", False):
        stats["sample_unapproved_source"] += 1
        return None, stats
    if sample["annotation"]["status"] != "approved":
        stats["sample_unapproved_annotation"] += 1
        return None, stats

    processed_reviews: list[dict[str, Any]] = []
    review_ids: set[str] = set()
    fingerprints: set[str] = set()
    masked_types: Counter[str] = Counter()

    for review in sample["reviews"]:
        if not isinstance(review, dict):
            stats["review_invalid_content"] += 1
            continue
        review_id = str(review.get("reviewId", "")).strip()
        rating = review.get("rating")
        content = review.get("content")
        if not review_id or review_id in review_ids:
            stats["review_invalid_id"] += 1
            continue
        review_ids.add(review_id)
        if not isinstance(rating, int) or not 1 <= rating <= 5:
            stats["review_invalid_rating"] += 1
            continue
        if not isinstance(content, str):
            stats["review_invalid_content"] += 1
            continue

        normalized = normalize_text(content)
        if len(normalized) < DEFAULT_MIN_CONTENT_LENGTH:
            stats["review_too_short"] += 1
            continue
        if len(normalized) > DEFAULT_MAX_CONTENT_LENGTH:
            stats["review_too_long"] += 1
            continue
        if has_marker(normalized, AD_MARKERS) or URL_PATTERN.search(normalized):
            stats["review_advertisement"] += 1
            continue
        if has_marker(normalized, ABUSIVE_MARKERS):
            stats["review_abusive"] += 1
            continue

        masked, personal_data_types = mask_personal_data(normalized)
        masked_types.update(personal_data_types)
        fingerprint = content_fingerprint(masked)
        if fingerprint in fingerprints:
            stats["review_duplicate"] += 1
            continue
        fingerprints.add(fingerprint)
        processed_reviews.append({
            "reviewId": review_id,
            "rating": rating,
            "content": masked,
        })

    if len(processed_reviews) < minimum_reviews:
        stats["sample_insufficient_reviews"] += 1
        return None, stats

    kept_review_ids = {review["reviewId"] for review in processed_reviews}
    reference = sample["referenceSummary"]
    raw_evidence_ids = reference.get("evidenceReviewIds", [])
    if not isinstance(raw_evidence_ids, list) or not all(
        isinstance(review_id, str) for review_id in raw_evidence_ids
    ):
        raise DatasetError("referenceSummary.evidenceReviewIds는 문자열 배열이어야 합니다.")
    evidence_ids = set(raw_evidence_ids)
    if not evidence_ids or not evidence_ids.issubset(kept_review_ids):
        stats["sample_invalid_evidence"] += 1
        return None, stats

    def clean_summary_items(field: str) -> list[str]:
        values = reference.get(field, [])
        if not isinstance(values, list) or not all(isinstance(value, str) for value in values):
            raise DatasetError(f"referenceSummary.{field}는 문자열 배열이어야 합니다.")
        return [
            mask_personal_data(normalize_text(value))[0]
            for value in values
            if value.strip()
        ]

    prepared = {
        "schemaVersion": SCHEMA_VERSION,
        "sampleId": str(sample["sampleId"]),
        "productGroupId": str(sample["productGroupId"]),
        "category": normalize_text(str(sample["category"])),
        "sourceId": source_id,
        "reviews": processed_reviews,
        "referenceSummary": {
            "pros": clean_summary_items("pros"),
            "cons": clean_summary_items("cons"),
            "commonOpinions": clean_summary_items("commonOpinions"),
            "evidenceReviewIds": sorted(evidence_ids),
        },
        "annotation": sample["annotation"],
        "processing": {
            "maskedPersonalDataTypes": sorted(masked_types),
            "removedReviewCount": len(sample["reviews"]) - len(processed_reviews),
        },
    }
    stats["sample_accepted"] += 1
    stats["review_accepted"] += len(processed_reviews)
    return prepared, stats


def choose_split(product_group_id: str, seed: str) -> str:
    digest = hashlib.sha256(f"{seed}:{product_group_id}".encode("utf-8")).digest()
    ratio = int.from_bytes(digest[:8], byteorder="big") / 2**64
    if ratio < 0.8:
        return "train"
    if ratio < 0.9:
        return "validation"
    return "test"


def assign_splits(product_group_ids: list[str], seed: str) -> dict[str, str]:
    unique_ids = set(product_group_ids)
    if len(unique_ids) != len(product_group_ids):
        raise DatasetError("분할 대상에 중복 productGroupId가 있습니다.")

    ordered_ids = sorted(
        product_group_ids,
        key=lambda product_group_id: hashlib.sha256(
            f"{seed}:{product_group_id}".encode("utf-8")
        ).digest(),
    )
    sample_count = len(ordered_ids)
    if sample_count == 0:
        return {}
    if sample_count == 1:
        return {ordered_ids[0]: "train"}
    if sample_count == 2:
        return {
            ordered_ids[0]: "test",
            ordered_ids[1]: "train",
        }

    test_count = max(1, round(sample_count * 0.1))
    validation_count = max(1, round(sample_count * 0.1))
    if test_count + validation_count >= sample_count:
        test_count = 1
        validation_count = 1

    assignments: dict[str, str] = {}
    for index, product_group_id in enumerate(ordered_ids):
        if index < test_count:
            assignments[product_group_id] = "test"
        elif index < test_count + validation_count:
            assignments[product_group_id] = "validation"
        else:
            assignments[product_group_id] = "train"
    return assignments


def write_jsonl(path: Path, records: list[dict[str, Any]]) -> None:
    lines = [json.dumps(record, ensure_ascii=False, sort_keys=True) for record in records]
    path.write_text("\n".join(lines) + ("\n" if lines else ""), encoding="utf-8")


def prepare_dataset(
    input_path: Path,
    sources_path: Path,
    output_dir: Path,
    seed: str,
    minimum_reviews: int,
) -> dict[str, Any]:
    sources = load_sources(sources_path)
    records = read_jsonl(input_path)
    splits: dict[str, list[dict[str, Any]]] = {
        "train": [],
        "validation": [],
        "test": [],
    }
    totals: Counter[str] = Counter()
    product_groups: set[str] = set()
    sample_ids: set[str] = set()
    prepared_records: list[dict[str, Any]] = []

    for record in records:
        sample_id = str(record.get("sampleId", ""))
        if sample_id in sample_ids:
            raise DatasetError(f"입력에 중복 sampleId가 있습니다: {sample_id}")
        sample_ids.add(sample_id)
        product_group_id = str(record.get("productGroupId", ""))
        if product_group_id in product_groups:
            raise DatasetError(f"입력에 중복 productGroupId가 있습니다: {product_group_id}")
        product_groups.add(product_group_id)
        prepared, stats = prepare_sample(record, sources, minimum_reviews)
        totals.update(stats)
        if prepared is not None:
            prepared_records.append(prepared)

    assignments = assign_splits(
        [record["productGroupId"] for record in prepared_records],
        seed,
    )
    for prepared in prepared_records:
        splits[assignments[prepared["productGroupId"]]].append(prepared)

    output_dir.mkdir(parents=True, exist_ok=True)
    for split_name, split_records in splits.items():
        write_jsonl(output_dir / f"{split_name}.jsonl", split_records)

    report = {
        "schemaVersion": SCHEMA_VERSION,
        "input": str(input_path),
        "seed": seed,
        "minimumReviews": minimum_reviews,
        "splitCounts": {name: len(values) for name, values in splits.items()},
        "statistics": dict(sorted(totals.items())),
    }
    (output_dir / "report.json").write_text(
        json.dumps(report, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    return report


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="YMall 리뷰 요약 데이터셋을 정제하고 분할합니다.")
    parser.add_argument("--input", type=Path, required=True)
    parser.add_argument("--sources", type=Path, required=True)
    parser.add_argument("--output-dir", type=Path, required=True)
    parser.add_argument("--seed", default="ymall-review-summary-v1")
    parser.add_argument("--minimum-reviews", type=int, default=DEFAULT_MIN_REVIEWS)
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    report = prepare_dataset(
        input_path=args.input,
        sources_path=args.sources,
        output_dir=args.output_dir,
        seed=args.seed,
        minimum_reviews=args.minimum_reviews,
    )
    print(json.dumps(report, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
