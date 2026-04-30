"""RAG evaluation of GET /support, grounded in the sample PDF.

Metrics:
  - FaithfulnessMetric: does the answer stay consistent with the supplied
    retrieval_context (i.e., no hallucinations)?
  - AnswerRelevancyMetric: does the answer actually address the question?
  - GEval correctness: does the answer semantically match the gold reference?

Note: /support does not expose what was retrieved from the vector store, so the
`retrieval_context` field is populated with the *expected* excerpts (the
ground-truth answer span). For a POC this is acceptable; for a production eval
you would expose the retrieved chunks via a debug endpoint.
"""
from __future__ import annotations

import pytest
from deepeval import assert_test
from deepeval.metrics import AnswerRelevancyMetric, FaithfulnessMetric, GEval
from deepeval.test_case import LLMTestCase, LLMTestCaseParams

from datasets.rag_goldens import RAG_GOLDENS, RagGolden


@pytest.mark.parametrize("golden", RAG_GOLDENS, ids=lambda g: g.name)
def test_support_rag(golden: RagGolden, spring_client, judge, rag_corpus_ready) -> None:
    actual_output = spring_client.support(golden.question)

    test_case = LLMTestCase(
        input=golden.question,
        actual_output=actual_output,
        expected_output=golden.expected_output,
        retrieval_context=list(golden.retrieval_context),
    )

    metrics = [
        FaithfulnessMetric(threshold=0.5, model=judge, async_mode=False),
        AnswerRelevancyMetric(threshold=0.5, model=judge, async_mode=False),
        GEval(
            name="Correctness",
            criteria=(
                "Determine whether the actual output conveys the same factual answer as the "
                "expected output. Allow paraphrasing and added detail, but penalise factual "
                "disagreement, hedging that avoids the answer, or refusal to answer when the "
                "expected output gives a clear answer."
            ),
            evaluation_params=[LLMTestCaseParams.INPUT, LLMTestCaseParams.ACTUAL_OUTPUT, LLMTestCaseParams.EXPECTED_OUTPUT],
            threshold=0.5,
            model=judge,
            async_mode=False,
        ),
    ]
    assert_test(test_case, metrics)
