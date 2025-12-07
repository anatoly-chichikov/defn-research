"""Tests for HokusaiPalette."""
from __future__ import annotations

from hamcrest import assert_that
from hamcrest import has_length
from hamcrest import starts_with

from src.pdf.palette import HokusaiPalette


class TestHokusaiPaletteBgReturnsHexColor:
    """HokusaiPalette bg returns valid hex color."""

    def test(self) -> None:
        palette = HokusaiPalette()
        assert_that(
            palette.bg(),
            starts_with("#"),
            "Bg color did not start with hash",
        )


class TestHokusaiPaletteBgReturnsSevenCharacters:
    """HokusaiPalette bg returns seven character hex."""

    def test(self) -> None:
        palette = HokusaiPalette()
        assert_that(
            palette.bg(),
            has_length(7),
            "Bg color was not seven characters",
        )


class TestHokusaiPaletteTextReturnsHexColor:
    """HokusaiPalette text returns valid hex color."""

    def test(self) -> None:
        palette = HokusaiPalette()
        assert_that(
            palette.text(),
            starts_with("#"),
            "Text color did not start with hash",
        )


class TestHokusaiPaletteHeadingReturnsHexColor:
    """HokusaiPalette heading returns valid hex color."""

    def test(self) -> None:
        palette = HokusaiPalette()
        assert_that(
            palette.heading(),
            starts_with("#"),
            "Heading color did not start with hash",
        )


class TestHokusaiPaletteLinkReturnsHexColor:
    """HokusaiPalette link returns valid hex color."""

    def test(self) -> None:
        palette = HokusaiPalette()
        assert_that(
            palette.link(),
            starts_with("#"),
            "Link color did not start with hash",
        )


class TestHokusaiPaletteMutedReturnsHexColor:
    """HokusaiPalette muted returns valid hex color."""

    def test(self) -> None:
        palette = HokusaiPalette()
        assert_that(
            palette.muted(),
            starts_with("#"),
            "Muted color did not start with hash",
        )


class TestHokusaiPaletteQuotebgReturnsHexColor:
    """HokusaiPalette quotebg returns valid hex color."""

    def test(self) -> None:
        palette = HokusaiPalette()
        assert_that(
            palette.quotebg(),
            starts_with("#"),
            "Quotebg color did not start with hash",
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


class TestHokusaiPaletteCodebgReturnsHexColor:
    """HokusaiPalette codebg returns valid hex color."""

    def test(self) -> None:
        palette = HokusaiPalette()
        assert_that(
            palette.codebg(),
            starts_with("#"),
            "Codebg color did not start with hash",
        )


class TestHokusaiPaletteCodeinlinebgReturnsHexColor:
    """HokusaiPalette codeinlinebg returns valid hex color."""

    def test(self) -> None:
        palette = HokusaiPalette()
        assert_that(
            palette.codeinlinebg(),
            starts_with("#"),
            "Codeinlinebg color did not start with hash",
        )


class TestHokusaiPaletteBorderReturnsHexColor:
    """HokusaiPalette border returns valid hex color."""

    def test(self) -> None:
        palette = HokusaiPalette()
        assert_that(
            palette.border(),
            starts_with("#"),
            "Border color did not start with hash",
        )
