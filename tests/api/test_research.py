"""Tests for DeepResearch executor."""
from __future__ import annotations

import uuid
from typing import Final

from hamcrest import assert_that
from hamcrest import equal_to
from hamcrest import is_

from src.api.client import Connectable
from src.api.research import DeepResearch


class FakeClient(Connectable):
    """Fake HTTP client for testing."""

    def __init__(self, identifier: str, output: str) -> None:
        """Initialize with predetermined response data."""
        self._identifier: Final[str] = identifier
        self._output: Final[str] = output
        self._posted: dict = {}

    def post(self, path: str, body: dict) -> dict:
        """Record POST and return fake run_id."""
        self._posted = body
        return {"run_id": self._identifier}

    def get(self, path: str) -> dict:
        """Return fake completed result."""
        return {
            "status": "completed",
            "output": self._output,
            "basis": {},
        }

    def recorded(self) -> dict:
        """Return recorded POST body."""
        return self._posted


class TestDeepResearchReturnsCompletedResponse:
    """DeepResearch returns completed response after execution."""

    def test(self) -> None:
        identifier = f"trun_{uuid.uuid4()}"
        client = FakeClient(identifier=identifier, output="result")
        executor = DeepResearch(client)
        response = executor.execute("query", "pro")
        assert_that(
            response.completed(),
            is_(True),
            "Response was not marked as completed",
        )


class TestDeepResearchPassesQueryToApi:
    """DeepResearch passes query in POST body."""

    def test(self) -> None:
        query = f"исследование-{uuid.uuid4()}"
        client = FakeClient(identifier="trun_x", output="")
        executor = DeepResearch(client)
        executor.execute(query, "pro")
        assert_that(
            client.recorded()["input"],
            equal_to(query),
            "Query was not passed in POST body",
        )


class TestDeepResearchPassesProcessorToApi:
    """DeepResearch passes processor in POST body."""

    def test(self) -> None:
        processor = f"ultra-{uuid.uuid4()}"
        client = FakeClient(identifier="trun_x", output="")
        executor = DeepResearch(client)
        executor.execute("query", processor)
        assert_that(
            client.recorded()["processor"],
            equal_to(processor),
            "Processor was not passed in POST body",
        )


class TestDeepResearchReturnsMarkdownFromResponse:
    """DeepResearch returns markdown from API response."""

    def test(self) -> None:
        output = f"# Результат {uuid.uuid4()}"
        client = FakeClient(identifier="trun_x", output=output)
        executor = DeepResearch(client)
        response = executor.execute("query", "pro")
        assert_that(
            response.markdown(),
            equal_to(output),
            "Markdown did not match API output",
        )
