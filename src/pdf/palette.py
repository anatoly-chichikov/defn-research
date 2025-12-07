"""Traditional Japanese color palette inspired by Hokusai."""
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
    """Traditional Japanese color palette inspired by The Great Wave."""

    def __init__(self) -> None:
        """Initialize palette with traditional Japanese colors."""
        self._indigo: Final[str] = "#264348"
        self._wave: Final[str] = "#1E4D7B"
        self._foam: Final[str] = "#E8E4D9"
        self._sky: Final[str] = "#C4B7A6"
        self._accent: Final[str] = "#B22222"
        self._ink: Final[str] = "#1A1A2E"

    def indigo(self) -> str:
        """Return Ai indigo color."""
        return self._indigo

    def wave(self) -> str:
        """Return deep wave blue color."""
        return self._wave

    def foam(self) -> str:
        """Return wave foam cream color."""
        return self._foam

    def sky(self) -> str:
        """Return aged paper sky color."""
        return self._sky

    def accent(self) -> str:
        """Return Bengara red accent color."""
        return self._accent

    def ink(self) -> str:
        """Return Sumi ink color."""
        return self._ink
