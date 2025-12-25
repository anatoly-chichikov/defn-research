"""Tests for ResearchDocument."""
from __future__ import annotations

import os
import random
import tempfile
import uuid
from datetime import datetime
from pathlib import Path

import pytest
from hamcrest import all_of
from hamcrest import assert_that
from hamcrest import contains_string
from hamcrest import equal_to
from hamcrest import is_
from hamcrest import not_

from src.domain.result import Source
from src.domain.result import TaskResult
from src.domain.pending import PendingRun
from src.domain.session import ResearchSession
from src.domain.task import ResearchTask
from src.storage.organizer import OutputOrganizer

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


class Test_research_document_renders_exploration_brief_title:
    """ResearchDocument render includes exploration brief title."""

    def test(self) -> None:
        """ResearchDocument render includes exploration brief title."""
        seed = sum(ord(c) for c in __name__) + 7
        maker = random.Random(seed)
        token = "".join(chr(maker.randrange(0x0400, 0x04ff)) for _ in range(6))
        task = ResearchTask(query=token, status="completed", result=None, service="valyu.ai")
        session = ResearchSession(topic=token, tasks=(task,))
        document = ResearchDocument(session, HokusaiPalette())
        html = document.render()
        assert_that(html, contains_string("<h1>Exploration Brief</h1>"), "Exploration Brief title was missing")


class Test_research_document_includes_authentic_hokusai_palette:
    """ResearchDocument render includes authentic Hokusai palette colors."""

    def test(self) -> None:
        """ResearchDocument render includes authentic Hokusai palette colors."""
        seed = sum(ord(c) for c in __name__) + 91
        generator = random.Random(seed)
        topic = "".join(chr(generator.randrange(0x3040, 0x309f)) for _ in range(6))
        session = ResearchSession(topic=topic, tasks=tuple())
        document = ResearchDocument(session, HokusaiPalette())
        html = document.render()
        assert_that(
            html,
            all_of(
                contains_string("#F6EFE3"),
                contains_string("#1C2430"),
                contains_string("#193D5E"),
                contains_string("#3A5F88"),
                contains_string("#6B645A"),
                contains_string("#E3D9C6"),
                contains_string("#D04A35"),
                contains_string("#1C2833"),
                contains_string("#DDD5C5"),
                contains_string("#BFB5A3"),
            ),
            "Rendered document did not include authentic Hokusai palette colors",
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


class Test_research_document_inserts_blank_line_before_hyphen_list:
    """ResearchDocument inserts blank line before hyphen lists."""

    def test(self) -> None:
        """ResearchDocument inserts blank line before hyphen list markers."""
        seed = sum(ord(c) for c in __name__) + 11
        maker = random.Random(seed)
        topic = "".join(chr(maker.randrange(0x0400, 0x04ff)) for _ in range(6))
        head = "".join(chr(maker.randrange(0x3040, 0x309f)) for _ in range(6))
        item = "".join(chr(maker.randrange(0x0370, 0x03ff)) for _ in range(6))
        tail = "".join(chr(maker.randrange(0x0100, 0x017f)) for _ in range(6))
        text = f"{head}:\n- {item}\n- {tail}"
        stamp = datetime.fromtimestamp(maker.randrange(1600000000, 1700000000))
        pending = PendingRun(identifier=topic, query=head, processor=item, language=tail, provider=topic)
        session = ResearchSession(topic=topic, tasks=tuple(), identifier=item, created=stamp, pending=pending)
        path = Path(tempfile.gettempdir()) / f"{maker.randrange(1000000)}.png"
        document = ResearchDocument(session, HokusaiPalette(), path)
        result = document._normalize(text)
        assert_that(result, contains_string(":\n\n-"), "Normalized text did not insert a blank line before hyphen list")


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


class Test_research_document_strips_sources_section:
    """ResearchDocument strips Sources section from text."""

    def test(self) -> None:
        """ResearchDocument strips Sources section from text."""
        seed = sum(ord(c) for c in __name__) + 61
        generator = random.Random(seed)
        token = "".join(chr(generator.randrange(0x0400, 0x04ff)) for _ in range(6))
        link = f"https://example.com/{generator.randrange(1000)}"
        text = f"{token}\n\n## Sources\n1. {link}\n2. {link}"
        session = ResearchSession(topic=token, tasks=tuple())
        document = ResearchDocument(session, HokusaiPalette())
        assert_that(
            document._strip(text),
            not_(contains_string("Sources")),
            "Sources section was not stripped",
        )


class Test_research_document_keeps_sources_without_links:
    """ResearchDocument keeps Sources section without links."""

    def test(self) -> None:
        """ResearchDocument keeps Sources section without links."""
        seed = sum(ord(c) for c in __name__) + 67
        generator = random.Random(seed)
        token = "".join(chr(generator.randrange(0x0400, 0x04ff)) for _ in range(6))
        note = "".join(chr(generator.randrange(0x0370, 0x03ff)) for _ in range(5))
        text = f"{token}\n\n## Sources\n1. {note}\n2. {note}"
        session = ResearchSession(topic=token, tasks=tuple())
        document = ResearchDocument(session, HokusaiPalette())
        assert_that(
            document._strip(text),
            contains_string("Sources"),
            "Sources section was removed without links",
        )


class Test_research_document_keeps_sources_when_not_last_section:
    """ResearchDocument keeps Sources section when followed by another header."""

    def test(self) -> None:
        """ResearchDocument keeps Sources section when followed by another header."""
        seed = sum(ord(c) for c in __name__) + 71
        generator = random.Random(seed)
        token = "".join(chr(generator.randrange(0x0400, 0x04ff)) for _ in range(6))
        note = "".join(chr(generator.randrange(0x0370, 0x03ff)) for _ in range(5))
        url = f"https://example.com/{generator.randrange(1000)}"
        text = f"{token}\n\n## Sources\n1. {url}\n\n## Далее\n{note}"
        session = ResearchSession(topic=token, tasks=tuple())
        document = ResearchDocument(session, HokusaiPalette())
        assert_that(
            document._strip(text),
            contains_string("Sources"),
            "Sources section was removed before end",
        )


class Test_research_document_inserts_images_before_sources:
    """ResearchDocument inserts images before Sources section."""

    def test(self) -> None:
        """ResearchDocument inserts images before Sources section."""
        seed = sum(ord(c) for c in __name__) + 73
        generator = random.Random(seed)
        token = "".join(chr(generator.randrange(0x0400, 0x04ff)) for _ in range(6))
        title = "".join(chr(generator.randrange(0x0370, 0x03ff)) for _ in range(5))
        url = f"https://example.com/{generator.randrange(1000)}"
        image = f"https://example.com/{generator.randrange(1000)}.png"
        sources = "".join(chr(code) for code in (83, 111, 117, 114, 99, 101, 115))
        images = "".join(chr(code) for code in (73, 109, 97, 103, 101, 115))
        text = f"{token}\n\n## {sources}\n1. {url}"
        raw = {"images": [{"image_url": image, "title": title}]}
        task = ResearchTask(query=token, status="completed", result=None, service="valyu.ai")
        session = ResearchSession(topic=token, tasks=(task,))
        document = ResearchDocument(session, HokusaiPalette())
        result = document._images(text, raw, task)
        expect = f"## {images}\n\n![{title}]({image})\n\n## {sources}"
        assert_that(
            result,
            contains_string(expect),
            "Images were not inserted before Sources",
        )


class Test_research_document_preserves_signed_image_urls:
    """ResearchDocument preserves image URLs without utm parameters."""

    def test(self) -> None:
        """ResearchDocument preserves image URLs without utm parameters."""
        seed = sum(ord(c) for c in __name__) + 79
        generator = random.Random(seed)
        token = "".join(chr(generator.randrange(0x0400, 0x04ff)) for _ in range(6))
        key = "".join(chr(generator.randrange(0x0370, 0x03ff)) for _ in range(4))
        val = "".join(chr(generator.randrange(0x0530, 0x058f)) for _ in range(4))
        link = f"https://example.com/{generator.randrange(1000)}?{key}={val}&sig={generator.randrange(1000)}"
        session = ResearchSession(topic=token, tasks=tuple())
        document = ResearchDocument(session, HokusaiPalette())
        assert_that(
            document._trim(link),
            equal_to(link),
            "Image URL was changed despite missing utm parameters",
        )


class Test_research_document_uses_cached_image_file:
    """ResearchDocument uses cached image file when available."""

    def test(self) -> None:
        """ResearchDocument uses cached image file when available."""
        seed = sum(ord(c) for c in __name__) + 83
        generator = random.Random(seed)
        token = "".join(chr(generator.randrange(0x0400, 0x04ff)) for _ in range(6))
        title = "".join(chr(generator.randrange(0x0370, 0x03ff)) for _ in range(5))
        code = "".join(chr(generator.randrange(0x0530, 0x058f)) for _ in range(4))
        temp = tempfile.mkdtemp()
        old = os.getcwd()
        os.chdir(temp)
        task = ResearchTask(query=token, status="completed", result=None, service="valyu.ai")
        session = ResearchSession(topic=token, tasks=(task,))
        maker = OutputOrganizer(Path("output"))
        name = maker.name(session.created(), session.topic(), session.id())
        folder = Path("output") / name / "valyu" / "images"
        folder.mkdir(parents=True, exist_ok=True)
        path = folder / f"{code}.png"
        path.write_bytes(b"image")
        raw = {"images": [{"image_url": "https://example.com/image.png", "image_id": code, "title": title}]}
        document = ResearchDocument(session, HokusaiPalette())
        text = f"{token}\n\n## Sources\n1. https://example.com"
        expect = path.resolve().as_uri()
        result = document._images(text, raw, task)
        os.chdir(old)
        assert_that(result, contains_string(expect), "Cached image file was not used")


class Test_research_document_strips_utm_fragments_from_text:
    """ResearchDocument strips utm fragments from text."""

    def test(self) -> None:
        """ResearchDocument strips utm fragments from text."""
        seed = sum(ord(c) for c in __name__) + 31
        generator = random.Random(seed)
        token = "".join(chr(generator.randrange(0x0400, 0x04ff)) for _ in range(6))
        mark = "".join(chr(code) for code in (117, 116, 109, 95))
        label = "".join(chr(generator.randrange(0x0370, 0x03ff)) for _ in range(5))
        value = "".join(chr(generator.randrange(0x0530, 0x058f)) for _ in range(4))
        number = generator.randrange(10, 99)
        text = f"{token} [{number}]?{mark}{label}={value}) {token}"
        session = ResearchSession(topic=token, tasks=tuple())
        document = ResearchDocument(session, HokusaiPalette())
        assert_that(
            document._clean(text),
            not_(contains_string(mark)),
            "utm fragments were not stripped from text",
        )
