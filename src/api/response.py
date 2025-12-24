"""Research response domain objects."""
from __future__ import annotations

from abc import ABC
from abc import abstractmethod
import re
from typing import Final
from urllib.parse import parse_qsl
from urllib.parse import urlencode
from urllib.parse import urlparse
from urllib.parse import urlunparse

from parallel.types import TaskRunResult
from parallel.types import TaskRunTextOutput

from src.domain.result import Source


class Responded(ABC):
    """Object representing API response."""

    @abstractmethod
    def identifier(self) -> str:
        """Return run identifier."""
        ...

    @abstractmethod
    def cost(self) -> float:
        """Return total request cost."""
        ...

    @abstractmethod
    def raw(self) -> dict:
        """Return raw response payload."""
        ...


class ResearchResponse(Responded):
    """Immutable response from deep research SDK."""

    def __init__(
        self,
        identifier: str,
        status: str,
        output: str,
        basis: list,
        cost: float = 0.0,
        raw: dict = {},
    ) -> None:
        """Initialize with response data."""
        self._identifier: Final[str] = identifier
        self._status: Final[str] = status
        self._output: Final[str] = self._strip(output)
        self._basis: Final[list] = basis
        self._cost: Final[float] = cost
        self._raw: Final[dict] = raw

    def identifier(self) -> str:
        """Return run identifier."""
        return self._identifier

    def cost(self) -> float:
        """Return total request cost."""
        return self._cost

    def raw(self) -> dict:
        """Return raw response payload."""
        return self._raw

    def completed(self) -> bool:
        """Return True if research completed successfully."""
        return self._status == "completed"

    def failed(self) -> bool:
        """Return True if research failed."""
        return self._status == "failed"

    def markdown(self) -> str:
        """Return research output as markdown."""
        return self._output

    def sources(self) -> tuple[Source, ...]:
        """Extract sources from basis citations with confidence."""
        seen: set[str] = set()
        result: list[Source] = []
        for field in self._basis:
            citations = self._value(field, "citations", []) or []
            confidence = self._value(field, "confidence", None)
            for citation in citations:
                link = self._value(citation, "url", "")
                if link:
                    link = self._clean(link)
                if link and link not in seen:
                    seen.add(link)
                    excerpts = self._value(citation, "excerpts", []) or []
                    excerpt = excerpts[0] if excerpts else ""
                    result.append(
                        Source(
                            title=self._value(citation, "title", "") or self._domain(link),
                            url=link,
                            excerpt=excerpt,
                            confidence=confidence,
                        )
                    )
        return tuple(result)

    def _domain(self, url: str) -> str:
        """Extract domain from URL."""
        parsed = urlparse(url)
        return parsed.netloc.replace("www.", "")

    def _strip(self, text: str) -> str:
        """Remove tracking parameters from URLs in text."""
        pattern = r"https?://[^\s\)\]]+"
        result: list[str] = []
        last = 0
        for match in re.finditer(pattern, text):
            result.append(text[last:match.start()])
            result.append(self._clean(match.group(0)))
            last = match.end()
        result.append(text[last:])
        return self._purge("".join(result))

    def _clean(self, url: str) -> str:
        """Remove utm parameters from URL."""
        parts = urlparse(url)
        if not parts.query:
            return url
        items = parse_qsl(parts.query, keep_blank_values=True)
        keep = [(key, value) for key, value in items if not key.lower().startswith("utm_")]
        if len(keep) == len(items):
            return url
        query = urlencode(keep, doseq=True)
        parts = parts._replace(query=query)
        return urlunparse(parts)

    def _purge(self, text: str) -> str:
        """Remove leftover utm fragments from text."""
        return re.sub(r"(\?utm_[^\s\)\]]+|&utm_[^\s\)\]]+)", "", text)

    def _value(self, item, name: str, default):
        """Return attribute or mapping value with fallback."""
        value = getattr(item, name, None)
        if value is not None:
            return value
        try:
            return item[name]
        except Exception:
            return default

    @classmethod
    def parse(cls, result: TaskRunResult) -> ResearchResponse:
        """Create response from SDK result."""
        output = result.output
        content = output.content if isinstance(output, TaskRunTextOutput) else ""
        basis = output.basis if isinstance(output, TaskRunTextOutput) else []
        raw = {}
        try:
            raw = result.model_dump()
        except Exception:
            try:
                raw = result.dict()
            except Exception:
                raw = {}
        return cls(
            identifier=result.run.run_id,
            status=result.run.status,
            output=content,
            basis=basis,
            cost=0.0,
            raw=raw,
        )
