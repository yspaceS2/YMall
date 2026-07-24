import json
import sys
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch


AI_DIR = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(AI_DIR))

from common.review_summary import (  # noqa: E402
    PROMPT_VERSION,
    build_reference_output,
    build_review_prompt,
)
from training.prepare_sft_data import (  # noqa: E402
    TrainingDataError,
    assert_split_isolation,
    file_sha256,
    load_training_config,
)
from training.evaluate_lora import (  # noqa: E402
    aggregate_results,
    build_sample_result,
    evaluate_candidates,
)
from training.train_lora import (  # noqa: E402
    ReviewSummaryDataset,
    TrainingError,
    validate_generated_data,
)


def sample(product_group_id: str) -> dict:
    return {
        "sampleId": f"sample-{product_group_id}",
        "productGroupId": product_group_id,
        "reviews": [
            {"rating": 5, "content": "소리가 조용하고 편안합니다."},
            {"rating": 4, "content": "연결이 빠르고 사용하기 좋습니다."},
        ],
        "referenceSummary": {
            "pros": ["조용하고 편안함"],
            "cons": [],
            "commonOpinions": ["사용하기 편리함"],
        },
    }


class ReviewSummaryPromptTest(unittest.TestCase):

    def test_prompt_separates_sentiment_and_requires_repeated_common_opinion(self):
        prompt = build_review_prompt(sample("keyboard"))

        self.assertIn("장점에는 긍정 의견만", prompt)
        self.assertIn("둘 이상의 리뷰가 직접 뒷받침", prompt)
        self.assertIn("없으면 빈 배열", prompt)

    def test_reference_output_contains_only_service_summary_fields(self):
        output = json.loads(build_reference_output(sample("keyboard")))

        self.assertEqual(
            {"pros", "cons", "commonOpinions"},
            set(output),
        )


class PrepareSftDataTest(unittest.TestCase):

    def test_rejects_product_group_leakage_between_splits(self):
        splits = {
            "train": [sample("same-product")],
            "validation": [sample("same-product")],
            "test": [sample("other-product")],
        }

        with self.assertRaisesRegex(TrainingDataError, "상품 그룹이 겹칩니다"):
            assert_split_isolation(splits)

    def test_rejects_prompt_version_mismatch(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "config.json"
            path.write_text(
                json.dumps({
                    "schemaVersion": "1.0",
                    "runId": "test",
                    "baseModel": {
                        "id": "model",
                        "revision": "revision",
                        "license": "license",
                    },
                    "dataset": {
                        "train": "train.jsonl",
                        "validation": "validation.jsonl",
                        "test": "test.jsonl",
                    },
                    "promptVersion": f"{PROMPT_VERSION}-other",
                    "seed": 1,
                }),
                encoding="utf-8",
            )

            with self.assertRaisesRegex(TrainingDataError, "프롬프트 버전"):
                load_training_config(path)

    def test_lora_evaluation_builds_schema_and_aggregate_metrics(self):
        evaluation_sample = {
            "sampleId": "sample-1",
            "referenceSummary": {
                "pros": ["밝기 조절이 편리함"],
                "cons": [],
                "commonOpinions": [],
            },
        }
        result = build_sample_result(
            evaluation_sample,
            '{"pros":["밝기 조절이 편리함"],"cons":[],"commonOpinions":[]}',
            latency_seconds=0.5,
            input_tokens=10,
            output_tokens=8,
        )

        aggregate = aggregate_results([result])

        self.assertTrue(result["schemaValid"])
        self.assertEqual(aggregate["sampleCount"], 1)
        self.assertEqual(aggregate["strictJsonObjectRate"], 1.0)
        self.assertEqual(aggregate["rougeLAverage"], 1.0)

    def test_rejects_generated_data_changed_after_manifest_creation(self):
        with tempfile.TemporaryDirectory() as directory:
            data_dir = Path(directory)
            generated = {}
            for split in ("train", "validation", "test"):
                split_path = data_dir / f"{split}.jsonl"
                split_path.write_text(f'{{"split":"{split}"}}\n', encoding="utf-8")
                generated[split] = {"sha256": file_sha256(split_path)}
            (data_dir / "train.jsonl").write_text(
                '{"split":"changed"}\n',
                encoding="utf-8",
            )

            with self.assertRaisesRegex(TrainingError, "train 데이터 해시"):
                validate_generated_data(data_dir, {"generated": generated})

    @patch("training.evaluate_lora.generate_summary")
    def test_warms_up_each_candidate_before_measuring(self, generate_summary_mock):
        generate_summary_mock.return_value = (
            '{"pros":[],"cons":[],"commonOpinions":[]}',
            0.5,
            10,
            8,
        )
        evaluation_sample = {
            "sampleId": "sample-1",
            "referenceSummary": {
                "pros": [],
                "cons": [],
                "commonOpinions": [],
            },
        }

        evaluate_candidates(
            model=object(),
            tokenizer=object(),
            samples=[evaluation_sample],
            torch_module=object(),
        )

        calls = generate_summary_mock.call_args_list
        self.assertEqual([False, True], [calls[0].args[4], calls[1].args[4]])
        self.assertEqual(8, calls[0].kwargs["max_new_tokens"])
        self.assertEqual(8, calls[1].kwargs["max_new_tokens"])
        self.assertEqual([False, True], [calls[2].args[4], calls[3].args[4]])


class FakeTokenizer:
    eos_token_id = 99

    def apply_chat_template(self, messages, **kwargs):
        self.prompt_messages = messages
        self.template_kwargs = kwargs
        return "formatted prompt"

    def __call__(self, value, **kwargs):
        if value == "formatted prompt":
            return {"input_ids": [10, 11, 12]}
        return {"input_ids": [20, 21]}


class FakeTorch:
    long = "long"

    @staticmethod
    def tensor(value, dtype):
        return list(value)

    @staticmethod
    def ones(length, dtype):
        return [1] * length


class ReviewSummaryDatasetTest(unittest.TestCase):

    def test_tokenizes_formatted_prompt_and_masks_prompt_labels(self):
        tokenizer = FakeTokenizer()
        records = [{
            "sampleId": "sample-1",
            "messages": [
                {"role": "system", "content": "system"},
                {"role": "user", "content": "reviews"},
                {"role": "assistant", "content": "summary"},
            ],
        }]

        dataset = ReviewSummaryDataset(records, tokenizer, 16, FakeTorch)

        self.assertEqual([10, 11, 12, 20, 21, 99], dataset[0]["input_ids"])
        self.assertEqual([-100, -100, -100, 20, 21, 99], dataset[0]["labels"])
        self.assertFalse(tokenizer.template_kwargs["tokenize"])
        self.assertFalse(tokenizer.template_kwargs["enable_thinking"])


if __name__ == "__main__":
    unittest.main()
