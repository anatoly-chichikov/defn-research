"""Tests for ResearchSession domain object."""
from __future__ import annotations

import uuid

from hamcrest import assert_that
from hamcrest import equal_to
from hamcrest import has_length

from src.domain.session import ResearchSession
from src.domain.task import ResearchTask


class TestResearchSessionGeneratesUniqueId:
    """ResearchSession generates unique identifier on creation."""

    def test(self) -> None:
        topic = f"topic-{uuid.uuid4()}"
        session = ResearchSession(topic=topic, tasks=tuple())
        assert_that(
            session.id(),
            has_length(36),
            "Session identifier was not a valid UUID length",
        )


class TestResearchSessionReturnsProvidedTopic:
    """ResearchSession returns topic provided during construction."""

    def test(self) -> None:
        topic = f"トピック-{uuid.uuid4()}"
        session = ResearchSession(topic=topic, tasks=tuple())
        assert_that(
            session.topic(),
            equal_to(topic),
            "Session topic did not match provided value",
        )


class TestResearchSessionExtendAddsTask:
    """ResearchSession extend returns session with added task."""

    def test(self) -> None:
        session = ResearchSession(topic="t", tasks=tuple())
        task = ResearchTask(query="q", status="pending", result=None)
        extended = session.extend(task)
        assert_that(
            extended.tasks(),
            has_length(1),
            "Extended session did not contain one task",
        )


class TestResearchSessionExtendPreservesId:
    """ResearchSession extend preserves original identifier."""

    def test(self) -> None:
        session = ResearchSession(topic="t", tasks=tuple())
        original = session.id()
        task = ResearchTask(query="q", status="pending", result=None)
        extended = session.extend(task)
        assert_that(
            extended.id(),
            equal_to(original),
            "Extended session ID did not match original",
        )


class TestResearchSessionSerializesTopic:
    """ResearchSession serializes topic to dictionary."""

    def test(self) -> None:
        topic = f"シリアル化-{uuid.uuid4()}"
        session = ResearchSession(topic=topic, tasks=tuple())
        assert_that(
            session.serialize()["topic"],
            equal_to(topic),
            "Serialized topic did not match original",
        )


class TestResearchSessionDeserializesCorrectly:
    """ResearchSession deserializes from dictionary."""

    def test(self) -> None:
        topic = f"デシリアル化-{uuid.uuid4()}"
        data = {
            "id": str(uuid.uuid4()),
            "topic": topic,
            "tasks": [],
            "created": "2025-12-06T10:00:00",
        }
        session = ResearchSession.deserialize(data)
        assert_that(
            session.topic(),
            equal_to(topic),
            "Deserialized topic did not match",
        )
