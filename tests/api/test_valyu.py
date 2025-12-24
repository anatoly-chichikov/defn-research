"""Tests for ValyuResearch adapter."""
from __future__ import annotations

import http.server
import json
import random
import threading
import time
from typing import Final

from hamcrest import assert_that
from hamcrest import equal_to

from src.api.valyu import ValyuResearch


class FakeTask:
    """Fake task with deepresearch identifier."""

    def __init__(self, identifier: str) -> None:
        """Initialize with identifier."""
        self.deepresearch_id: Final[str] = identifier


class FakeUsage:
    """Fake usage metrics container."""

    def __init__(self, total: float) -> None:
        """Initialize with total cost."""
        self.total_cost: Final[float] = total


class FakeResult:
    """Fake Valyu result container."""

    def __init__(self, output: str, sources: list[dict], total: float, status: str) -> None:
        """Initialize with output, sources, total cost, and status."""
        self.output: Final[str] = output
        self.sources: Final[list[dict]] = sources
        self.usage: Final[FakeUsage] = FakeUsage(total)
        self.status: Final[str] = status


class FakeApi:
    """Fake Valyu deepresearch API."""

    def __init__(self, identifier: str, result: FakeResult) -> None:
        """Initialize with identifier and result."""
        self._task: Final[FakeTask] = FakeTask(identifier)
        self._result: Final[FakeResult] = result
        self._record: dict = {}

    def create(self, **data) -> FakeTask:
        """Return fake task and record input."""
        self._record = data
        return self._task

    def wait(self, identifier: str, **data) -> FakeResult:
        """Return fake result."""
        return self._result

    def record(self) -> dict:
        """Return recorded create data."""
        return self._record


class RawApi:
    """Fake Valyu API exposing base url and headers."""

    def __init__(self, base: str) -> None:
        """Initialize with base URL."""
        self._base_url: Final[str] = base
        self._headers: Final[dict] = {}

    def wait(self, identifier: str, **data) -> object:
        """Reject SDK wait usage."""
        raise ValueError("Valyu wait should not be called")


class Test_valyu_research_returns_run_identifier:
    """ValyuResearch start returns run identifier."""

    def test(self) -> None:
        """ValyuResearch returns identifier from create."""
        seed = sum(ord(c) for c in __name__) + 21
        generator = random.Random(seed)
        identifier = f"dr_{generator.randrange(100000)}"
        output = "".join(chr(generator.randrange(0x0370, 0x03ff)) for _ in range(6))
        sources = [{"url": f"https://example.com/{generator.randrange(10000)}"}]
        api = FakeApi(identifier, FakeResult(output, sources, 0.0, "completed"))
        research = ValyuResearch(api)
        result = research.start(output, f"lite-{generator.randrange(1000)}")
        assert_that(result, equal_to(identifier), "start() did not return expected identifier")


class Test_valyu_research_maps_high_confidence_from_metadata:
    """ValyuResearch maps high confidence from paper metadata."""

    def test(self) -> None:
        """ValyuResearch yields High confidence when doi and authors present."""
        seed = sum(ord(c) for c in __name__) + 23
        generator = random.Random(seed)
        title = "".join(chr(generator.randrange(0x0400, 0x04ff)) for _ in range(6))
        content = "".join(chr(generator.randrange(0x0370, 0x03ff)) for _ in range(8))
        url = f"https://example.com/{generator.randrange(10000)}"
        date = f"202{generator.randrange(0, 5)}-0{generator.randrange(1, 9)}-1{generator.randrange(0, 9)}"
        source = {
            "title": title,
            "url": url,
            "content": content,
            "source": f"source-{generator.randrange(1000)}",
            "source_type": "paper",
            "authors": [f"author-{generator.randrange(1000)}"],
            "doi": f"10.{generator.randrange(1000)}/{generator.randrange(10000)}",
            "publication_date": date,
            "citation_count": 1,
            "relevance_score": 0.9,
        }
        api = FakeApi(f"dr_{generator.randrange(100000)}", FakeResult(content, [source], 0.0, "completed"))
        research = ValyuResearch(api)
        response = research.finish("dr_x")
        assert_that(
            response.sources()[0].confidence(),
            equal_to("High"),
            "confidence was not high for paper with doi",
        )


class Test_valyu_research_maps_unknown_confidence_for_missing_metadata:
    """ValyuResearch maps unknown confidence when metadata is missing."""

    def test(self) -> None:
        """ValyuResearch yields Unknown confidence when metadata is missing."""
        seed = sum(ord(c) for c in __name__) + 29
        generator = random.Random(seed)
        title = "".join(chr(generator.randrange(0x0530, 0x058f)) for _ in range(6))
        content = "".join(chr(generator.randrange(0x0590, 0x05ff)) for _ in range(9))
        url = f"https://example.com/{generator.randrange(10000)}"
        source = {"title": title, "url": url, "content": content}
        api = FakeApi(f"dr_{generator.randrange(100000)}", FakeResult(content, [source], 0.0, "completed"))
        research = ValyuResearch(api)
        response = research.finish("dr_x")
        assert_that(
            response.sources()[0].confidence(),
            equal_to("Unknown"),
            "confidence was not unknown for missing metadata",
        )


class Test_valyu_research_maps_medium_confidence_for_trusted_domain:
    """ValyuResearch maps medium confidence for trusted domains."""

    def test(self) -> None:
        """ValyuResearch yields Medium confidence for trusted domain."""
        seed = sum(ord(c) for c in __name__) + 31
        generator = random.Random(seed)
        title = "".join(chr(generator.randrange(0x0410, 0x044f)) for _ in range(7))
        content = "".join(chr(generator.randrange(0x3040, 0x309f)) for _ in range(8))
        url = f"https://www.wikipedia.org/{generator.randrange(10000)}"
        source = {"title": title, "url": url, "content": content}
        api = FakeApi(f"dr_{generator.randrange(100000)}", FakeResult(content, [source], 0.0, "completed"))
        research = ValyuResearch(api)
        response = research.finish("dr_x")
        assert_that(
            response.sources()[0].confidence(),
            equal_to("Medium"),
            "confidence was not medium for trusted domain",
        )


class Test_valyu_research_maps_medium_confidence_for_public_sector_domain:
    """ValyuResearch maps medium confidence for public sector domains."""

    def test(self) -> None:
        """ValyuResearch yields Medium confidence for gov domains."""
        seed = sum(ord(c) for c in __name__) + 33
        generator = random.Random(seed)
        title = "".join(chr(generator.randrange(0x0530, 0x058f)) for _ in range(6))
        content = "".join(chr(generator.randrange(0x0400, 0x04ff)) for _ in range(9))
        url = f"https://www.nasa.gov/{generator.randrange(10000)}"
        source = {"title": title, "url": url, "content": content}
        api = FakeApi(f"dr_{generator.randrange(100000)}", FakeResult(content, [source], 0.0, "completed"))
        research = ValyuResearch(api)
        response = research.finish("dr_x")
        assert_that(
            response.sources()[0].confidence(),
            equal_to("Medium"),
            "confidence was not medium for gov domain",
        )


class Test_valyu_research_maps_low_confidence_for_low_relevance:
    """ValyuResearch maps low confidence for low relevance."""

    def test(self) -> None:
        """ValyuResearch yields Low confidence when relevance score is low."""
        seed = sum(ord(c) for c in __name__) + 27
        generator = random.Random(seed)
        content = "".join(chr(generator.randrange(0x3040, 0x309f)) for _ in range(7))
        url = f"https://example.com/{generator.randrange(10000)}"
        date = f"202{generator.randrange(0, 5)}-0{generator.randrange(1, 9)}-2{generator.randrange(0, 9)}"
        source = {
            "title": content,
            "url": url,
            "content": content,
            "source": "web",
            "source_type": "web",
            "authors": [],
            "publication_date": date,
            "relevance_score": 0.2,
        }
        api = FakeApi(f"dr_{generator.randrange(100000)}", FakeResult(content, [source], 0.0, "completed"))
        research = ValyuResearch(api)
        response = research.finish("dr_x")
        assert_that(
            response.sources()[0].confidence(),
            equal_to("Low"),
            "confidence was not low for low relevance",
        )


class Test_valyu_research_reads_progress_messages:
    """ValyuResearch reads progress messages."""

    def test_valyu_research_reads_progress_messages(self) -> None:
        """ValyuResearch reads progress messages."""
        seed = sum(ord(c) for c in __name__) + 43
        generator = random.Random(seed)
        message = "".join(chr(generator.randrange(0x0400, 0x04ff)) for _ in range(6))
        api = FakeApi("trun_x", FakeResult(message, [], 0.0, "completed"))
        research = ValyuResearch(api)
        research._token = "trun_x"
        research._seen["trun_x"] = 0
        result = research._message({"messages": [{"message": message}]})
        assert_that(result, equal_to(message), "message was not returned")


class Test_valyu_research_formats_list_messages:
    """ValyuResearch formats list messages."""

    def test_valyu_research_formats_list_messages(self) -> None:
        """ValyuResearch formats list messages."""
        seed = sum(ord(c) for c in __name__) + 47
        generator = random.Random(seed)
        left = "".join(chr(generator.randrange(0x0400, 0x04ff)) for _ in range(4))
        right = "".join(chr(generator.randrange(0x0530, 0x058f)) for _ in range(4))
        api = FakeApi("trun_x", FakeResult("", [], 0.0, "completed"))
        research = ValyuResearch(api)
        research._token = "trun_x"
        research._seen["trun_x"] = 0
        result = research._message({"messages": [{"message": [left, right]}]})
        assert_that(result, equal_to(f"{left} {right}"), "list message was not joined")


class Test_valyu_research_uses_raw_status_payload:
    """ValyuResearch uses raw status payload without validation errors."""

    def test_valyu_research_uses_raw_status_payload(self) -> None:
        """ValyuResearch uses raw status payload without validation errors."""
        seed = sum(ord(c) for c in __name__) + 59
        generator = random.Random(seed)
        identifier = f"dr_{generator.randrange(100000)}"
        output = "".join(chr(generator.randrange(0x0400, 0x04ff)) for _ in range(6))
        title = "".join(chr(generator.randrange(0x0370, 0x03ff)) for _ in range(5))
        url = f"http://example.com/{generator.randrange(1000)}"
        payload = {
            "success": True,
            "status": "completed",
            "output": {"markdown": output},
            "sources": [{"title": title, "url": url, "content": output}],
            "images": [{
                "image_id": f"img_{generator.randrange(1000)}",
                "image_type": "chart",
                "deepresearch_id": identifier,
                "title": title,
                "description": output,
                "image_url": url,
                "s3_key": f"key_{generator.randrange(1000)}",
                "created_at": generator.randrange(1000),
                "chart_type": "doughnut",
            }],
        }
        class Handler(http.server.BaseHTTPRequestHandler):
            """Serve static Valyu status payload."""
            data = payload
            def do_GET(self) -> None:
                """Handle GET request."""
                body = json.dumps(self.data).encode("utf-8")
                self.send_response(200)
                self.send_header("Content-Type", "application/json")
                self.send_header("Content-Length", str(len(body)))
                self.end_headers()
                self.wfile.write(body)
            def log_message(self, format, *args) -> None:
                """Suppress logging."""
                return None
        server = http.server.HTTPServer(("127.0.0.1", 0), Handler)
        thread = threading.Thread(target=server.serve_forever)
        thread.daemon = True
        thread.start()
        base = f"http://127.0.0.1:{server.server_port}"
        api = RawApi(base)
        research = ValyuResearch(api)
        result = None
        error = None
        for _ in range(3):
            try:
                result = research.finish(identifier)
                break
            except Exception as exc:
                error = exc
                time.sleep(0.05)
        server.shutdown()
        server.server_close()
        thread.join(1)
        if result is None:
            raise error
        assert_that(result.markdown(), equal_to(output), "Markdown did not match raw payload")
