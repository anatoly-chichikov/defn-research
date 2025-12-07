"""Tests for ResearchTask domain object."""
from __future__ import annotations

import uuid

from hamcrest import assert_that
from hamcrest import equal_to
from hamcrest import has_length
from hamcrest import is_not
from hamcrest import none
from hamcrest import not_none
from hamcrest import starts_with

from src.domain.result import TaskResult
from src.domain.task import ResearchTask


class TestResearchTaskGeneratesUniqueId:
    """ResearchTask generates unique identifier on creation."""

    def test(self) -> None:
        query = f"query-{uuid.uuid4()}"
        task = ResearchTask(query=query, status="pending", result=None)
        assert_that(
            task.id(),
            has_length(36),
            "Task identifier was not a valid UUID length",
        )


class TestResearchTaskReturnsProvidedQuery:
    """ResearchTask returns query provided during construction."""

    def test(self) -> None:
        query = f"研究-{uuid.uuid4()}"
        task = ResearchTask(query=query, status="pending", result=None)
        assert_that(
            task.query(),
            equal_to(query),
            "Task query did not match provided value",
        )


class TestResearchTaskReturnsProvidedStatus:
    """ResearchTask returns status provided during construction."""

    def test(self) -> None:
        status = f"status-{uuid.uuid4()}"
        task = ResearchTask(query="q", status=status, result=None)
        assert_that(
            task.status(),
            equal_to(status),
            "Task status did not match provided value",
        )


class TestResearchTaskCompleteReturnsNewTask:
    """ResearchTask complete method returns new task with result."""

    def test(self) -> None:
        task = ResearchTask(query="q", status="pending", result=None)
        result = TaskResult(summary="s", sources=tuple())
        completed = task.complete(result)
        assert_that(
            completed.status(),
            equal_to("completed"),
            "Completed task status was not completed",
        )


class TestResearchTaskCompletePreservesId:
    """ResearchTask complete preserves original identifier."""

    def test(self) -> None:
        task = ResearchTask(query="q", status="pending", result=None)
        original = task.id()
        result = TaskResult(summary="s", sources=tuple())
        completed = task.complete(result)
        assert_that(
            completed.id(),
            equal_to(original),
            "Completed task ID did not match original",
        )


class TestResearchTaskCompleteAddsTimestamp:
    """ResearchTask complete adds completion timestamp."""

    def test(self) -> None:
        task = ResearchTask(query="q", status="pending", result=None)
        result = TaskResult(summary="s", sources=tuple())
        completed = task.complete(result)
        assert_that(
            completed.completed(),
            not_none(),
            "Completed task timestamp was None",
        )


class TestResearchTaskSerializesQuery:
    """ResearchTask serializes query to dictionary."""

    def test(self) -> None:
        query = f"сериализация-{uuid.uuid4()}"
        task = ResearchTask(query=query, status="pending", result=None)
        assert_that(
            task.serialize()["query"],
            equal_to(query),
            "Serialized query did not match original",
        )


class TestResearchTaskDeserializesCorrectly:
    """ResearchTask deserializes from dictionary."""

    def test(self) -> None:
        query = f"десериализация-{uuid.uuid4()}"
        data = {
            "id": str(uuid.uuid4()),
            "query": query,
            "status": "pending",
            "created": "2025-12-06T10:00:00",
        }
        task = ResearchTask.deserialize(data)
        assert_that(
            task.query(),
            equal_to(query),
            "Deserialized query did not match",
        )
