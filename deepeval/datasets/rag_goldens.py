"""Goldens for GET /support, grounded in the sample PDF that ships with the repo
(`AI Platform Team - Spring AI Intro.pdf`).

Each golden carries:
  - `expected_output`: a reference answer for GEval correctness scoring.
  - `retrieval_context`: excerpts that *should* be retrieved from the vector store.
    The /support endpoint does not expose what was actually retrieved, so for the
    POC we evaluate Faithfulness and ContextualRelevancy against the ground-truth
    excerpts. This is a known POC simplification — see deepeval/README.md.
"""
from dataclasses import dataclass


@dataclass(frozen=True)
class RagGolden:
    name: str
    question: str
    expected_output: str
    retrieval_context: list[str]


RAG_GOLDENS: list[RagGolden] = [
    RagGolden(
        name="chat_memory_tech",
        question="According to the slides, which technology is used for ChatMemory?",
        expected_output="Redis is used for ChatMemory (session-based short-term memory with TTL).",
        retrieval_context=[
            "ChatMemory (Session-based Short-term Memory). Technology of choice: Redis. "
            "Session-based short-term memory context with TTL. Contributes to context window."
        ],
    ),
    RagGolden(
        name="vector_store_options",
        question="Which vector store technologies are mentioned as options?",
        expected_output="OpenSearch, Elasticsearch, and pgvector (a PostgreSQL extension).",
        retrieval_context=[
            "VectorStore stores embeddings (vectors) and supports semantic search "
            "(similaritySearch) with topK and similarityThreshold. Technology of choice: "
            "OpenSearch, Elasticsearch, pgvector (PostgreSQL Extension)."
        ],
    ),
    RagGolden(
        name="hybrid_search_definition",
        question="What is hybrid search and which algorithm is used to combine results?",
        expected_output=(
            "Hybrid search combines semantic and keyword search results by merging "
            "ranked lists, using Reciprocal Rank Fusion (RRF)."
        ),
        retrieval_context=[
            "Hybrid Search (RRF): Combines semantic and keyword search results by merging "
            "ranked lists to improve overall relevance and precision. (Reciprocal Rank Fusion)"
        ],
    ),
    RagGolden(
        name="idempotency_mechanism",
        question="How is idempotency achieved during RAG document ingestion?",
        expected_output=(
            "Idempotency is achieved via metadata so duplicate documents are not stored."
        ),
        retrieval_context=[
            "RAG Importation: Idempotency is important to avoid duplicating any documents. "
            "Metadata can achieve it. Target files: pdf or md that contain company or "
            "policy or core business knowledge."
        ],
    ),
    RagGolden(
        name="tool_annotation_purpose",
        question="Who is the description in the @Tool annotation written for?",
        expected_output=(
            "The @Tool description is meant for the LLM, not for humans. It lets the LLM "
            "decide when to invoke the tool."
        ),
        retrieval_context=[
            "@Tool(description = '...') is not meant for humans. It is meant for the LLM. "
            "With tools, an LLM turns from a simple chatbot into an agent with actual "
            "capabilities beyond chatting."
        ],
    ),
    RagGolden(
        name="topk_meaning",
        question="What does the topK parameter control in semantic search?",
        expected_output="topK controls how many hits (results) are returned by the similarity search.",
        retrieval_context=[
            "Semantic Search (a.k.a. similaritySearch): topK - how many hits (results) do "
            "I want? similarityThreshold - how good must a match be?"
        ],
    ),
]
