"""Tests for CLI application logic."""
from __future__ import annotations

import os
import random
import tempfile
from contextlib import redirect_stdout
from datetime import datetime
from io import StringIO
from pathlib import Path
from typing import Final

from hamcrest import assert_that
from hamcrest import equal_to
from hamcrest import is_

from src.api.response import ResearchResponse
from src.domain.pending import PendingRun
from src.domain.session import ResearchSession
from src.main import Application
from src.storage.file import JsonFile
from src.storage.organizer import OutputOrganizer
from src.storage.repository import SessionsRepository


class FakeApp(Application):
    """Fake application capturing run calls."""

    def __init__(self, token: str) -> None:
        """Initialize with token."""
        self._token: Final[str] = token
        self._topic: str | None = None
        self._value: tuple | None = None

    def create(self, topic: str) -> str:
        """Store topic and return token."""
        self._topic = topic
        return self._token

    def research(self, token: str, query: str, processor: str, language: str, provider: str) -> None:
        """Store research arguments."""
        self._value = (self._topic, token, query, processor, language, provider)

    def value(self) -> tuple | None:
        """Return stored value."""
        return self._value


class FakeExecutor:
    """Fake executor returning predetermined response."""

    def __init__(self, identifier: str, response: ResearchResponse) -> None:
        """Initialize with identifier and response."""
        self._identifier: Final[str] = identifier
        self._response: Final[ResearchResponse] = response

    def start(self, query: str, processor: str) -> str:
        """Return stored identifier."""
        return self._identifier

    def stream(self, identifier: str) -> None:
        """Ignore streaming request."""
        return None

    def finish(self, identifier: str) -> ResearchResponse:
        """Return stored response."""
        return self._response


class ProbeApp(Application):
    """Application with stubbed executor and PDF handling."""

    def __init__(self, root: Path, executor: FakeExecutor) -> None:
        """Initialize with root and executor."""
        self._root: Final[Path] = root
        self._data: Final[Path] = root / "data" / "research.json"
        self._output: Final[Path] = root / "output"
        self._engine: Final[FakeExecutor] = executor

    def _executor(self, provider: str) -> FakeExecutor:
        """Return stub executor."""
        return self._engine

    def _pdf(
        self,
        session: ResearchSession,
        cover: Path,
        name: str,
        provider: str,
        organizer: OutputOrganizer,
    ) -> Path:
        """Return report path without rendering."""
        path = organizer.report(name, provider)
        return path


class TestApplicationRun:
    """Application run combines create and research."""

    def test_application_run_forwards_parameters(self) -> None:
        """Application run forwards parameters to research."""
        seed = sum(ord(c) for c in __name__) + 41
        generator = random.Random(seed)
        topic = "".join(chr(generator.randrange(0x0400, 0x04ff)) for _ in range(6))
        query = "".join(chr(generator.randrange(0x0370, 0x03ff)) for _ in range(7))
        token = "".join(chr(generator.randrange(0x0530, 0x058f)) for _ in range(5))
        processor = "".join(chr(generator.randrange(0x0400, 0x04ff)) for _ in range(4))
        language = "".join(chr(generator.randrange(0x0370, 0x03ff)) for _ in range(4))
        provider = "".join(chr(generator.randrange(0x3040, 0x309f)) for _ in range(4))
        app = FakeApp(token)
        app._run(topic, query, processor, language, provider)
        data = (topic, token, query, processor, language, provider)
        assert_that(app.value(), equal_to(data), "run did not pass data")


class TestApplicationSkipsCoverWhenKeyMissing:
    """Application skips cover generation when key missing."""

    def test_application_skips_cover_when_key_missing(self) -> None:
        """Application skips cover generation when key missing."""
        seed = sum(ord(c) for c in __name__) + 73
        generator = random.Random(seed)
        if "GEMINI_API_KEY" in os.environ:
            del os.environ["GEMINI_API_KEY"]
        topic = "".join(chr(generator.randrange(0x0400, 0x04ff)) for _ in range(6))
        query = "".join(chr(generator.randrange(0x0370, 0x03ff)) for _ in range(7))
        processor = "".join(chr(generator.randrange(0x0530, 0x058f)) for _ in range(5))
        language = "".join(chr(generator.randrange(0x3040, 0x309f)) for _ in range(4))
        provider = "parallel"
        run = "".join(chr(generator.randrange(0x0600, 0x06ff)) for _ in range(8))
        text = "".join(chr(generator.randrange(0x0400, 0x04ff)) for _ in range(12))
        year = 2000 + generator.randrange(0, 20)
        month = 1 + generator.randrange(0, 11)
        day = 1 + generator.randrange(0, 27)
        hour = generator.randrange(0, 23)
        minute = generator.randrange(0, 59)
        second = generator.randrange(0, 59)
        stamp = datetime(year, month, day, hour, minute, second)
        identifier = "".join(chr(generator.randrange(0x0400, 0x04ff)) for _ in range(16))
        entry = PendingRun(run, query, processor, language, provider)
        session = ResearchSession(topic=topic, tasks=tuple(), identifier=identifier, created=stamp, pending=entry)
        root = Path(tempfile.mkdtemp())
        data = root / "data"
        data.mkdir(parents=True, exist_ok=True)
        folder = root / "output"
        folder.mkdir(parents=True, exist_ok=True)
        repository = SessionsRepository(JsonFile(data / "research.json"))
        repository.save((session,))
        response = ResearchResponse(identifier=run, status="completed", output=text, basis=[], cost=0.0)
        executor = FakeExecutor(run, response)
        app = ProbeApp(root, executor)
        token = identifier[:8]
        stream = StringIO()
        with redirect_stdout(stream):
            app.research(token, query, processor, language, provider)
        organizer = OutputOrganizer(folder)
        name = organizer.name(stamp, topic, identifier)
        cover = organizer.cover(name, provider)
        assert_that(cover.exists(), is_(False), "Cover image was generated despite missing key")
