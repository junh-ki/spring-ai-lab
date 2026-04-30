"""Multi-turn evaluation of GET /ai/generate to verify chat memory.

Each test seeds a conversation by issuing N-1 priming turns, then scores only the
final turn. The custom GEval metric explicitly asks the judge whether the final
answer surfaces the fact that was established in the priming turns — this is the
behavior chat memory is supposed to enable.

A unique chatId per test isolates conversations across runs.
"""
from __future__ import annotations

import uuid

import pytest
from deepeval import assert_test
from deepeval.metrics import GEval
from deepeval.test_case import LLMTestCase, LLMTestCaseParams

from datasets.memory_goldens import MEMORY_GOLDENS, MemoryGolden


@pytest.mark.parametrize("golden", MEMORY_GOLDENS, ids=lambda g: g.name)
def test_chat_memory_recall(golden: MemoryGolden, spring_client, judge) -> None:
    chat_id = f"deepeval-mem-{uuid.uuid4().hex[:8]}"

    for priming in golden.turns[:-1]:
        spring_client.generate(priming, chat_id=chat_id)

    final_question = golden.turns[-1]
    actual_output = spring_client.generate(final_question, chat_id=chat_id)

    history_str = "\n".join(f"- {turn}" for turn in golden.turns[:-1])
    criteria = (
        f"The user previously said:\n{history_str}\n\n"
        f"Pass only if the actual output correctly recalls or uses {golden.recall_topic}. "
        f"A response that ignores prior turns, asks the user to repeat the information, "
        f"or contradicts what the user said earlier should fail."
    )

    test_case = LLMTestCase(
        input=final_question,
        actual_output=actual_output,
        expected_output=golden.expected_recall,
    )

    metric = GEval(
        name="MemoryRecall",
        criteria=criteria,
        evaluation_params=[LLMTestCaseParams.INPUT, LLMTestCaseParams.ACTUAL_OUTPUT, LLMTestCaseParams.EXPECTED_OUTPUT],
        threshold=0.5,
        model=judge,
        async_mode=False,
    )
    assert_test(test_case, [metric])
