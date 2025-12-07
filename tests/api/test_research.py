"""Tests for DeepResearch executor."""
from __future__ import annotations

import uuid
from typing import Final
from unittest.mock import MagicMock

from hamcrest import assert_that
from hamcrest import equal_to
from hamcrest import is_
from parallel.types import TaskRunTextOutput

from src.api.client import Connectable
from src.api.research import DeepResearch


class FakeSDK:
    """Fake Parallel SDK for testing."""

    def __init__(self, identifier: str, output: str) -> None:
        """Initialize with predetermined response."""
        self._identifier: Final[str] = identifier
        self._output: Final[str] = output
        self._recorded: dict = {}
        self.beta = MagicMock()
        self.task_run = MagicMock()
        self._configure()

    def _configure(self) -> None:
        """Configure mock responses."""
        task = MagicMock()
        task.run_id = self._identifier
        self.beta.task_run.create.return_value = task
        self.beta.task_run.events.return_value = iter([])
        result = MagicMock()
        result.run.run_id = self._identifier
        result.run.status = "completed"
        output = MagicMock(spec=TaskRunTextOutput)
        output.content = self._output
        output.basis = []
        output.type = "text"
        result.output = output
        self.task_run.result.return_value = result

    def recorded(self) -> dict:
        """Return recorded create() kwargs."""
        return self.beta.task_run.create.call_args.kwargs


class FakeClient(Connectable):
    """Fake client wrapping FakeSDK."""

    def __init__(self, identifier: str, output: str) -> None:
        """Initialize with predetermined response."""
        self._sdk: Final[FakeSDK] = FakeSDK(identifier, output)

    def sdk(self) -> FakeSDK:
        """Return fake SDK instance."""
        return self._sdk

    def recorded(self) -> dict:
        """Return recorded create() kwargs."""
        return self._sdk.recorded()


class TestDeepResearchStartReturnsRunId:
    """DeepResearch.start returns run_id from API."""

    def test(self) -> None:
        identifier = f"trun_{uuid.uuid4()}"
        client = FakeClient(identifier=identifier, output="result")
        executor = DeepResearch(client)
        result = executor.start("query", "pro")
        assert_that(
            result,
            equal_to(identifier),
            "start() did not return expected run_id",
        )


class TestDeepResearchStartPassesQuery:
    """DeepResearch.start passes query to API."""

    def test(self) -> None:
        query = f"исследование-{uuid.uuid4()}"
        client = FakeClient(identifier="trun_x", output="")
        executor = DeepResearch(client)
        executor.start(query, "pro")
        assert_that(
            client.recorded()["input"],
            equal_to(query),
            "Query was not passed to create()",
        )


class TestDeepResearchStartPassesProcessor:
    """DeepResearch.start passes processor to API."""

    def test(self) -> None:
        processor = f"ultra-{uuid.uuid4()}"
        client = FakeClient(identifier="trun_x", output="")
        executor = DeepResearch(client)
        executor.start("query", processor)
        assert_that(
            client.recorded()["processor"],
            equal_to(processor),
            "Processor was not passed to create()",
        )


class TestDeepResearchFinishReturnsCompletedResponse:
    """DeepResearch.finish returns completed response."""

    def test(self) -> None:
        identifier = f"trun_{uuid.uuid4()}"
        client = FakeClient(identifier=identifier, output="result")
        executor = DeepResearch(client)
        response = executor.finish(identifier)
        assert_that(
            response.completed(),
            is_(True),
            "Response was not marked as completed",
        )


class TestDeepResearchFinishReturnsMarkdown:
    """DeepResearch.finish returns markdown from response."""

    def test(self) -> None:
        output = f"# Результат {uuid.uuid4()}"
        client = FakeClient(identifier="trun_x", output=output)
        executor = DeepResearch(client)
        response = executor.finish("trun_x")
        assert_that(
            response.markdown(),
            equal_to(output),
            "Markdown did not match API output",
        )


class TestDeepResearchStreamHandlesEmptyEvents:
    """DeepResearch.stream handles empty event stream."""

    def test(self) -> None:
        client = FakeClient(identifier="trun_x", output="")
        executor = DeepResearch(client)
        executor.stream("trun_x")
        assert_that(True, is_(True), "Stream completed without error")
