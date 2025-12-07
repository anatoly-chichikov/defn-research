"""Tests for ParallelClient HTTP client."""
from __future__ import annotations

import uuid

from hamcrest import assert_that
from hamcrest import calling
from hamcrest import equal_to
from hamcrest import raises

from src.api.client import ParallelClient


class TestParallelClientBuildsPostUrl:
    """ParallelClient builds correct POST URL."""

    def test(self) -> None:
        token = f"token-{uuid.uuid4()}"
        client = ParallelClient(
            token=token,
            base="https://test.api",
            timeout=10,
        )
        assert_that(
            client._headers()["x-api-key"],
            equal_to(token),
            "Client headers did not contain correct token",
        )


class TestParallelClientRequiresToken:
    """ParallelClient.create raises when token missing."""

    def test(self) -> None:
        import os
        original = os.environ.get("PARALLEL_API_KEY")
        os.environ.pop("PARALLEL_API_KEY", None)
        try:
            assert_that(
                calling(ParallelClient.create),
                raises(ValueError),
                "Client did not raise for missing token",
            )
        finally:
            if original:
                os.environ["PARALLEL_API_KEY"] = original
