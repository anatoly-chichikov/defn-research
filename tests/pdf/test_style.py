"""Tests for HokusaiStyle."""
from __future__ import annotations

import random

from hamcrest import assert_that
from hamcrest import contains_string
from hamcrest import not_

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


class Test_h1_heading_omits_the_underline:
    """HokusaiStyle h1 heading omits underline."""

    def test(self) -> None:
        """HokusaiStyle h1 heading omits underline."""
        seed = sum(ord(c) for c in __name__) + 19
        maker = random.Random(seed)
        value = "".join(chr(maker.randrange(0x0400, 0x04ff)) for _ in range(6))
        palette = Palette(value)
        style = HokusaiStyle(palette)
        css = style.css()
        snippet = "\n".join(
            (
                "h1 {",
                "  font-size: 2.05rem;",
                "  font-weight: 700;",
                "  color: var(--heading);",
                "  line-height: 1.15;",
                "  margin: 0 0 0.7em;",
                "  border-bottom: 1px solid var(--border);",
            )
        )
        assert_that(css, not_(contains_string(snippet)), "H1 underline was present")


class Test_h2_heading_omits_the_underline:
    """HokusaiStyle h2 heading omits underline."""

    def test(self) -> None:
        """HokusaiStyle h2 heading omits underline."""
        seed = sum(ord(c) for c in __name__) + 21
        maker = random.Random(seed)
        value = "".join(chr(maker.randrange(0x0400, 0x04ff)) for _ in range(6))
        palette = Palette(value)
        style = HokusaiStyle(palette)
        css = style.css()
        snippet = "\n".join(
            (
                "h2 {",
                "  font-size: 1.55rem;",
                "  font-weight: 600;",
                "  color: var(--heading);",
                "  line-height: 1.2;",
                "  margin: 2.2em 0 0.6em;",
                "  position: relative;",
                "  border-bottom: 1px solid var(--border);",
            )
        )
        assert_that(css, not_(contains_string(snippet)), "H2 underline was present")


class Test_h2_heading_draws_accent_bar:
    """HokusaiStyle h2 heading draws accent bar."""

    def test(self) -> None:
        """HokusaiStyle h2 heading draws accent bar."""
        seed = sum(ord(c) for c in __name__) + 25
        maker = random.Random(seed)
        value = "".join(chr(maker.randrange(0x0400, 0x04ff)) for _ in range(6))
        palette = Palette(value)
        style = HokusaiStyle(palette)
        css = style.css()
        snippet = "\n".join(
            (
                "h2::before {",
                "  content: \"\";",
                "  position: absolute;",
                "  left: -1.4rem;",
                "  top: 0;",
                "  bottom: 0;",
                "  width: 4px;",
                "  background: linear-gradient(to bottom, var(--accent) 0%, var(--accent) 82%, transparent 100%);",
                "  border-radius: 0 0 4px 0;",
                "}",
            )
        )
        assert_that(css, contains_string(snippet), "Heading accent bar was missing")


class Test_synthesis_block_omits_left_border:
    """HokusaiStyle synthesis block omits left border."""

    def test(self) -> None:
        """HokusaiStyle synthesis block omits left border."""
        seed = sum(ord(c) for c in __name__) + 29
        maker = random.Random(seed)
        value = "".join(chr(maker.randrange(0x0400, 0x04ff)) for _ in range(6))
        palette = Palette(value)
        style = HokusaiStyle(palette)
        css = style.css()
        assert_that(css, contains_string("border-left: none;"), "Synthesis left border was present")


class Test_hr_hides_divider_line:
    """HokusaiStyle hides horizontal rule line."""

    def test(self) -> None:
        """HokusaiStyle hides horizontal rule line."""
        seed = sum(ord(c) for c in __name__) + 23
        maker = random.Random(seed)
        value = "".join(chr(maker.randrange(0x0400, 0x04ff)) for _ in range(6))
        palette = Palette(value)
        style = HokusaiStyle(palette)
        css = style.css()
        assert_that(css, contains_string("border-top: 0;"), "Horizontal rule line was visible")


class Test_brief_query_uses_quote_colors:
    """HokusaiStyle brief query uses quote colors."""

    def test(self) -> None:
        """HokusaiStyle brief query uses quote colors."""
        seed = sum(ord(c) for c in __name__) + 27
        maker = random.Random(seed)
        value = "".join(chr(maker.randrange(0x0400, 0x04ff)) for _ in range(6))
        palette = Palette(value)
        style = HokusaiStyle(palette)
        css = style.css()
        assert_that(css, contains_string("background: var(--quote-bg);"), "Brief query background was not quote color")


class Test_brief_query_uses_link_tone:
    """HokusaiStyle brief query uses link tone."""

    def test(self) -> None:
        """HokusaiStyle brief query uses link tone."""
        seed = sum(ord(c) for c in __name__) + 35
        maker = random.Random(seed)
        value = "".join(chr(maker.randrange(0x0400, 0x04ff)) for _ in range(6))
        palette = Palette(value)
        style = HokusaiStyle(palette)
        css = style.css()
        assert_that(css, contains_string("border-left: 3px solid var(--link);"), "Brief query border did not use link color")


class Test_blockquote_uses_link_tone:
    """HokusaiStyle blockquote uses link tone."""

    def test(self) -> None:
        """HokusaiStyle blockquote uses link tone."""
        seed = sum(ord(c) for c in __name__) + 33
        maker = random.Random(seed)
        value = "".join(chr(maker.randrange(0x0400, 0x04ff)) for _ in range(6))
        palette = Palette(value)
        style = HokusaiStyle(palette)
        css = style.css()
        assert_that(css, contains_string("border-left: 3px solid var(--link);"), "Blockquote border did not use link color")


class Test_serif_font_includes_japanese_fallback:
    """HokusaiStyle serif font includes Japanese fallback."""

    def test(self) -> None:
        """HokusaiStyle serif font includes Japanese fallback."""
        seed = sum(ord(c) for c in __name__) + 31
        maker = random.Random(seed)
        value = "".join(chr(maker.randrange(0x0400, 0x04ff)) for _ in range(6))
        palette = Palette(value)
        style = HokusaiStyle(palette)
        css = style.css()
        assert_that(css, contains_string("Noto Serif JP"), "Japanese fallback font was missing")
