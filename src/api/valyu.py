"""Valyu deep research adapter."""
from __future__ import annotations

import time
from typing import Final
from urllib.parse import urlparse

import requests

from src.api.research import Researchable
from src.api.response import ResearchResponse


class ValyuCitation:
    """Valyu citation with URL and excerpt."""

    def __init__(self, title: str, url: str, excerpts: list[str]) -> None:
        """Initialize citation with title, URL, and excerpts."""
        self.title: Final[str] = title
        self.url: Final[str] = url
        self.excerpts: Final[list[str]] = excerpts


class ValyuField:
    """Valyu field with citations and confidence."""

    def __init__(self, citations: list[ValyuCitation], confidence: str) -> None:
        """Initialize field with citations and confidence."""
        self.citations: Final[list[ValyuCitation]] = citations
        self.confidence: Final[str] = confidence


class ValyuResearch(Researchable):
    """Deep research executor using Valyu deepresearch API."""

    def __init__(self, api) -> None:
        """Initialize with Valyu deepresearch handle."""
        self._api: Final = api
        self._cache: Final[dict[str, object]] = {}
        self._seen: Final[dict[str, int]] = {}
        self._token: str = ""
        self._science: Final[set[str]] = {
            "valyu/valyu-arxiv",
            "valyu/valyu-pubmed",
            "valyu/valyu-clinical-trials",
        }
        self._finance: Final[set[str]] = {
            "valyu/valyu-stocks",
            "valyu/sec-filings",
            "valyu/sec",
        }
        self._trust: Final[set[str]] = {
            "acm.org",
            "apnews.com",
            "arxiv.org",
            "bbc.co.uk",
            "bloomberg.com",
            "britannica.com",
            "cell.com",
            "doi.org",
            "economist.com",
            "elsevier.com",
            "europa.eu",
            "ft.com",
            "ieee.org",
            "ietf.org",
            "imf.org",
            "jstor.org",
            "nature.com",
            "nytimes.com",
            "oecd.org",
            "openalex.org",
            "ourworldindata.org",
            "reuters.com",
            "science.org",
            "sciencedirect.com",
            "springer.com",
            "tandfonline.com",
            "theguardian.com",
            "un.org",
            "worldbank.org",
            "who.int",
            "wikipedia.org",
            "wiley.com",
            "w3.org",
            "wsj.com",
        }

    def start(self, query: str, processor: str) -> str:
        """Create task and return run_id."""
        formats = ["markdown", "pdf"]
        task = self._api.create(input=query, model=processor, output_formats=formats)
        return task.deepresearch_id

    def stream(self, identifier: str) -> None:
        """Stream progress updates via wait callback."""
        self._token = identifier
        self._seen[identifier] = 0
        result = self._wait(identifier, 60, True)
        self._cache[identifier] = result

    def finish(self, identifier: str) -> ResearchResponse:
        """Get final result, works even if progress streaming skipped."""
        if identifier in self._cache:
            result = self._cache[identifier]
        else:
            result = self._wait(identifier, 60, False)
        output = self._value(result, "output", "")
        if isinstance(output, dict):
            output = output.get("markdown") or output.get("content") or ""
        sources = self._value(result, "sources", None) or []
        state = self._value(result, "status", "completed")
        status = self._value(state, "value", state)
        basis = self._basis(sources)
        raw = self._raw(result)
        return ResearchResponse(
            identifier=identifier,
            status=status,
            output=output,
            basis=basis,
            cost=self._cost(result),
            raw=raw,
        )

    def _wait(self, identifier: str, interval: int, emit: bool) -> object:
        """Return final status via raw polling or SDK wait."""
        if hasattr(self._api, "_base_url") and hasattr(self._api, "_headers"):
            return self._poll(identifier, interval, emit)
        callback = self._emit if emit else None
        return self._api.wait(identifier, poll_interval=interval, on_progress=callback)

    def _poll(self, identifier: str, interval: int, emit: bool) -> dict:
        """Poll raw status endpoint until completion."""
        start = time.time()
        limit = 7200
        while True:
            data = self._status(identifier)
            if data.get("success") is False:
                message = str(data.get("error") or data.get("message") or "status error").replace(".", "")
                raise ValueError(f"Valyu status failed for {identifier} with {message}")
            if emit:
                self._emit(data)
            state = data.get("status", "")
            value = state.get("value", state) if isinstance(state, dict) else state
            label = str(value).lower()
            if label == "completed":
                return data
            if label in {"failed", "cancelled", "canceled"}:
                message = str(data.get("error") or data.get("message") or "task failed").replace(".", "")
                raise ValueError(f"Valyu task failed for {identifier} with {message}")
            if time.time() - start > limit:
                raise TimeoutError(f"Valyu task timed out for {identifier} after {limit} seconds")
            time.sleep(interval)

    def _status(self, identifier: str) -> dict:
        """Return raw status payload from API."""
        base = getattr(self._api, "_base_url", "")
        headers = getattr(self._api, "_headers", {})
        if not base:
            parent = getattr(self._api, "_parent", None)
            base = getattr(parent, "base_url", "")
            headers = getattr(parent, "headers", {})
        url = f"{base}/deepresearch/tasks/{identifier}/status"
        response = requests.get(url, headers=headers, timeout=30)
        data = response.json()
        if not response.ok:
            message = str(data.get("error") or response.status_code).replace(".", "")
            raise RuntimeError(f"Valyu status request failed for {identifier} with {message}")
        return data

    def _basis(self, sources: list[dict]) -> list[ValyuField]:
        """Build basis list from Valyu sources."""
        result: list[ValyuField] = []
        for source in sources:
            url = self._value(source, "url", "") or ""
            if not url:
                continue
            content = self._value(source, "content", "") or ""
            snippet = self._value(source, "snippet", "") or ""
            description = self._value(source, "description", "") or ""
            text = content or snippet or description or ""
            title = self._value(source, "title", "") or self._domain(url)
            excerpt = text
            level = self._level(source)
            citation = ValyuCitation(title=title, url=url, excerpts=[excerpt])
            result.append(ValyuField(citations=[citation], confidence=level))
        return result

    def _level(self, source: dict) -> str:
        """Return confidence level mapped from Valyu metadata."""
        kind = self._value(source, "source", "") or ""
        form = self._value(source, "source_type", "") or self._value(source, "category", "") or ""
        count = self._value(source, "citation_count", 0) or 0
        names = self._value(source, "authors", []) or []
        code = self._value(source, "doi", "") or ""
        date = self._value(source, "publication_date", "") or ""
        score = self._value(source, "relevance_score", None)
        url = self._value(source, "url", "") or ""
        domain = self._domain(url) if url else ""
        detail = bool(count or names or code or date)
        level = "Unknown"
        if kind in self._science or form == "paper":
            level = "Medium"
            if count >= 10:
                level = "High"
        if form == "paper" and names and code:
            level = "High"
        if form == "paper" and not code and level != "High":
            level = "Medium"
        if kind == "web" and names and date and level != "High":
            level = "Medium"
        if kind in self._finance and level != "High":
            level = "Medium"
        if self._trusted(domain):
            if level == "Unknown":
                level = "Medium"
        if score is not None and score < 0.5 and detail and level == "Unknown":
            level = "Low"
        return level

    def _cost(self, result) -> float:
        """Extract total cost from Valyu result."""
        usage = getattr(result, "usage", None)
        total = getattr(usage, "total_cost", 0.0) if usage else 0.0
        return float(total)

    def _raw(self, result) -> dict:
        """Return raw response payload."""
        if isinstance(result, dict):
            return result
        try:
            data = result.model_dump()
            return data
        except Exception:
            try:
                data = result.dict()
                return data
            except Exception:
                return getattr(result, "__dict__", {})

    def _domain(self, url: str) -> str:
        """Extract domain from URL."""
        parsed = urlparse(url)
        return parsed.netloc.replace("www.", "")

    def _trusted(self, domain: str) -> bool:
        """Return True when domain matches trusted rules."""
        if not domain:
            return False
        if any(domain == item or domain.endswith(f".{item}") for item in self._trust):
            return True
        if domain.endswith(".gov") or ".gov." in domain:
            return True
        if domain.endswith(".edu") or ".edu." in domain:
            return True
        if domain.endswith(".ac") or ".ac." in domain:
            return True
        if domain.endswith(".mil") or ".mil." in domain:
            return True
        if domain.endswith(".int") or ".int." in domain:
            return True
        return False

    def _emit(self, value) -> None:
        """Emit progress info from Valyu wait callback."""
        status = self._value(value, "status", "") or ""
        progress = self._value(value, "progress", None)
        message = self._message(value)
        parts: list[str] = []
        if status:
            parts.append(str(status))
        if progress:
            current = self._value(progress, "current_step", None)
            total = self._value(progress, "total_steps", None)
            if current is not None and total is not None:
                parts.append(f"{current}/{total}")
        if message:
            parts.append(message)
        if not parts:
            parts.append(self._value(value, "message", "") or str(value))
        line = " | ".join(parts)
        print(f"[PROGRESS] {line}", flush=True)

    def _value(self, source, name: str, default):
        """Return value from dict or attribute."""
        if isinstance(source, dict):
            return source.get(name, default)
        return getattr(source, name, default)

    def _message(self, value) -> str:
        """Return newest message from status update."""
        items = self._value(value, "messages", None)
        if not items:
            return ""
        seen = self._seen.get(self._token, 0)
        if len(items) <= seen:
            return ""
        self._seen[self._token] = len(items)
        item = items[-1]
        text = self._value(item, "message", "") or self._value(item, "content", "") or self._value(item, "text", "")
        if isinstance(text, list):
            return " ".join(str(entry) for entry in text)
        if isinstance(text, dict):
            return str(text)
        return text or str(item)
