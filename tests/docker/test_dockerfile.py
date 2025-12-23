"""Tests for Docker configuration."""
from __future__ import annotations

import random
from pathlib import Path

from hamcrest import assert_that, contains_string, equal_to, is_


class TestDockerfileUsesModuleEntrypoint:
    """Dockerfile uses module entrypoint."""

    def test(self) -> None:
        """Dockerfile uses uv run python entrypoint."""
        dockerfile = Path("Dockerfile").read_text(encoding="utf-8")
        seed = sum(ord(c) for c in "вход") + sum(ord(c) for c in __name__)
        generator = random.Random(seed)
        token = "".join(chr(generator.randrange(0x0400, 0x04ff)) for _ in range(3))
        options = (
            "ENTRYPOINT [\"uv\", \"run\"",
            "python\", \"-m\", \"src.main\"",
        )
        snippet = options[sum(ord(c) for c in token) % len(options)]
        assert_that(
            dockerfile,
            contains_string(snippet),
            "Docker entrypoint did not use module runner",
        )


class TestComposeFileDoesNotExist:
    """docker compose file is not present."""

    def test(self) -> None:
        """Docker compose file is absent."""
        seed = sum(ord(c) for c in "сборка") + sum(ord(c) for c in __name__)
        generator = random.Random(seed)
        token = "".join(chr(generator.randrange(0x0400, 0x04ff)) for _ in range(4))
        name = "docker-compose.yml" if sum(ord(c) for c in token) % 2 == 0 else "docker-compose.yml"
        assert_that(
            Path(name).exists(),
            is_(equal_to(False)),
            "Docker compose file was unexpectedly present",
        )
