"""Tests for HokusaiPalette."""
from __future__ import annotations

from hamcrest import assert_that
from hamcrest import has_length
from hamcrest import starts_with

from src.pdf.palette import HokusaiPalette


class TestHokusaiPaletteIndigoReturnsHexColor:
    """HokusaiPalette indigo returns valid hex color."""

    def test(self) -> None:
        palette = HokusaiPalette()
        assert_that(
            palette.indigo(),
            starts_with("#"),
            "Indigo color did not start with hash",
        )


class TestHokusaiPaletteIndigoReturnsSevenCharacters:
    """HokusaiPalette indigo returns seven character hex."""

    def test(self) -> None:
        palette = HokusaiPalette()
        assert_that(
            palette.indigo(),
            has_length(7),
            "Indigo color was not seven characters",
        )


class TestHokusaiPaletteWaveReturnsHexColor:
    """HokusaiPalette wave returns valid hex color."""

    def test(self) -> None:
        palette = HokusaiPalette()
        assert_that(
            palette.wave(),
            starts_with("#"),
            "Wave color did not start with hash",
        )


class TestHokusaiPaletteFoamReturnsHexColor:
    """HokusaiPalette foam returns valid hex color."""

    def test(self) -> None:
        palette = HokusaiPalette()
        assert_that(
            palette.foam(),
            starts_with("#"),
            "Foam color did not start with hash",
        )


class TestHokusaiPaletteAccentReturnsHexColor:
    """HokusaiPalette accent returns valid hex color."""

    def test(self) -> None:
        palette = HokusaiPalette()
        assert_that(
            palette.accent(),
            starts_with("#"),
            "Accent color did not start with hash",
        )


class TestHokusaiPaletteInkReturnsHexColor:
    """HokusaiPalette ink returns valid hex color."""

    def test(self) -> None:
        palette = HokusaiPalette()
        assert_that(
            palette.ink(),
            starts_with("#"),
            "Ink color did not start with hash",
        )
