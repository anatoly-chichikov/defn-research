"""Tests for ResearchSession domain object."""
from __future__ import annotations

import uuid

from hamcrest import assert_that
from hamcrest import equal_to
from hamcrest import has_length

from src.domain.pending import PendingRun
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


class TestResearchSessionPendingReturnsNoneByDefault:
    """ResearchSession.pending returns None when no pending run."""

    def test(self) -> None:
        session = ResearchSession(topic="t", tasks=tuple())
        assert_that(
            session.pending(),
            equal_to(None),
            "pending() was not None for new session",
        )


class TestResearchSessionStartSetsPendingRun:
    """ResearchSession.start sets pending run."""

    def test(self) -> None:
        session = ResearchSession(topic="t", tasks=tuple())
        pending = PendingRun("trun_x", "query", "pro", "english", "parallel")
        updated = session.start(pending)
        assert_that(
            updated.pending().identifier(),
            equal_to("trun_x"),
            "start() did not set pending run",
        )


class TestResearchSessionClearRemovesPending:
    """ResearchSession.clear removes pending run."""

    def test(self) -> None:
        pending = PendingRun("trun_x", "query", "pro", "english", "parallel")
        session = ResearchSession(topic="t", tasks=tuple(), pending=pending)
        cleared = session.clear()
        assert_that(
            cleared.pending(),
            equal_to(None),
            "clear() did not remove pending run",
        )


class TestResearchSessionSerializesPending:
    """ResearchSession serializes pending run."""

    def test(self) -> None:
        pending = PendingRun("trun_123", "q", "pro", "en", "parallel")
        session = ResearchSession(topic="t", tasks=tuple(), pending=pending)
        data = session.serialize()
        assert_that(
            data["pending"]["run_id"],
            equal_to("trun_123"),
            "serialize() did not include pending run_id",
        )


class TestResearchSessionDeserializesPending:
    """ResearchSession deserializes pending run."""

    def test(self) -> None:
        data = {
            "id": str(uuid.uuid4()),
            "topic": "test",
            "tasks": [],
            "created": "2025-12-06T10:00:00",
            "pending": {
                "run_id": "trun_abc",
                "query": "test query",
                "processor": "ultra",
                "language": "русский",
            },
        }
        session = ResearchSession.deserialize(data)
        assert_that(
            session.pending().identifier(),
            equal_to("trun_abc"),
            "deserialize() did not restore pending run",
        )
