"""Research response domain objects."""
from __future__ import annotations

from abc import ABC
from abc import abstractmethod
from typing import Final
from urllib.parse import urlparse

from parallel.types import TaskRunResult
from parallel.types import TaskRunTextOutput

from src.domain.result import Source


class Responded(ABC):
    """Object representing API response."""

    @abstractmethod
    def identifier(self) -> str:
        """Return run identifier."""
        ...


class ResearchResponse(Responded):
    """Immutable response from deep research SDK."""

    def __init__(
        self,
        identifier: str,
        status: str,
        output: str,
        basis: list,
    ) -> None:
        """Initialize with response data."""
        self._identifier: Final[str] = identifier
        self._status: Final[str] = status
        self._output: Final[str] = output
        self._basis: Final[list] = basis

    def identifier(self) -> str:
        """Return run identifier."""
        return self._identifier

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
        """Extract sources from basis citations."""
        seen: set[str] = set()
        result: list[Source] = []
        for field in self._basis:
            citations = field.citations or []
            for citation in citations:
                url = citation.url
                if url and url not in seen:
                    seen.add(url)
                    excerpt = citation.excerpts[0] if citation.excerpts else ""
                    result.append(
                        Source(
                            title=citation.title or self._domain(url),
                            url=url,
                            excerpt=excerpt[:500],
                        )
                    )
        return tuple(result)

    def _domain(self, url: str) -> str:
        """Extract domain from URL."""
        parsed = urlparse(url)
        return parsed.netloc.replace("www.", "")

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
        )
