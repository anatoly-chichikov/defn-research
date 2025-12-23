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


class ResearchResponse(Responded):
    """Immutable response from deep research SDK."""

    def __init__(
        self,
        identifier: str,
        status: str,
        output: str,
        basis: list,
        cost: float = 0.0,
    ) -> None:
        """Initialize with response data."""
        self._identifier: Final[str] = identifier
        self._status: Final[str] = status
        self._output: Final[str] = self._strip(output)
        self._basis: Final[list] = basis
        self._cost: Final[float] = cost

    def identifier(self) -> str:
        """Return run identifier."""
        return self._identifier

    def cost(self) -> float:
        """Return total request cost."""
        return self._cost

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
            citations = field.citations or []
            confidence = getattr(field, "confidence", None)
            for citation in citations:
                link = citation.url
                if link:
                    link = self._clean(link)
                if link and link not in seen:
                    seen.add(link)
                    excerpt = citation.excerpts[0] if citation.excerpts else ""
                    result.append(
                        Source(
                            title=citation.title or self._domain(link),
                            url=link,
                            excerpt=excerpt[:500],
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
        return "".join(result)

    def _clean(self, url: str) -> str:
        """Remove utm parameters from URL."""
        parts = urlparse(url)
        if not parts.query:
            return url
        items = parse_qsl(parts.query, keep_blank_values=True)
        keep = [(key, value) for key, value in items if not key.lower().startswith("utm_")]
        query = urlencode(keep, doseq=True)
        parts = parts._replace(query=query)
        return urlunparse(parts)

    def serialize(self) -> dict:
        """Return response data as dictionary for JSON storage."""
        return {
            "run_id": self._identifier,
            "status": self._status,
            "output": self._output,
            "cost": self._cost,
            "sources": [s.serialize() for s in self.sources()],
        }

    @classmethod
    def parse(cls, result: TaskRunResult) -> ResearchResponse:
        """Create response from SDK result."""
        output = result.output
        content = output.content if isinstance(output, TaskRunTextOutput) else ""
        basis = output.basis if isinstance(output, TaskRunTextOutput) else []
        return cls(
            identifier=result.run.run_id,
            status=result.run.status,
            output=content,
            basis=basis,
            cost=0.0,
        )
