"""Single-turn goldens for GET /ai/generate.

Each golden carries:
  - `message`: the user prompt
  - `expected_output`: a reference answer (used by `AnswerRelevancyMetric` context)
  - `expected_substring`: the unambiguous keyword that MUST appear in the actual
    output. Used for a deterministic, judge-free correctness check — see
    `tests/test_chat.py` for the rationale (a 1B local judge is too noisy to
    score phrasing differences reliably).
"""
from dataclasses import dataclass


@dataclass(frozen=True)
class ChatGolden:
    name: str
    message: str
    expected_output: str
    expected_substring: str


CHAT_GOLDENS: list[ChatGolden] = [
    ChatGolden(
        name="capital_france",
        message="What is the capital of France? Answer in one short sentence.",
        expected_output="The capital of France is Paris.",
        expected_substring="Paris",
    ),
    ChatGolden(
        name="basic_math",
        message="What is 15 plus 27? Reply with just the number.",
        expected_output="42",
        expected_substring="42",
    ),
    ChatGolden(
        name="programming_language",
        message="Which programming language is the Spring Framework primarily written in?",
        expected_output="Java.",
        expected_substring="Java",
    ),
    ChatGolden(
        name="boiling_point",
        message="At standard atmospheric pressure, at what temperature in Celsius does water boil?",
        expected_output="100 degrees Celsius.",
        expected_substring="100",
    ),
    ChatGolden(
        name="opposite_word",
        message="What is the opposite of 'hot'?",
        expected_output="Cold.",
        expected_substring="cold",
    ),
]
