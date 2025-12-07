"""Task result and source domain objects."""
from __future__ import annotations

from abc import ABC
from abc import abstractmethod
from typing import Final


class Sourced(ABC):
    """Object with URL source."""

    @abstractmethod
    def url(self) -> str:
        """Return source URL."""
        ...


class Summarized(ABC):
    """Object with text summary."""

    @abstractmethod
    def summary(self) -> str:
        """Return text summary."""
        ...


class Serializable(ABC):
    """Object that can serialize to dictionary."""

    @abstractmethod
    def serialize(self) -> dict:
        """Return dictionary representation."""
        ...


class Deserializable(ABC):
    """Object that can deserialize from dictionary."""

    @classmethod
    @abstractmethod
    def deserialize(cls, data: dict) -> Deserializable:
        """Create instance from dictionary."""
        ...


class Source(Sourced, Serializable):
    """Immutable research source with title and excerpt."""

    def __init__(self, title: str, url: str, excerpt: str) -> None:
        """Initialize source with title, URL, and excerpt."""
        self._title: Final[str] = title
        self._url: Final[str] = url
        self._excerpt: Final[str] = excerpt

    def title(self) -> str:
        """Return source title."""
        return self._title

    def url(self) -> str:
        """Return source URL."""
        return self._url

    def excerpt(self) -> str:
        """Return relevant excerpt."""
        return self._excerpt

    def serialize(self) -> dict:
        """Return dictionary representation."""
        return {
            "title": self._title,
            "url": self._url,
            "excerpt": self._excerpt,
        }

    @classmethod
    def deserialize(cls, data: dict) -> Source:
        """Create Source from dictionary."""
        return cls(
            title=data["title"],
            url=data["url"],
            excerpt=data["excerpt"],
        )


class TaskResult(Summarized, Serializable):
    """Immutable result containing summary and sources."""

    def __init__(self, summary: str, sources: tuple[Source, ...]) -> None:
        """Initialize result with summary and sources."""
        self._summary: Final[str] = summary
        self._sources: Final[tuple[Source, ...]] = sources

    def summary(self) -> str:
        """Return synthesis of findings."""
        return self._summary

    def sources(self) -> tuple[Source, ...]:
        """Return tuple of sources."""
        return self._sources

    def serialize(self) -> dict:
        """Return dictionary representation."""
        return {
            "summary": self._summary,
            "sources": [s.serialize() for s in self._sources],
        }

    @classmethod
    def deserialize(cls, data: dict) -> TaskResult:
        """Create TaskResult from dictionary."""
        raw = data["summary"]
        text = raw["content"] if isinstance(raw, dict) else raw
        return cls(
            summary=text,
            sources=tuple(Source.deserialize(s) for s in data["sources"]),
        )
