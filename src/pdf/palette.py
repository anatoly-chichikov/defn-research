"""Semantic color palette inspired by Hokusai."""
from __future__ import annotations

from abc import ABC
from abc import abstractmethod
from typing import Final


class Colored(ABC):
    """Object with color value."""

    @abstractmethod
    def hex(self) -> str:
        """Return hex color value."""
        ...


class HokusaiPalette:
    """Semantic color palette inspired by The Great Wave."""

    def __init__(self) -> None:
        """Initialize palette with semantic colors."""
        self._bg: Final[str] = "#F6EFE3"
        self._text: Final[str] = "#1C2430"
        self._heading: Final[str] = "#193D5E"
        self._link: Final[str] = "#3A5F88"
        self._muted: Final[str] = "#6B645A"
        self._quotebg: Final[str] = "#E3D9C6"
        self._accent: Final[str] = "#D04A35"
        self._codebg: Final[str] = "#1C2833"
        self._codeinlinebg: Final[str] = "#DDD5C5"
        self._border: Final[str] = "#BFB5A3"

    def bg(self) -> str:
        """Return page background color."""
        return self._bg

    def text(self) -> str:
        """Return body text color."""
        return self._text

    def heading(self) -> str:
        """Return heading color."""
        return self._heading

    def link(self) -> str:
        """Return link color."""
        return self._link

    def muted(self) -> str:
        """Return muted text color."""
        return self._muted

    def quotebg(self) -> str:
        """Return blockquote background color."""
        return self._quotebg

    def accent(self) -> str:
        """Return accent color."""
        return self._accent

    def codebg(self) -> str:
        """Return code block background color."""
        return self._codebg

    def codeinlinebg(self) -> str:
        """Return inline code background color."""
        return self._codeinlinebg

    def border(self) -> str:
        """Return border color."""
        return self._border
