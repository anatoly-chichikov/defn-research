"""SVG wave pattern inspired by The Great Wave off Kanagawa."""
from __future__ import annotations

from abc import ABC
from abc import abstractmethod
from typing import Final

from src.pdf.palette import HokusaiPalette


class Renderable(ABC):
    """Object that can render to string."""

    @abstractmethod
    def render(self) -> str:
        """Return rendered representation."""
        ...


class WavePattern(Renderable):
    """SVG wave pattern inspired by Hokusai Great Wave."""

    def __init__(self, palette: HokusaiPalette) -> None:
        """Initialize with color palette."""
        self._palette: Final[HokusaiPalette] = palette

    def render(self) -> str:
        """Return SVG wave pattern."""
        return f"""<svg viewBox="0 0 1200 200" preserveAspectRatio="none" xmlns="http://www.w3.org/2000/svg">
  <defs>
    <linearGradient id="waveGradient" x1="0%" y1="0%" x2="0%" y2="100%">
      <stop offset="0%" style="stop-color:{self._palette.wave()};stop-opacity:0.9"/>
      <stop offset="100%" style="stop-color:{self._palette.indigo()};stop-opacity:1"/>
    </linearGradient>
  </defs>
  <path d="M0,100 C100,150 200,50 300,100 C400,150 500,50 600,100 C700,150 800,50 900,100 C1000,150 1100,50 1200,100 L1200,200 L0,200 Z" fill="url(#waveGradient)"/>
  <path d="M0,120 C150,80 250,160 400,120 C550,80 650,160 800,120 C950,80 1050,160 1200,120 L1200,200 L0,200 Z" fill="{self._palette.indigo()}" opacity="0.7"/>
  <path d="M0,140 C200,180 400,100 600,140 C800,180 1000,100 1200,140 L1200,200 L0,200 Z" fill="{self._palette.ink()}" opacity="0.5"/>
  <circle cx="100" cy="90" r="8" fill="{self._palette.foam()}" opacity="0.8"/>
  <circle cx="350" cy="70" r="6" fill="{self._palette.foam()}" opacity="0.6"/>
  <circle cx="600" cy="85" r="10" fill="{self._palette.foam()}" opacity="0.7"/>
  <circle cx="850" cy="75" r="7" fill="{self._palette.foam()}" opacity="0.5"/>
  <circle cx="1100" cy="80" r="9" fill="{self._palette.foam()}" opacity="0.8"/>
</svg>"""


class WaveFooter(Renderable):
    """SVG wave footer pattern."""

    def __init__(self, palette: HokusaiPalette) -> None:
        """Initialize with color palette."""
        self._palette: Final[HokusaiPalette] = palette

    def render(self) -> str:
        """Return SVG wave footer."""
        return f"""<svg viewBox="0 0 1200 100" preserveAspectRatio="none" xmlns="http://www.w3.org/2000/svg">
  <path d="M0,0 L0,50 C150,80 350,20 600,50 C850,80 1050,20 1200,50 L1200,0 Z" fill="{self._palette.wave()}" opacity="0.3"/>
  <path d="M0,0 L0,30 C200,60 400,10 600,30 C800,50 1000,10 1200,30 L1200,0 Z" fill="{self._palette.indigo()}" opacity="0.5"/>
</svg>"""
