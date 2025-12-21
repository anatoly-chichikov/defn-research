"""Tests for TaskResult and Source domain objects."""
from __future__ import annotations

import uuid

from hamcrest import assert_that
from hamcrest import equal_to
from hamcrest import has_length
from hamcrest import is_
from hamcrest import is_not
from hamcrest import starts_with

from src.domain.result import Source
from src.domain.result import TaskResult


class TestSourceReturnsProvidedTitle:
    """Source returns the title provided during construction."""

    def test(self) -> None:
        title = f"title-{uuid.uuid4()}"
        source = Source(title=title, url="https://example.com", excerpt="text")
        assert_that(
            source.title(),
            equal_to(title),
            "Source title did not match provided value",
        )


class TestSourceReturnsProvidedUrl:
    """Source returns the URL provided during construction."""

    def test(self) -> None:
        url = f"https://example.com/{uuid.uuid4()}"
        source = Source(title="Title", url=url, excerpt="text")
        assert_that(
            source.url(),
            equal_to(url),
            "Source URL did not match provided value",
        )


class TestSourceSerializesAllFields:
    """Source serializes all fields to dictionary."""

    def test(self) -> None:
        excerpt = f"excerpt-{uuid.uuid4()}"
        source = Source(title="T", url="https://x.com", excerpt=excerpt)
        assert_that(
            source.serialize()["excerpt"],
            equal_to(excerpt),
            "Serialized excerpt did not match original",
        )


class TestSourceDeserializesFromDictionary:
    """Source deserializes correctly from dictionary."""

    def test(self) -> None:
        title = f"restored-{uuid.uuid4()}"
        data = {"title": title, "url": "https://x.com", "excerpt": "e"}
        source = Source.deserialize(data)
        assert_that(
            source.title(),
            equal_to(title),
            "Deserialized source title did not match",
        )


class TestTaskResultReturnsSummary:
    """TaskResult returns provided summary."""

    def test(self) -> None:
        summary = f"summary-{uuid.uuid4()}"
        result = TaskResult(summary=summary, sources=tuple())
        assert_that(
            result.summary(),
            equal_to(summary),
            "TaskResult summary did not match provided value",
        )


class TestTaskResultReturnsSources:
    """TaskResult returns tuple of sources."""

    def test(self) -> None:
        source = Source(title="T", url="https://x.com", excerpt="e")
        result = TaskResult(summary="s", sources=(source,))
        assert_that(
            result.sources(),
            has_length(1),
            "TaskResult sources count was not one",
        )


class TestTaskResultSerializesCorrectly:
    """TaskResult serializes summary and sources."""

    def test(self) -> None:
        summary = f"serialized-{uuid.uuid4()}"
        result = TaskResult(summary=summary, sources=tuple())
        assert_that(
            result.serialize()["summary"],
            equal_to(summary),
            "Serialized summary did not match original",
        )


class TestTaskResultDeserializesCorrectly:
    """TaskResult deserializes from dictionary."""

    def test(self) -> None:
        summary = f"deserialized-{uuid.uuid4()}"
        data = {"summary": summary, "sources": []}
        result = TaskResult.deserialize(data)
        assert_that(
            result.summary(),
            equal_to(summary),
            "Deserialized summary did not match",
        )


class TestSourceReturnsProvidedConfidence:
    """Source returns the confidence level provided during construction."""

    def test(self) -> None:
        confidence = "High"
        source = Source(
            title="T",
            url="https://example.com",
            excerpt="e",
            confidence=confidence,
        )
        assert_that(
            source.confidence(),
            equal_to(confidence),
            "Source confidence did not match provided value",
        )


class TestSourceReturnsNoneWhenConfidenceNotProvided:
    """Source returns None when confidence is not provided."""

    def test(self) -> None:
        source = Source(title="T", url="https://example.com", excerpt="e")
        assert_that(
            source.confidence(),
            equal_to(None),
            "Source confidence was not None when not provided",
        )


class TestSourceSerializesConfidenceWhenProvided:
    """Source includes confidence in serialization when provided."""

    def test(self) -> None:
        confidence = "Medium"
        source = Source(
            title="T",
            url="https://x.com",
            excerpt="e",
            confidence=confidence,
        )
        assert_that(
            source.serialize()["confidence"],
            equal_to(confidence),
            "Serialized confidence did not match provided value",
        )


class TestSourceOmitsConfidenceWhenNotProvided:
    """Source omits confidence from serialization when not provided."""

    def test(self) -> None:
        source = Source(title="T", url="https://x.com", excerpt="e")
        assert_that(
            "confidence" in source.serialize(),
            is_(False),
            "Serialized source contained confidence when not provided",
        )


class TestSourceDeserializesConfidence:
    """Source deserializes confidence from dictionary."""

    def test(self) -> None:
        confidence = "Low"
        data = {
            "title": "T",
            "url": "https://x.com",
            "excerpt": "e",
            "confidence": confidence,
        }
        source = Source.deserialize(data)
        assert_that(
            source.confidence(),
            equal_to(confidence),
            "Deserialized confidence did not match",
        )


class TestSourceDeserializesWithoutConfidence:
    """Source deserializes correctly when confidence is absent."""

    def test(self) -> None:
        data = {"title": "T", "url": "https://x.com", "excerpt": "e"}
        source = Source.deserialize(data)
        assert_that(
            source.confidence(),
            equal_to(None),
            "Deserialized confidence was not None when absent",
        )
