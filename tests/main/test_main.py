"""Tests for CLI application logic."""
from __future__ import annotations

import random
from typing import Final

from hamcrest import assert_that
from hamcrest import equal_to

from src.main import Application


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
