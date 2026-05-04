"""Single-turn evaluation of GET /ai/generate.

Two complementary checks per case:
  - AnswerRelevancyMetric (LLM-judged): does the response actually address the
    question? This is the kind of check DeepEval is good at and the local
    `llama3.2:1b` judge can handle.
  - Deterministic substring assertion: does the response contain the unambiguous
    keyword from the golden (e.g. "Paris")? Replaces a previous GEval correctness
    metric — at POC scale the 1B judge mis-scores exact-match answers as wrong
    when phrasing differs from the reference.
"""
from __future__ import annotations

import uuid

import pytest
from deepeval import assert_test
from deepeval.metrics import AnswerRelevancyMetric
from deepeval.test_case import LLMTestCase

from datasets.chat_goldens import CHAT_GOLDENS, ChatGolden


@pytest.mark.parametrize("golden", CHAT_GOLDENS, ids=lambda g: g.name)
def test_chat_quality(golden: ChatGolden, spring_client, judge) -> None:
    chat_id = f"deepeval-chat-{uuid.uuid4().hex[:8]}"
    actual_output = spring_client.generate(golden.message, chat_id=chat_id)

    assert golden.expected_substring.lower() in actual_output.lower(), (
        f"Expected substring {golden.expected_substring!r} not found in response: "
        f"{actual_output!r}"
    )

    test_case = LLMTestCase(
        input=golden.message,
        actual_output=actual_output,
        expected_output=golden.expected_output,
    )
    assert_test(
        test_case,
        [AnswerRelevancyMetric(threshold=0.5, model=judge, async_mode=False)],
    )
