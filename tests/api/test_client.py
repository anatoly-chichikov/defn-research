"""Tests for ParallelClient SDK wrapper."""
from __future__ import annotations

import os
from unittest.mock import MagicMock
from unittest.mock import patch

from hamcrest import assert_that
from hamcrest import instance_of
from hamcrest import is_
from parallel import Parallel
from parallel import ParallelError

from src.api.client import ParallelClient


class TestParallelClientReturnsSdkInstance:
    """ParallelClient.sdk returns Parallel SDK instance."""

    def test(self) -> None:
        mock = MagicMock(spec=Parallel)
        client = ParallelClient(mock)
        assert_that(
            client.sdk(),
            is_(mock),
            "sdk() did not return provided instance",
        )


class TestParallelClientCreateUsesEnvironment:
    """ParallelClient.create uses PARALLEL_API_KEY from environment."""

    def test(self) -> None:
        with patch.dict(os.environ, {"PARALLEL_API_KEY": "test-key"}):
            client = ParallelClient.create()
            assert_that(
                client.sdk(),
                instance_of(Parallel),
                "create() did not return client with SDK instance",
            )


class TestParallelClientCreateRaisesWithoutKey:
    """ParallelClient.create raises when API key missing."""

    def test(self) -> None:
        original = os.environ.pop("PARALLEL_API_KEY", None)
        try:
            raised = False
            try:
                ParallelClient.create()
            except ParallelError:
                raised = True
            assert_that(raised, is_(True), "create() did not raise ParallelError")
        finally:
            if original:
                os.environ["PARALLEL_API_KEY"] = original
