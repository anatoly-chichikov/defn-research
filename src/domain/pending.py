"""Pending research run for crash recovery."""
from __future__ import annotations

from typing import Final

from src.domain.result import Serializable


class PendingRun(Serializable):
    """Immutable pending run for persistence across crashes."""

    def __init__(
        self,
        identifier: str,
        query: str,
        processor: str,
        language: str,
        provider: str,
    ) -> None:
        """Initialize with run details."""
        self._identifier: Final[str] = identifier
        self._query: Final[str] = query
        self._processor: Final[str] = processor
        self._language: Final[str] = language
        self._provider: Final[str] = provider

    def identifier(self) -> str:
        """Return run_id from Parallel API."""
        return self._identifier

    def query(self) -> str:
        """Return research query."""
        return self._query

    def processor(self) -> str:
        """Return processor type."""
        return self._processor

    def language(self) -> str:
        """Return research language."""
        return self._language

    def provider(self) -> str:
        """Return provider name."""
        return self._provider

    def serialize(self) -> dict:
        """Return dictionary representation."""
        return {
            "run_id": self._identifier,
            "query": self._query,
            "processor": self._processor,
            "language": self._language,
            "provider": self._provider,
        }

    @classmethod
    def deserialize(cls, data: dict) -> PendingRun:
        """Create PendingRun from dictionary."""
        return cls(
            identifier=data["run_id"],
            query=data["query"],
            processor=data["processor"],
            language=data["language"],
            provider=data.get("provider") or "parallel",
        )
