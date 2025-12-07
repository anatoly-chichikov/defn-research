"""Tests for SessionsRepository."""
from __future__ import annotations

import tempfile
import uuid
from pathlib import Path

from hamcrest import assert_that
from hamcrest import equal_to
from hamcrest import has_length
from hamcrest import none

from src.domain.session import ResearchSession
from src.domain.task import ResearchTask
from src.storage.file import JsonFile
from src.storage.repository import SessionsRepository


class TestSessionsRepositoryReturnsEmptyTupleForMissingFile:
    """SessionsRepository returns empty tuple when file missing."""

    def test(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / f"missing-{uuid.uuid4()}.json"
            repository = SessionsRepository(JsonFile(path))
            assert_that(
                repository.load(),
                has_length(0),
                "Load did not return empty tuple for missing file",
            )


class TestSessionsRepositorySavesAndLoadsSession:
    """SessionsRepository saves session that can be loaded."""

    def test(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / f"repo-{uuid.uuid4()}.json"
            repository = SessionsRepository(JsonFile(path))
            topic = f"topic-{uuid.uuid4()}"
            session = ResearchSession(topic=topic, tasks=tuple())
            repository.save((session,))
            loaded = repository.load()
            assert_that(
                loaded[0].topic(),
                equal_to(topic),
                "Loaded session topic did not match saved",
            )


class TestSessionsRepositoryAppendAddsSession:
    """SessionsRepository append adds session to existing."""

    def test(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / f"append-{uuid.uuid4()}.json"
            repository = SessionsRepository(JsonFile(path))
            first = ResearchSession(topic="first", tasks=tuple())
            second = ResearchSession(topic="second", tasks=tuple())
            repository.append(first)
            repository.append(second)
            assert_that(
                repository.load(),
                has_length(2),
                "Repository did not contain two sessions after append",
            )


class TestSessionsRepositoryFindReturnsMatchingSession:
    """SessionsRepository find returns session with matching ID."""

    def test(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / f"find-{uuid.uuid4()}.json"
            repository = SessionsRepository(JsonFile(path))
            session = ResearchSession(topic="findme", tasks=tuple())
            repository.append(session)
            found = repository.find(session.id())
            assert_that(
                found.topic(),
                equal_to("findme"),
                "Found session topic did not match",
            )


class TestSessionsRepositoryFindReturnsNoneForMissing:
    """SessionsRepository find returns None for missing ID."""

    def test(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / f"notfound-{uuid.uuid4()}.json"
            repository = SessionsRepository(JsonFile(path))
            found = repository.find(str(uuid.uuid4()))
            assert_that(
                found,
                none(),
                "Find returned non-None for missing ID",
            )


class TestSessionsRepositoryUpdateModifiesSession:
    """SessionsRepository update modifies existing session."""

    def test(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / f"update-{uuid.uuid4()}.json"
            repository = SessionsRepository(JsonFile(path))
            session = ResearchSession(topic="original", tasks=tuple())
            repository.append(session)
            task = ResearchTask(query="q", status="pending", result=None)
            updated = session.extend(task)
            repository.update(updated)
            loaded = repository.find(session.id())
            assert_that(
                loaded.tasks(),
                has_length(1),
                "Updated session did not contain task",
            )
