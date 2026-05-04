"""Multi-turn evaluation of GET /ai/generate to verify chat memory.

Each test seeds a conversation by issuing N-1 priming turns, then checks the
final turn for a deterministic substring (e.g. the user's name "Junhyung", the
favorite color "teal"). A unique chatId per test isolates conversations.

This is intentionally judge-free: at POC scale the local `llama3.2:1b` judge
mis-scores exact factual matches when phrasing differs, so a substring check
on the expected recall keyword is both more reliable and faster.
"""
from __future__ import annotations

import uuid

import pytest

from datasets.memory_goldens import MEMORY_GOLDENS, MemoryGolden


@pytest.mark.parametrize("golden", MEMORY_GOLDENS, ids=lambda g: g.name)
def test_chat_memory_recall(golden: MemoryGolden, spring_client) -> None:
    chat_id = f"deepeval-mem-{uuid.uuid4().hex[:8]}"

    for priming in golden.turns[:-1]:
        spring_client.generate(priming, chat_id=chat_id)

    final_question = golden.turns[-1]
    actual_output = spring_client.generate(final_question, chat_id=chat_id)

    assert golden.expected_recall.lower() in actual_output.lower(), (
        f"Memory recall failed for {golden.name!r}: expected the response to "
        f"surface {golden.expected_recall!r} from prior turns, got: {actual_output!r}"
    )
