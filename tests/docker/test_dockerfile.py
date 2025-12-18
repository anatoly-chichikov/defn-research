"""Tests for Docker configuration."""
from __future__ import annotations

from pathlib import Path

from hamcrest import assert_that, contains_string, equal_to, is_


class TestDockerfileUsesEntrypointScript:
    """Dockerfile uses entrypoint script."""

    def test(self) -> None:
        dockerfile = Path("Dockerfile").read_text(encoding="utf-8")
        assert_that(
            dockerfile,
            contains_string("ENTRYPOINT [\"./entrypoint.sh\"]"),
            "Docker entrypoint did not use entrypoint script",
        )


class TestComposeFileDoesNotExist:
    """docker compose file is not present."""

    def test(self) -> None:
        assert_that(
            Path("docker-compose.yml").exists(),
            is_(equal_to(False)),
            "Docker compose file was unexpectedly present",
        )


class TestEntrypointIgnoresLanguageDirective:
    """Entrypoint ignores language directive in request files."""

    def test(self) -> None:
        script = Path("entrypoint.sh").read_text(encoding="utf-8")
        assert_that(
            script,
            contains_string("(Язык ответа:|Language:)"),
            "Entrypoint did not handle language directive lines",
        )
