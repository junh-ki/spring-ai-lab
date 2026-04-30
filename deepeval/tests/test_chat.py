"""Single-turn evaluation of GET /ai/generate.

Metrics:
  - AnswerRelevancyMetric: does the response actually address the question?
  - GEval correctness: does the response semantically match the gold reference?
"""
from __future__ import annotations

import uuid

import pytest
from deepeval import assert_test
from deepeval.metrics import AnswerRelevancyMetric, GEval
from deepeval.test_case import LLMTestCase, LLMTestCaseParams

from datasets.chat_goldens import CHAT_GOLDENS, ChatGolden


@pytest.mark.parametrize("golden", CHAT_GOLDENS, ids=lambda g: g.name)
def test_chat_quality(golden: ChatGolden, spring_client, judge) -> None:
    chat_id = f"deepeval-chat-{uuid.uuid4().hex[:8]}"
    actual_output = spring_client.generate(golden.message, chat_id=chat_id)

    test_case = LLMTestCase(
        input=golden.message,
        actual_output=actual_output,
        expected_output=golden.expected_output,
    )

    metrics = [
        AnswerRelevancyMetric(threshold=0.5, model=judge, async_mode=False),
        GEval(
            name="Correctness",
            criteria=(
                "Determine whether the actual output expresses the same factual answer as the "
                "expected output. Phrasing differences are acceptable; factual disagreement is not."
            ),
            evaluation_params=[LLMTestCaseParams.INPUT, LLMTestCaseParams.ACTUAL_OUTPUT, LLMTestCaseParams.EXPECTED_OUTPUT],
            threshold=0.5,
            model=judge,
            async_mode=False,
        ),
    ]
    assert_test(test_case, metrics)
