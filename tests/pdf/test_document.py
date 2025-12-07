"""Tests for ResearchDocument."""
from __future__ import annotations

import tempfile
import uuid
from pathlib import Path

import pytest
from hamcrest import assert_that
from hamcrest import contains_string
from hamcrest import is_

from src.domain.result import TaskResult
from src.domain.session import ResearchSession
from src.domain.task import ResearchTask

try:
    from src.pdf.document import ResearchDocument
    WEASYPRINT_AVAILABLE = True
except OSError:
    WEASYPRINT_AVAILABLE = False
    ResearchDocument = None

from src.pdf.palette import HokusaiPalette

pytestmark = pytest.mark.skipif(
    not WEASYPRINT_AVAILABLE,
    reason="WeasyPrint system dependencies not installed"
)


class TestResearchDocumentRenderContainsHtmlTag:
    """ResearchDocument render returns valid HTML."""

    def test(self) -> None:
        session = ResearchSession(topic="Test", tasks=tuple())
        document = ResearchDocument(session, HokusaiPalette())
        assert_that(
            document.render(),
            contains_string("<!DOCTYPE html>"),
            "Rendered document did not contain DOCTYPE",
        )


class TestResearchDocumentRenderContainsTopic:
    """ResearchDocument render includes session topic."""

    def test(self) -> None:
        topic = f"トピック-{uuid.uuid4()}"
        session = ResearchSession(topic=topic, tasks=tuple())
        document = ResearchDocument(session, HokusaiPalette())
        assert_that(
            document.render(),
            contains_string(topic),
            "Rendered document did not contain topic",
        )


class TestResearchDocumentRenderContainsTaskQuery:
    """ResearchDocument render includes task queries."""

    def test(self) -> None:
        query = f"クエリ-{uuid.uuid4()}"
        task = ResearchTask(query=query, status="completed", result=None)
        session = ResearchSession(topic="T", tasks=(task,), )
        document = ResearchDocument(session, HokusaiPalette())
        assert_that(
            document.render(),
            contains_string(query),
            "Rendered document did not contain task query",
        )


class TestResearchDocumentRenderContainsSynthesis:
    """ResearchDocument render includes result synthesis."""

    def test(self) -> None:
        summary = f"サマリー-{uuid.uuid4()}"
        result = TaskResult(summary=summary, sources=tuple())
        task = ResearchTask(query="q", status="completed", result=result)
        session = ResearchSession(topic="T", tasks=(task,), )
        document = ResearchDocument(session, HokusaiPalette())
        assert_that(
            document.render(),
            contains_string(summary),
            "Rendered document did not contain synthesis",
        )


class TestResearchDocumentRenderEscapesHtml:
    """ResearchDocument render escapes HTML characters."""

    def test(self) -> None:
        topic = "<script>alert('xss')</script>"
        session = ResearchSession(topic=topic, tasks=tuple())
        document = ResearchDocument(session, HokusaiPalette())
        assert_that(
            document.render(),
            contains_string("&lt;script&gt;"),
            "Rendered document did not escape HTML",
        )


class TestResearchDocumentHtmlCreatesFile:
    """ResearchDocument html method creates HTML file."""

    def test(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / f"test-{uuid.uuid4()}.html"
            session = ResearchSession(topic="T", tasks=tuple())
            document = ResearchDocument(session, HokusaiPalette())
            document.html(path)
            assert_that(
                path.exists(),
                is_(True),
                "HTML file was not created",
            )


class TestResearchDocumentNormalizeAddsBlankLineBeforeList:
    """ResearchDocument normalize adds blank line before list marker."""

    def test(self) -> None:
        session = ResearchSession(topic="T", tasks=tuple())
        document = ResearchDocument(session, HokusaiPalette())
        text = f"**見出し-{uuid.uuid4()}**\n* アイテム"
        assert_that(
            document._normalize(text),
            contains_string("**\n\n* "),
            "Normalize did not add blank line before list",
        )


class TestResearchDocumentNormalizePreservesExistingBlankLines:
    """ResearchDocument normalize preserves already correct formatting."""

    def test(self) -> None:
        session = ResearchSession(topic="T", tasks=tuple())
        document = ResearchDocument(session, HokusaiPalette())
        marker = uuid.uuid4()
        text = f"**ヘッダー-{marker}**\n\n* リスト項目"
        assert_that(
            document._normalize(text),
            is_(text),
            "Normalize modified already correct text",
        )


class TestResearchDocumentNormalizeHandlesMultipleLists:
    """ResearchDocument normalize fixes multiple lists in same text."""

    def test(self) -> None:
        session = ResearchSession(topic="T", tasks=tuple())
        document = ResearchDocument(session, HokusaiPalette())
        text = f"**第一-{uuid.uuid4()}**\n* 一\n**第二**\n* 二"
        result = document._normalize(text)
        assert_that(
            result.count("\n\n* "),
            is_(2),
            "Normalize did not fix all lists",
        )


class TestResearchDocumentNormalizeIgnoresListsAfterBlankLine:
    """ResearchDocument normalize does not double blank lines."""

    def test(self) -> None:
        session = ResearchSession(topic="T", tasks=tuple())
        document = ResearchDocument(session, HokusaiPalette())
        text = f"段落-{uuid.uuid4()}\n\n* すでに正しい"
        assert_that(
            document._normalize(text).count("\n\n\n"),
            is_(0),
            "Normalize added extra blank lines",
        )


class TestResearchDocumentCitationsConvertsReferencesToLinks:
    """ResearchDocument citations converts [N] to clickable links."""

    def test(self) -> None:
        session = ResearchSession(topic="T", tasks=tuple())
        document = ResearchDocument(session, HokusaiPalette())
        marker = uuid.uuid4()
        text = f"テキスト-{marker} [1]\n\n## References\n\n1. タイトル https://example.com/{marker}"
        result, urls = document._citations(text)
        assert_that(
            result,
            contains_string(f'<a href="https://example.com/{marker}" class="cite"'),
            "Citations did not create link from reference",
        )


class TestResearchDocumentCitationsExtractsUrls:
    """ResearchDocument citations returns extracted URLs."""

    def test(self) -> None:
        session = ResearchSession(topic="T", tasks=tuple())
        document = ResearchDocument(session, HokusaiPalette())
        marker = uuid.uuid4()
        text = f"参照 [1]\n\n## References\n\n1. ソース https://test.jp/{marker}"
        result, urls = document._citations(text)
        assert_that(
            len(urls),
            is_(1),
            "Citations did not extract URL",
        )


class TestResearchDocumentReferencesExtractsMapping:
    """ResearchDocument references extracts number to URL mapping."""

    def test(self) -> None:
        session = ResearchSession(topic="T", tasks=tuple())
        document = ResearchDocument(session, HokusaiPalette())
        marker = uuid.uuid4()
        text = f"## References\n\n1. 一 https://a.jp/{marker}\n2. 二 https://b.jp/{marker}"
        refs = document._references(text)
        assert_that(
            len(refs),
            is_(2),
            "References did not extract all entries",
        )


class TestResearchDocumentPathReturnsSessionBasedPath:
    """ResearchDocument path returns path based on session ID."""

    def test(self) -> None:
        identifier = str(uuid.uuid4())
        session = ResearchSession(topic="T", tasks=tuple(), identifier=identifier)
        document = ResearchDocument(session, HokusaiPalette())
        assert_that(
            str(document._path()),
            contains_string(identifier),
            "Path did not contain session ID",
        )


class TestResearchDocumentBriefReadsFromFileWhenExists:
    """ResearchDocument brief reads content from file when it exists."""

    def test(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            identifier = str(uuid.uuid4())
            marker = f"マーカー-{uuid.uuid4()}"
            briefs = Path(tmp) / "data" / "briefs"
            briefs.mkdir(parents=True)
            (briefs / f"{identifier}.md").write_text(marker, encoding="utf-8")
            task = ResearchTask(query="fallback", status="completed", result=None)
            session = ResearchSession(
                topic="T", tasks=(task,), identifier=identifier
            )
            document = ResearchDocument(session, HokusaiPalette())
            original = document._path
            document._path = lambda: briefs / f"{identifier}.md"
            assert_that(
                document._brief(),
                contains_string(marker),
                "Brief did not read from file",
            )


class TestResearchDocumentBriefFallsBackToQueryWhenNoFile:
    """ResearchDocument brief uses task query when file does not exist."""

    def test(self) -> None:
        marker = f"クエリ-{uuid.uuid4()}"
        task = ResearchTask(query=marker, status="completed", result=None)
        session = ResearchSession(topic="T", tasks=(task,))
        document = ResearchDocument(session, HokusaiPalette())
        assert_that(
            document._brief(),
            contains_string(marker),
            "Brief did not fall back to query",
        )
