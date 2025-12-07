"""Tests for PendingRun domain object."""
from __future__ import annotations

import uuid

from hamcrest import assert_that
from hamcrest import equal_to
from hamcrest import has_key

from src.domain.pending import PendingRun


class TestPendingRunReturnsIdentifier:
    """PendingRun returns the run identifier."""

    def test(self) -> None:
        identifier = f"trun_{uuid.uuid4()}"
        pending = PendingRun(
            identifier=identifier,
            query="test query",
            processor="pro",
            language="english",
        )
        assert_that(
            pending.identifier(),
            equal_to(identifier),
            "identifier() did not return provided value",
        )


class TestPendingRunReturnsQuery:
    """PendingRun returns the query."""

    def test(self) -> None:
        query = f"query-{uuid.uuid4()}"
        pending = PendingRun(
            identifier="trun_x",
            query=query,
            processor="pro",
            language="english",
        )
        assert_that(
            pending.query(),
            equal_to(query),
            "query() did not return provided value",
        )


class TestPendingRunReturnsProcessor:
    """PendingRun returns the processor."""

    def test(self) -> None:
        processor = f"ultra-{uuid.uuid4()}"
        pending = PendingRun(
            identifier="trun_x",
            query="test",
            processor=processor,
            language="english",
        )
        assert_that(
            pending.processor(),
            equal_to(processor),
            "processor() did not return provided value",
        )


class TestPendingRunReturnsLanguage:
    """PendingRun returns the language."""

    def test(self) -> None:
        language = f"lang-{uuid.uuid4()}"
        pending = PendingRun(
            identifier="trun_x",
            query="test",
            processor="pro",
            language=language,
        )
        assert_that(
            pending.language(),
            equal_to(language),
            "language() did not return provided value",
        )


class TestPendingRunSerializesCorrectly:
    """PendingRun serializes all fields."""

    def test(self) -> None:
        pending = PendingRun(
            identifier="trun_123",
            query="test query",
            processor="ultra",
            language="русский",
        )
        data = pending.serialize()
        assert_that(data, has_key("run_id"), "serialize() missing run_id")
        assert_that(data, has_key("query"), "serialize() missing query")
        assert_that(data, has_key("processor"), "serialize() missing processor")
        assert_that(data, has_key("language"), "serialize() missing language")


class TestPendingRunDeserializesCorrectly:
    """PendingRun deserializes from dictionary."""

    def test(self) -> None:
        identifier = f"trun_{uuid.uuid4()}"
        data = {
            "run_id": identifier,
            "query": "test query",
            "processor": "pro",
            "language": "english",
        }
        pending = PendingRun.deserialize(data)
        assert_that(
            pending.identifier(),
            equal_to(identifier),
            "deserialize() did not restore identifier",
        )
