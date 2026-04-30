"""Single-turn goldens for GET /ai/generate.

Each golden contains the user message and an `expected_output` reference that the
GEval correctness judge can compare against. Keep questions small and factual so
weak local judges produce consistent scores.
"""
from dataclasses import dataclass


@dataclass(frozen=True)
class ChatGolden:
    name: str
    message: str
    expected_output: str


CHAT_GOLDENS: list[ChatGolden] = [
    ChatGolden(
        name="capital_france",
        message="What is the capital of France? Answer in one short sentence.",
        expected_output="The capital of France is Paris.",
    ),
    ChatGolden(
        name="basic_math",
        message="What is 15 plus 27? Reply with just the number.",
        expected_output="42",
    ),
    ChatGolden(
        name="programming_language",
        message="Which programming language is the Spring Framework primarily written in?",
        expected_output="Java.",
    ),
    ChatGolden(
        name="boiling_point",
        message="At standard atmospheric pressure, at what temperature in Celsius does water boil?",
        expected_output="100 degrees Celsius.",
    ),
    ChatGolden(
        name="opposite_word",
        message="What is the opposite of 'hot'?",
        expected_output="Cold.",
    ),
]
