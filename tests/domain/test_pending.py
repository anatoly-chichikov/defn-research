"""Tests for PendingRun domain object."""
from __future__ import annotations

import random
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
            provider="parallel",
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
            provider="parallel",
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
            provider="parallel",
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
            provider="parallel",
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
            provider="parallel",
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


class Test_pending_run_returns_provider:
    """PendingRun returns provider name."""

    def test(self) -> None:
        """PendingRun returns provider name."""
        seed = sum(ord(c) for c in __name__) + 11
        generator = random.Random(seed)
        name = "".join(chr(generator.randrange(0x0400, 0x04ff)) for _ in range(6))
        query = "".join(chr(generator.randrange(0x0370, 0x03ff)) for _ in range(5))
        processor = f"lite-{generator.randrange(1000)}"
        language = f"lang-{generator.randrange(1000)}"
        pending = PendingRun(
            identifier=f"trun_{generator.randrange(100000)}",
            query=query,
            processor=processor,
            language=language,
            provider=name,
        )
        assert_that(
            pending.provider(),
            equal_to(name),
            "provider() did not return provided value",
        )


class Test_pending_run_serializes_provider:
    """PendingRun serializes provider name."""

    def test(self) -> None:
        """PendingRun serializes provider name."""
        seed = sum(ord(c) for c in __name__) + 13
        generator = random.Random(seed)
        name = "".join(chr(generator.randrange(0x3040, 0x309f)) for _ in range(4))
        query = "".join(chr(generator.randrange(0x0370, 0x03ff)) for _ in range(6))
        processor = f"core-{generator.randrange(1000)}"
        language = f"lang-{generator.randrange(1000)}"
        pending = PendingRun(
            identifier=f"trun_{generator.randrange(100000)}",
            query=query,
            processor=processor,
            language=language,
            provider=name,
        )
        data = pending.serialize()
        assert_that(
            data["provider"],
            equal_to(name),
            "serialize() did not include provider",
        )
