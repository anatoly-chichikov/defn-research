"""Tests for ResearchResponse domain object."""
from __future__ import annotations

import uuid

from hamcrest import assert_that
from hamcrest import equal_to
from hamcrest import has_length
from hamcrest import is_

from src.api.response import ResearchResponse


class TestResearchResponseReturnsIdentifier:
    """ResearchResponse returns the run identifier."""

    def test(self) -> None:
        identifier = f"trun_{uuid.uuid4()}"
        response = ResearchResponse(
            identifier=identifier,
            status="completed",
            output="",
            basis={},
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
            basis={},
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
            basis={},
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
            basis={},
        )
        assert_that(
            response.markdown(),
            equal_to(output),
            "Response markdown did not match output",
        )


class TestResearchResponseExtractsSources:
    """ResearchResponse extracts sources from basis citations."""

    def test(self) -> None:
        basis = {
            "field": {
                "citations": [
                    {"url": f"https://example.com/{uuid.uuid4()}", "excerpt": "text"}
                ]
            }
        }
        response = ResearchResponse(
            identifier=f"trun_{uuid.uuid4()}",
            status="completed",
            output="",
            basis=basis,
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
        basis = {
            "field1": {"citations": [{"url": url, "excerpt": "a"}]},
            "field2": {"citations": [{"url": url, "excerpt": "b"}]},
        }
        response = ResearchResponse(
            identifier=f"trun_{uuid.uuid4()}",
            status="completed",
            output="",
            basis=basis,
        )
        assert_that(
            response.sources(),
            has_length(1),
            "Response did not deduplicate sources",
        )


class TestResearchResponseParsesApiData:
    """ResearchResponse parses from API data dictionary."""

    def test(self) -> None:
        identifier = f"trun_{uuid.uuid4()}"
        data = {
            "run_id": identifier,
            "status": "completed",
            "output": "markdown",
            "basis": {},
        }
        response = ResearchResponse.parse(data)
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
            basis={},
        )
        assert_that(
            response.sources(),
            has_length(0),
            "Response sources was not empty for empty basis",
        )
