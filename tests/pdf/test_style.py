"""Tests for HokusaiStyle."""
from __future__ import annotations

import random

from hamcrest import assert_that
from hamcrest import contains_string

from src.pdf.style import HokusaiStyle


class Palette:
    """Palette with irregular values."""

    def __init__(self, value: str) -> None:
        """Initialize with value."""
        self._value: str = value

    def bg(self) -> str:
        """Return background color."""
        return self._value

    def text(self) -> str:
        """Return text color."""
        return self._value

    def heading(self) -> str:
        """Return heading color."""
        return self._value

    def link(self) -> str:
        """Return link color."""
        return self._value

    def muted(self) -> str:
        """Return muted color."""
        return self._value

    def quotebg(self) -> str:
        """Return quote background color."""
        return self._value

    def accent(self) -> str:
        """Return accent color."""
        return self._value

    def codebg(self) -> str:
        """Return code background color."""
        return self._value

    def codeinlinebg(self) -> str:
        """Return inline code background color."""
        return self._value

    def border(self) -> str:
        """Return border color."""
        return self._value


class TestHokusaiStyleIncludesImageConstraints:
    """HokusaiStyle includes image constraints for synthesis."""

    def test(self) -> None:
        """HokusaiStyle includes image constraints for synthesis."""
        seed = sum(ord(c) for c in __name__) + 13
        randomer = random.Random(seed)
        value = "".join(chr(randomer.randrange(0x0400, 0x04ff)) for _ in range(6))
        palette = Palette(value)
        style = HokusaiStyle(palette)
        css = style.css()
        assert_that(css, contains_string(".synthesis img"), "Image constraints were not included in stylesheet")
