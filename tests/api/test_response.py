"""Tests for ResearchResponse domain object."""
from __future__ import annotations

import random
import uuid
from unittest.mock import MagicMock

from hamcrest import assert_that
from hamcrest import contains_string
from hamcrest import equal_to
from hamcrest import has_length
from hamcrest import is_
from hamcrest import not_
from parallel.types import TaskRunTextOutput

from src.api.response import ResearchResponse


class TestResearchResponseReturnsIdentifier:
    """ResearchResponse returns the run identifier."""

    def test(self) -> None:
        identifier = f"trun_{uuid.uuid4()}"
        response = ResearchResponse(
            identifier=identifier,
            status="completed",
            output="",
            basis=[],
        )
        assert_that(
            response.identifier(),
            equal_to(identifier),
            "Response identifier did not match provided value",
        )


class TestResearchResponseDetectsCompleted:
    """ResearchResponse detects completed status."""

    def test(self) -> None:
        response = ResearchResponse(
            identifier=f"trun_{uuid.uuid4()}",
            status="completed",
            output="",
            basis=[],
        )
        assert_that(
            response.completed(),
            is_(True),
            "Response was not detected as completed",
        )


class TestResearchResponseDetectsFailed:
    """ResearchResponse detects failed status."""

    def test(self) -> None:
        response = ResearchResponse(
            identifier=f"trun_{uuid.uuid4()}",
            status="failed",
            output="",
            basis=[],
        )
        assert_that(
            response.failed(),
            is_(True),
            "Response was not detected as failed",
        )


class TestResearchResponseReturnsMarkdown:
    """ResearchResponse returns output as markdown."""

    def test(self) -> None:
        output = f"# Исследование {uuid.uuid4()}\n\nТекст с кириллицей"
        response = ResearchResponse(
            identifier=f"trun_{uuid.uuid4()}",
            status="completed",
            output=output,
            basis=[],
        )
        assert_that(
            response.markdown(),
            equal_to(output),
            "Response markdown did not match output",
        )


class TestResearchResponseExtractsSources:
    """ResearchResponse extracts sources from basis citations."""

    def test(self) -> None:
        url = f"https://example.com/{uuid.uuid4()}"
        citation = MagicMock()
        citation.url = url
        citation.title = "Test"
        citation.excerpts = ["text"]
        field = MagicMock()
        field.citations = [citation]
        response = ResearchResponse(
            identifier=f"trun_{uuid.uuid4()}",
            status="completed",
            output="",
            basis=[field],
        )
        assert_that(
            response.sources(),
            has_length(1),
            "Response did not extract one source",
        )


class TestResearchResponseDeduplicatesSources:
    """ResearchResponse deduplicates sources by URL."""

    def test(self) -> None:
        url = f"https://example.com/{uuid.uuid4()}"
        citation1 = MagicMock()
        citation1.url = url
        citation1.title = "A"
        citation1.excerpts = ["a"]
        citation2 = MagicMock()
        citation2.url = url
        citation2.title = "B"
        citation2.excerpts = ["b"]
        field1 = MagicMock()
        field1.citations = [citation1]
        field2 = MagicMock()
        field2.citations = [citation2]
        response = ResearchResponse(
            identifier=f"trun_{uuid.uuid4()}",
            status="completed",
            output="",
            basis=[field1, field2],
        )
        assert_that(
            response.sources(),
            has_length(1),
            "Response did not deduplicate sources",
        )


class TestResearchResponseParsesApiData:
    """ResearchResponse parses from TaskRunResult."""

    def test(self) -> None:
        identifier = f"trun_{uuid.uuid4()}"
        result = MagicMock()
        result.run.run_id = identifier
        result.run.status = "completed"
        output = MagicMock(spec=TaskRunTextOutput)
        output.content = "markdown"
        output.basis = []
        result.output = output
        response = ResearchResponse.parse(result)
        assert_that(
            response.identifier(),
            equal_to(identifier),
            "Parsed response identifier did not match",
        )


class TestResearchResponseHandlesEmptyBasis:
    """ResearchResponse handles empty basis gracefully."""

    def test(self) -> None:
        response = ResearchResponse(
            identifier=f"trun_{uuid.uuid4()}",
            status="completed",
            output="",
            basis=[],
        )
        assert_that(
            response.sources(),
            has_length(0),
            "Response sources was not empty for empty basis",
        )


class TestResearchResponseExtractsConfidence:
    """ResearchResponse extracts confidence from FieldBasis."""

    def test(self) -> None:
        confidence = "High"
        citation = MagicMock()
        citation.url = f"https://example.com/{uuid.uuid4()}"
        citation.title = "T"
        citation.excerpts = ["e"]
        field = MagicMock()
        field.citations = [citation]
        field.confidence = confidence
        response = ResearchResponse(
            identifier=f"trun_{uuid.uuid4()}",
            status="completed",
            output="",
            basis=[field],
        )
        assert_that(
            response.sources()[0].confidence(),
            equal_to(confidence),
            "Source confidence did not match FieldBasis confidence",
        )


class TestResearchResponseHandlesMissingConfidence:
    """ResearchResponse handles FieldBasis without confidence attribute."""

    def test(self) -> None:
        citation = MagicMock()
        citation.url = f"https://example.com/{uuid.uuid4()}"
        citation.title = "T"
        citation.excerpts = ["e"]
        field = MagicMock(spec=["citations"])
        field.citations = [citation]
        response = ResearchResponse(
            identifier=f"trun_{uuid.uuid4()}",
            status="completed",
            output="",
            basis=[field],
        )
        assert_that(
            response.sources()[0].confidence(),
            equal_to(None),
            "Source confidence was not None when FieldBasis lacks confidence",
        )


class Test_research_response_returns_cost:
    """ResearchResponse returns total cost."""

    def test(self) -> None:
        """ResearchResponse returns total cost."""
        seed = sum(ord(c) for c in __name__) + 17
        generator = random.Random(seed)
        value = round(generator.random() * 100, 2)
        output = f"Отчет-{generator.randrange(1000)}"
        response = ResearchResponse(
            identifier=f"trun_{generator.randrange(100000)}",
            status="completed",
            output=output,
            basis=[],
            cost=value,
        )
        assert_that(
            response.cost(),
            equal_to(value),
            "cost() did not return expected value",
        )


class Test_research_response_strips_utm_parameters_from_markdown:
    """ResearchResponse strips utm parameters from markdown."""

    def test(self) -> None:
        """ResearchResponse strips utm parameters from markdown."""
        seed = sum(ord(c) for c in __name__) + 19
        generator = random.Random(seed)
        slug = "".join(chr(generator.randrange(0x0400, 0x04ff)) for _ in range(6))
        link = f"https://example.com/{generator.randrange(1000)}?utm_source=valyu.ai&utm_medium=referral&x={generator.randrange(9)}"
        output = f"Источники {slug}\n1. {link}"
        response = ResearchResponse(
            identifier=f"trun_{generator.randrange(100000)}",
            status="completed",
            output=output,
            basis=[],
        )
        assert_that(
            response.markdown(),
            not_(contains_string("utm_source")),
            "utm parameters were not stripped from markdown",
        )


class Test_research_response_strips_utm_parameters_from_sources:
    """ResearchResponse strips utm parameters from sources."""

    def test(self) -> None:
        """ResearchResponse strips utm parameters from sources."""
        seed = sum(ord(c) for c in __name__) + 29
        generator = random.Random(seed)
        slug = "".join(chr(generator.randrange(0x3040, 0x309f)) for _ in range(5))
        link = f"https://example.com/{generator.randrange(1000)}?utm_source=valyu.ai&utm_medium=referral&x={generator.randrange(9)}"
        citation = MagicMock()
        citation.url = link
        citation.title = slug
        citation.excerpts = [slug]
        field = MagicMock()
        field.citations = [citation]
        response = ResearchResponse(
            identifier=f"trun_{generator.randrange(100000)}",
            status="completed",
            output=slug,
            basis=[field],
        )
        assert_that(
            response.sources()[0].url(),
            not_(contains_string("utm_source")),
            "utm parameters were not stripped from sources",
        )


class Test_research_response_preserves_signed_urls:
    """ResearchResponse preserves URLs without utm parameters."""

    def test(self) -> None:
        """ResearchResponse preserves URLs without utm parameters."""
        seed = sum(ord(c) for c in __name__) + 37
        generator = random.Random(seed)
        token = "".join(chr(generator.randrange(0x0400, 0x04ff)) for _ in range(6))
        key = "".join(chr(generator.randrange(0x0370, 0x03ff)) for _ in range(4))
        val = "".join(chr(generator.randrange(0x0530, 0x058f)) for _ in range(4))
        link = f"https://example.com/{generator.randrange(1000)}?{key}={val}&sig={generator.randrange(1000)}"
        output = f"{token} {link}"
        response = ResearchResponse(
            identifier=f"trun_{generator.randrange(100000)}",
            status="completed",
            output=output,
            basis=[],
        )
        assert_that(
            response.markdown(),
            equal_to(output),
            "URL was changed despite missing utm parameters",
        )
