"""Tests for WavePattern."""
from __future__ import annotations

from hamcrest import assert_that
from hamcrest import contains_string

from src.pdf.palette import HokusaiPalette
from src.pdf.wave import WaveFooter
from src.pdf.wave import WavePattern


class TestWavePatternRenderReturnsSvg:
    """WavePattern render returns SVG element."""

    def test(self) -> None:
        palette = HokusaiPalette()
        wave = WavePattern(palette)
        assert_that(
            wave.render(),
            contains_string("<svg"),
            "Rendered wave did not contain svg tag",
        )


class TestWavePatternRenderContainsPath:
    """WavePattern render contains path elements."""

    def test(self) -> None:
        palette = HokusaiPalette()
        wave = WavePattern(palette)
        assert_that(
            wave.render(),
            contains_string("<path"),
            "Rendered wave did not contain path tag",
        )


class TestWavePatternRenderContainsPaletteColor:
    """WavePattern render uses palette wave color."""

    def test(self) -> None:
        palette = HokusaiPalette()
        wave = WavePattern(palette)
        assert_that(
            wave.render(),
            contains_string(palette.wave()),
            "Rendered wave did not contain palette wave color",
        )


class TestWavePatternRenderContainsGradient:
    """WavePattern render contains gradient definition."""

    def test(self) -> None:
        palette = HokusaiPalette()
        wave = WavePattern(palette)
        assert_that(
            wave.render(),
            contains_string("linearGradient"),
            "Rendered wave did not contain gradient",
        )


class TestWaveFooterRenderReturnsSvg:
    """WaveFooter render returns SVG element."""

    def test(self) -> None:
        palette = HokusaiPalette()
        footer = WaveFooter(palette)
        assert_that(
            footer.render(),
            contains_string("<svg"),
            "Rendered footer did not contain svg tag",
        )


class TestWaveFooterRenderContainsPath:
    """WaveFooter render contains path elements."""

    def test(self) -> None:
        palette = HokusaiPalette()
        footer = WaveFooter(palette)
        assert_that(
            footer.render(),
            contains_string("<path"),
            "Rendered footer did not contain path tag",
        )
