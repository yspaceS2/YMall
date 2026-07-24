import json
import sys
import tempfile
import unittest
from pathlib import Path


SCRIPT_DIR = Path(__file__).resolve().parents[1] / "scripts"
sys.path.insert(0, str(SCRIPT_DIR))

from prepare_dataset import (  # noqa: E402
    DatasetError,
    assign_splits,
    choose_split,
    prepare_dataset,
    prepare_sample,
    read_jsonl,
)


def approved_sources():
    return {
        "test-source": {
            "sourceId": "test-source",
            "licenseId": "test-license",
            "reviewedAt": "2026-07-24",
            "approvedForTraining": True,
        }
    }


def sample():
    return {
        "schemaVersion": "1.0",
        "sampleId": "sample-1",
        "productGroupId": "product-group-1",
        "category": "테스트",
        "sourceId": "test-source",
        "reviews": [
            {"reviewId": "r1", "rating": 5, "content": "제품 마감이 깔끔하고 사용하는 동안 만족스러웠습니다."},
            {"reviewId": "r2", "rating": 4, "content": "배송이 빠르고 포장 상태도 안전하게 도착했습니다."},
            {"reviewId": "r3", "rating": 3, "content": "기능은 좋지만 크기가 예상보다 조금 크게 느껴집니다."},
        ],
        "referenceSummary": {
            "pros": ["마감과 배송 상태가 좋음"],
            "cons": ["크기가 크게 느껴질 수 있음"],
            "commonOpinions": ["전반적인 만족도가 높음"],
            "evidenceReviewIds": ["r1", "r2", "r3"],
        },
        "annotation": {"version": "1.0", "status": "approved"},
    }


class PrepareSampleTest(unittest.TestCase):

    def test_masks_personal_data(self):
        record = sample()
        record["reviews"][0]["content"] = (
            "제품 문의 답변은 만족스러웠지만 연락처 010-1234-5678과 "
            "메일 test@example.com은 리뷰에 남기지 않을게요."
        )

        prepared, stats = prepare_sample(record, approved_sources())

        self.assertIsNotNone(prepared)
        content = prepared["reviews"][0]["content"]
        self.assertNotIn("010-1234-5678", content)
        self.assertNotIn("test@example.com", content)
        self.assertIn("[PHONE]", content)
        self.assertIn("[EMAIL]", content)
        self.assertEqual({"email", "phone"}, set(prepared["processing"]["maskedPersonalDataTypes"]))
        self.assertEqual(1, stats["sample_accepted"])

    def test_rejects_sample_when_filtered_evidence_is_missing(self):
        record = sample()
        record["reviews"][2]["content"] = "짧음"

        prepared, stats = prepare_sample(record, approved_sources())

        self.assertIsNone(prepared)
        self.assertEqual(1, stats["sample_insufficient_reviews"])

    def test_filters_duplicate_review(self):
        record = sample()
        record["reviews"].append({
            "reviewId": "r4",
            "rating": 5,
            "content": "제품 마감이 깔끔하고 사용하는 동안 만족스러웠습니다!",
        })

        prepared, stats = prepare_sample(record, approved_sources())

        self.assertIsNotNone(prepared)
        self.assertEqual(3, len(prepared["reviews"]))
        self.assertEqual(1, stats["review_duplicate"])

    def test_rejects_unapproved_source(self):
        sources = approved_sources()
        sources["test-source"]["approvedForTraining"] = False

        prepared, stats = prepare_sample(sample(), sources)

        self.assertIsNone(prepared)
        self.assertEqual(1, stats["sample_unapproved_source"])

    def test_rejects_unapproved_annotation(self):
        record = sample()
        record["annotation"]["status"] = "reviewed"

        prepared, stats = prepare_sample(record, approved_sources())

        self.assertIsNone(prepared)
        self.assertEqual(1, stats["sample_unapproved_annotation"])

    def test_rejects_invalid_review_collection_type(self):
        record = sample()
        record["reviews"] = "not-a-list"

        with self.assertRaises(DatasetError):
            prepare_sample(record, approved_sources())

    def test_rejects_missing_annotation_version(self):
        record = sample()
        del record["annotation"]["version"]

        with self.assertRaises(DatasetError):
            prepare_sample(record, approved_sources())

    def test_rejects_missing_reference_summary_field(self):
        record = sample()
        del record["referenceSummary"]["commonOpinions"]

        with self.assertRaises(DatasetError):
            prepare_sample(record, approved_sources())

    def test_split_is_deterministic_and_product_based(self):
        first = choose_split("product-group-1", "fixed-seed")
        second = choose_split("product-group-1", "fixed-seed")

        self.assertEqual(first, second)
        self.assertIn(first, {"train", "validation", "test"})

    def test_small_dataset_has_all_splits(self):
        product_group_ids = [f"product-{index}" for index in range(6)]

        assignments = assign_splits(product_group_ids, "fixed-seed")

        self.assertEqual(
            {"train", "validation", "test"},
            set(assignments.values()),
        )
        self.assertEqual(
            assignments,
            assign_splits(list(reversed(product_group_ids)), "fixed-seed"),
        )


class PrepareDatasetTest(unittest.TestCase):

    def test_rejects_non_object_jsonl_with_line_number(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            input_path = Path(temp_dir) / "input.jsonl"
            input_path.write_text("[]\n", encoding="utf-8")

            with self.assertRaisesRegex(DatasetError, r"input\.jsonl:1"):
                read_jsonl(input_path)

    def test_writes_reproducible_split_and_report(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            root = Path(temp_dir)
            input_path = root / "input.jsonl"
            sources_path = root / "sources.json"
            output_dir = root / "output"
            input_path.write_text(
                json.dumps(sample(), ensure_ascii=False) + "\n",
                encoding="utf-8",
            )
            sources_path.write_text(
                json.dumps({"sources": list(approved_sources().values())}, ensure_ascii=False),
                encoding="utf-8",
            )

            report = prepare_dataset(
                input_path,
                sources_path,
                output_dir,
                seed="fixed-seed",
                minimum_reviews=3,
            )

            self.assertEqual(1, report["statistics"]["sample_accepted"])
            self.assertEqual(1, sum(report["splitCounts"].values()))
            self.assertTrue((output_dir / "report.json").exists())
            self.assertEqual(
                {"report.json", "test.jsonl", "train.jsonl", "validation.jsonl"},
                {path.name for path in output_dir.iterdir()},
            )

    def test_rejects_duplicate_product_group(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            root = Path(temp_dir)
            input_path = root / "input.jsonl"
            sources_path = root / "sources.json"
            record = json.dumps(sample(), ensure_ascii=False)
            input_path.write_text(f"{record}\n{record}\n", encoding="utf-8")
            sources_path.write_text(
                json.dumps({"sources": list(approved_sources().values())}, ensure_ascii=False),
                encoding="utf-8",
            )

            with self.assertRaises(DatasetError):
                prepare_dataset(
                    input_path,
                    sources_path,
                    root / "output",
                    seed="fixed-seed",
                    minimum_reviews=3,
                )

    def test_rejects_duplicate_sample_id(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            root = Path(temp_dir)
            input_path = root / "input.jsonl"
            sources_path = root / "sources.json"
            first = sample()
            second = sample()
            second["productGroupId"] = "product-group-2"
            input_path.write_text(
                "\n".join([
                    json.dumps(first, ensure_ascii=False),
                    json.dumps(second, ensure_ascii=False),
                ]) + "\n",
                encoding="utf-8",
            )
            sources_path.write_text(
                json.dumps({"sources": list(approved_sources().values())}, ensure_ascii=False),
                encoding="utf-8",
            )

            with self.assertRaises(DatasetError):
                prepare_dataset(
                    input_path,
                    sources_path,
                    root / "output",
                    seed="fixed-seed",
                    minimum_reviews=3,
                )


if __name__ == "__main__":
    unittest.main()
