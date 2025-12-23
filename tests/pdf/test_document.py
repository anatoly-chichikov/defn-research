"""Tests for ResearchDocument."""
from __future__ import annotations

import os
import random
import tempfile
import uuid
from pathlib import Path

import pytest
from hamcrest import assert_that
from hamcrest import contains_string
from hamcrest import is_
from hamcrest import not_

from src.domain.result import Source
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


class Test_research_document_renders_author_name_from_environment:
    """ResearchDocument render includes author name from environment."""

    def test(self) -> None:
        seed = sum(ord(c) for c in __name__)
        generator = random.Random(seed)
        name = "".join(chr(generator.randrange(0x0400, 0x04ff)) for _ in range(6))
        service = "".join(chr(generator.randrange(0x3040, 0x309f)) for _ in range(4))
        value = "".join(chr(generator.randrange(0x0370, 0x03ff)) for _ in range(5))
        os.environ["REPORT_FOR"] = name
        result = TaskResult(summary=value, sources=tuple())
        task = ResearchTask(query=value, status="completed", result=result, language=value, service=service)
        session = ResearchSession(topic=value, tasks=(task,))
        path = Path(tempfile.gettempdir()) / f"{generator.randrange(1000000)}.png"
        document = ResearchDocument(session, HokusaiPalette(), path)
        html = document.render()
        assert_that(html, contains_string(name), "Author name was missing")


class Test_research_document_renders_service_name:
    """ResearchDocument render includes service name."""

    def test(self) -> None:
        seed = sum(ord(c) for c in __name__) + 1
        generator = random.Random(seed)
        name = "".join(chr(generator.randrange(0x0400, 0x04ff)) for _ in range(6))
        service = "parallel.ai"
        value = "".join(chr(generator.randrange(0x0370, 0x03ff)) for _ in range(5))
        os.environ["REPORT_FOR"] = name
        result = TaskResult(summary=value, sources=tuple())
        task = ResearchTask(query=value, status="completed", result=result, language=value, service=service)
        session = ResearchSession(topic=value, tasks=(task,))
        path = Path(tempfile.gettempdir()) / f"{generator.randrange(1000000)}.png"
        document = ResearchDocument(session, HokusaiPalette(), path)
        html = document.render()
        assert_that(html, contains_string(service), "Service name was missing")


class Test_research_document_renders_parallel_domain:
    """ResearchDocument render includes parallel domain."""

    def test(self) -> None:
        seed = sum(ord(c) for c in __name__) + 3
        generator = random.Random(seed)
        name = "".join(chr(generator.randrange(0x0400, 0x04ff)) for _ in range(6))
        value = "".join(chr(generator.randrange(0x0370, 0x03ff)) for _ in range(5))
        os.environ["REPORT_FOR"] = name
        result = TaskResult(summary=value, sources=tuple())
        task = ResearchTask(query=value, status="completed", result=result, language=value, service="parallel.ai")
        session = ResearchSession(topic=value, tasks=(task,))
        path = Path(tempfile.gettempdir()) / f"{generator.randrange(1000000)}.png"
        document = ResearchDocument(session, HokusaiPalette(), path)
        html = document.render()
        assert_that(html, contains_string("parallel.ai"), "Parallel domain was missing")


class Test_research_document_renders_valyu_domain:
    """ResearchDocument render includes valyu domain."""

    def test(self) -> None:
        seed = sum(ord(c) for c in __name__) + 4
        generator = random.Random(seed)
        name = "".join(chr(generator.randrange(0x0400, 0x04ff)) for _ in range(6))
        value = "".join(chr(generator.randrange(0x0370, 0x03ff)) for _ in range(5))
        os.environ["REPORT_FOR"] = name
        result = TaskResult(summary=value, sources=tuple())
        task = ResearchTask(query=value, status="completed", result=result, language=value, service="valyu.ai")
        session = ResearchSession(topic=value, tasks=(task,))
        path = Path(tempfile.gettempdir()) / f"{generator.randrange(1000000)}.png"
        document = ResearchDocument(session, HokusaiPalette(), path)
        html = document.render()
        assert_that(html, contains_string("valyu.ai"), "Valyu domain was missing")


class Test_research_document_omits_author_when_name_missing:
    """ResearchDocument render omits author span when author is missing."""

    def test(self) -> None:
        seed = sum(ord(c) for c in __name__) + 2
        generator = random.Random(seed)
        service = "".join(chr(generator.randrange(0x3040, 0x309f)) for _ in range(4))
        value = "".join(chr(generator.randrange(0x0370, 0x03ff)) for _ in range(5))
        os.environ["REPORT_FOR"] = ""
        result = TaskResult(summary=value, sources=tuple())
        task = ResearchTask(query=value, status="completed", result=result, language=value, service=service)
        session = ResearchSession(topic=value, tasks=(task,))
        path = Path(tempfile.gettempdir()) / f"{generator.randrange(1000000)}.png"
        document = ResearchDocument(session, HokusaiPalette(), path)
        html = document.render()
        assert_that(html, not_(contains_string('<span class="author">')), "Author span was present")


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


class Test_research_document_renders_confidence_badge_for_trimmed_urls:
    """ResearchDocument renders confidence badges for trimmed URLs."""

    def test(self) -> None:
        """ResearchDocument renders confidence badges for trimmed URLs."""
        seed = sum(ord(c) for c in __name__) + 35
        generator = random.Random(seed)
        text = "".join(chr(generator.randrange(0x0400, 0x04ff)) for _ in range(5))
        url = f"https://example.com/{generator.randrange(1000)}?utm_source=valyu.ai&utm_medium=referral"
        source = Source(title=text, url=url, excerpt=text, confidence="High")
        summary = f"{text}\n\n## References\n1. {url}"
        result = TaskResult(summary=summary, sources=(source,))
        task = ResearchTask(query=text, status="completed", result=result)
        session = ResearchSession(topic=text, tasks=(task,), )
        document = ResearchDocument(session, HokusaiPalette())
        html = document.render()
        assert_that(
            html,
            contains_string("confidence-high"),
            "Confidence badge was missing",
        )


class Test_research_document_strips_utm_parameters_from_urls:
    """ResearchDocument render strips utm parameters from URLs."""

    def test(self) -> None:
        """ResearchDocument render strips utm parameters from URLs."""
        seed = sum(ord(c) for c in __name__) + 31
        generator = random.Random(seed)
        slug = "".join(chr(generator.randrange(0x0400, 0x04ff)) for _ in range(5))
        link = f"https://example.com/{generator.randrange(1000)}?utm_source=valyu.ai&utm_medium=referral&x={generator.randrange(9)}"
        summary = f"Sources\n1. {link}\n2. {slug}"
        result = TaskResult(summary=summary, sources=tuple())
        task = ResearchTask(query=slug, status="completed", result=result)
        session = ResearchSession(topic=slug, tasks=(task,), )
        document = ResearchDocument(session, HokusaiPalette())
        html = document.render()
        assert_that(
            html,
            not_(contains_string("utm_source")),
            "utm parameters were not stripped from document",
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
    """ResearchDocument path returns path based on short session ID."""

    def test(self) -> None:
        identifier = str(uuid.uuid4())
        short = identifier.split("-")[0]
        session = ResearchSession(topic="T", tasks=tuple(), identifier=identifier)
        document = ResearchDocument(session, HokusaiPalette())
        assert_that(
            str(document._path()),
            contains_string(short),
            "Path did not contain short session ID",
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


class TestResearchDocumentNestedListsWithSingleSpaceIndent:
    """ResearchDocument nested converts single space indent to proper nesting."""

    def test(self) -> None:
        session = ResearchSession(topic="T", tasks=tuple())
        document = ResearchDocument(session, HokusaiPalette())
        marker = uuid.uuid4()
        text = f"* **親-{marker}:**\n * **子要素:** 内容"
        result = document._nested(text)
        assert_that(
            result,
            contains_string("    * "),
            "Nested did not convert single space to four spaces",
        )


class TestResearchDocumentNestedListsPreservesFourSpaceIndent:
    """ResearchDocument nested preserves already correct four space indent."""

    def test(self) -> None:
        session = ResearchSession(topic="T", tasks=tuple())
        document = ResearchDocument(session, HokusaiPalette())
        marker = uuid.uuid4()
        text = f"* **親-{marker}:**\n    * **子:** 内容"
        assert_that(
            document._nested(text),
            is_(text),
            "Nested modified already correct indentation",
        )


class TestResearchDocumentNestedListsHandlesMultipleLevels:
    """ResearchDocument nested handles two and three space indents."""

    def test(self) -> None:
        session = ResearchSession(topic="T", tasks=tuple())
        document = ResearchDocument(session, HokusaiPalette())
        marker = uuid.uuid4()
        text = f"* 一-{marker}\n  * 二\n   * 三"
        result = document._nested(text)
        assert_that(
            result.count("    * "),
            is_(2),
            "Nested did not normalize all indented items",
        )


class TestResearchDocumentNormalizeAddsBlankLineBeforeNumberedList:
    """ResearchDocument normalize adds blank line before numbered list."""

    def test(self) -> None:
        session = ResearchSession(topic="T", tasks=tuple())
        document = ResearchDocument(session, HokusaiPalette())
        marker = uuid.uuid4()
        text = f"調査-{marker}:\n1. 最初の項目"
        assert_that(
            document._normalize(text),
            contains_string(":\n\n1. "),
            "Normalize did not add blank line before numbered list",
        )


class TestResearchDocumentNormalizeHandlesMixedLists:
    """ResearchDocument normalize handles both bullet and numbered lists."""

    def test(self) -> None:
        session = ResearchSession(topic="T", tasks=tuple())
        document = ResearchDocument(session, HokusaiPalette())
        marker = uuid.uuid4()
        text = f"テキスト-{marker}\n* 箇条書き\n別のテキスト\n1. 番号付き"
        result = document._normalize(text)
        assert_that(
            result.count("\n\n"),
            is_(2),
            "Normalize did not add blank lines before both list types",
        )


class TestResearchDocumentBriefNormalizesNumberedLists:
    """ResearchDocument brief normalizes numbered lists in query."""

    def test(self) -> None:
        marker = uuid.uuid4()
        query = f"調査-{marker}:\n1. 最初\n2. 二番目"
        task = ResearchTask(query=query, status="completed", result=None)
        session = ResearchSession(topic="T", tasks=(task,))
        document = ResearchDocument(session, HokusaiPalette())
        assert_that(
            document._brief(),
            contains_string("<ol>"),
            "Brief did not render numbered list as <ol>",
        )
