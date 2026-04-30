"""Multi-turn goldens for GET /ai/generate that verify chat memory.

Each scenario is a sequence of turns sharing the same chatId. Only the final turn
is scored — earlier turns exist purely to seed conversation state. The
`expected_recall` string captures the fact the model must surface from prior
context. We use a unique chatId per test run so independent runs don't poison
each other through Redis-backed memory.
"""
from dataclasses import dataclass, field


@dataclass(frozen=True)
class MemoryGolden:
    name: str
    turns: list[str]
    expected_recall: str
    recall_topic: str = field(default="")


MEMORY_GOLDENS: list[MemoryGolden] = [
    MemoryGolden(
        name="remember_user_name",
        turns=[
            "Hi, my name is Junhyung. Please remember this.",
            "I work as a software engineer.",
            "What is my name?",
        ],
        expected_recall="Junhyung",
        recall_topic="the user's name (Junhyung) shared in the first turn",
    ),
    MemoryGolden(
        name="remember_favorite_color",
        turns=[
            "My favorite color is teal.",
            "I also enjoy mountain hiking on weekends.",
            "What color did I tell you was my favorite?",
        ],
        expected_recall="teal",
        recall_topic="the user's favorite color (teal) shared in the first turn",
    ),
    MemoryGolden(
        name="remember_project_name",
        turns=[
            "I'm building a project called AtlasNotes.",
            "It is a note-taking app for researchers.",
            "Remind me of the name of my project.",
        ],
        expected_recall="AtlasNotes",
        recall_topic="the user's project name (AtlasNotes)",
    ),
    MemoryGolden(
        name="remember_dietary_constraint",
        turns=[
            "I'm vegetarian and I'm allergic to peanuts.",
            "Can you suggest one simple lunch idea that respects both constraints?",
        ],
        expected_recall="vegetarian",
        recall_topic="the user's vegetarian + peanut-allergy constraints",
    ),
    MemoryGolden(
        name="remember_destination",
        turns=[
            "I'm planning a trip to Lisbon next month.",
            "I'll be there for five days.",
            "Where did I say I am traveling to?",
        ],
        expected_recall="Lisbon",
        recall_topic="the user's travel destination (Lisbon)",
    ),
]
