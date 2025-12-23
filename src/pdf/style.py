"""CSS stylesheet with Hokusai aesthetic."""
from __future__ import annotations

from abc import ABC
from abc import abstractmethod
from typing import Final

from src.pdf.palette import HokusaiPalette


class Styled(ABC):
    """Object with CSS style."""

    @abstractmethod
    def css(self) -> str:
        """Return CSS stylesheet."""
        ...


class HokusaiStyle(Styled):
    """Complete CSS stylesheet in Hokusai aesthetic."""

    def __init__(self, palette: HokusaiPalette) -> None:
        """Initialize with color palette."""
        self._palette: Final[HokusaiPalette] = palette

    def css(self) -> str:
        """Return complete CSS stylesheet."""
        return f"""@import url('https://fonts.googleapis.com/css2?family=Source+Serif+Pro:ital,wght@0,400;0,600;0,700;1,400&family=JetBrains+Mono:wght@400;500&display=swap');

:root {{
  --bg: {self._palette.bg()};
  --text: {self._palette.text()};
  --heading: {self._palette.heading()};
  --link: {self._palette.link()};
  --muted: {self._palette.muted()};
  --quote-bg: {self._palette.quotebg()};
  --accent: {self._palette.accent()};
  --code-bg: {self._palette.codebg()};
  --code-inline-bg: {self._palette.codeinlinebg()};
  --border: {self._palette.border()};
}}

* {{
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}}

@page {{
  size: A4;
  margin: 2cm 2cm 2.5cm 2cm;
  background: {self._palette.bg()};
  @bottom-center {{
    content: element(page-footer);
  }}
}}

@page intro {{
  margin: 0;
  background: var(--bg);
  @bottom-center {{
    content: none;
  }}
}}

.page-footer {{
  position: running(page-footer);
  font-family: "Source Serif Pro", Georgia, serif;
  font-size: 9pt;
  color: #666;
  text-align: center;
}}

.page-footer .author {{
  color: var(--accent);
  font-weight: 600;
}}

body {{
  font-family: "Source Serif Pro", Georgia, serif;
  font-size: 13px;
  background: var(--bg);
  color: var(--text);
  line-height: 1.45;
  min-height: 100vh;
}}

.container {{
  max-width: 700px;
  margin: 0 auto;
  padding: 0 2rem 3rem 2rem;
}}

.intro {{
  page: intro;
  page-break-after: always;
  break-after: always;
}}

.intro-content {{
  padding: 3rem 4rem;
  text-align: left;
}}

.cover-image {{
  width: 100%;
  height: auto;
  margin-top: 0;
  margin-bottom: 4rem;
  overflow: visible;
}}

.cover-image img {{
  width: 100%;
  height: auto;
  object-fit: contain;
  display: block;
}}

.wave-header {{
  width: 100%;
  height: 200px;
  margin-bottom: 2rem;
}}

.wave-header svg {{
  width: 100%;
  height: 100%;
}}

.intro h1 {{
  font-size: 2.8rem;
  font-weight: 700;
  color: var(--heading);
  text-align: left;
  line-height: 1.15;
  margin-bottom: 1.5rem;
  max-width: none;
}}

h1 {{
  font-size: 1.6rem;
  font-weight: 700;
  color: var(--heading);
  margin-bottom: 0.5rem;
}}

.meta {{
  border-top: 1px solid var(--border);
  padding-top: 1rem;
}}

.intro .subtitle {{
  text-align: left;
  color: var(--text);
  font-size: 0.9rem;
  margin-bottom: 0.4rem;
  opacity: 0.85;
}}

.intro .date {{
  text-align: left;
  color: var(--muted);
  font-size: 0.85rem;
  margin-bottom: 0;
  opacity: 0.7;
}}

.subtitle {{
  text-align: center;
  color: var(--text);
  font-size: 0.85rem;
  margin-bottom: 0.5rem;
  opacity: 0.8;
}}

.date {{
  text-align: center;
  color: var(--text);
  font-size: 0.85rem;
  margin-bottom: 2rem;
  opacity: 0.7;
}}

h2 {{
  font-size: 1.1rem;
  font-weight: 600;
  color: var(--heading);
  margin-top: 2rem;
  margin-bottom: 0.75rem;
  padding-bottom: 0.4rem;
  border-bottom: 2px solid var(--accent);
}}

h3 {{
  font-size: 1rem;
  font-weight: 600;
  color: var(--link);
  margin-top: 1.25rem;
  margin-bottom: 0.5rem;
}}

h4 {{
  font-size: 0.9rem;
  font-weight: 600;
  color: var(--muted);
  margin-top: 1rem;
  margin-bottom: 0.4rem;
}}

p {{
  margin-bottom: 1rem;
  text-align: justify;
}}

.synthesis {{
  border-left: 4px solid var(--link);
  padding: 1.25rem;
  margin: 1.5rem 0;
}}

.sources {{
  margin-top: 1.5rem;
}}

.source {{
  background: var(--quote-bg);
  padding: 1rem;
  margin-bottom: 0.75rem;
  border-radius: 6px;
  border: 1px solid var(--border);
  opacity: 0.9;
}}

.source-title {{
  font-weight: 600;
  color: var(--link);
  margin-bottom: 0.25rem;
}}

.source-url {{
  font-size: 0.8rem;
  color: var(--text);
  word-break: break-all;
  margin-bottom: 0.5rem;
}}

.source-url a {{
  color: var(--link);
  text-decoration: none;
}}

.source-excerpt {{
  font-size: 0.9rem;
  font-style: italic;
  color: var(--text);
  opacity: 0.85;
}}

.confidence-badge {{
  display: inline-block;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  margin-left: 1px;
  vertical-align: super;
}}

.confidence-high {{
  background: #7BA23F;  /* Moegi 萌黄 — spring shoots */
}}

.confidence-medium {{
  background: #C4A35A;  /* Kitsune-iro 狐色 — fox ochre */
}}

.confidence-low {{
  background: #D94537;  /* Shu-iro 朱色 — vermillion */
}}

.confidence-unknown {{
  background: #8A8F98;
}}

.divider {{
  height: 2px;
  background: linear-gradient(90deg, transparent 0%, var(--accent) 50%, transparent 100%);
  margin: 2rem 0;
}}

section:last-child .divider {{
  display: none;
}}

.footer {{
  margin-top: 3rem;
  text-align: center;
  font-size: 0.8rem;
  color: var(--text);
  opacity: 0.7;
}}

.author {{
  color: var(--accent);
  font-weight: 600;
}}

.subtitle .author {{
  color: var(--accent);
  font-weight: 600;
  opacity: 1;
}}

.disclaimer {{
  font-size: 0.75rem;
  opacity: 0.6;
  font-style: italic;
}}

.wave-footer {{
  width: 100%;
  height: 100px;
  margin-top: 2rem;
}}

.wave-footer svg {{
  width: 100%;
  height: 100%;
}}

.synthesis h1 {{
  font-size: 1.4rem;
  color: var(--heading);
  border-bottom: none;
  margin-top: 0;
}}

.synthesis h2 {{
  font-size: 1.1rem;
  margin-top: 1.25rem;
  border-bottom: 1px solid var(--border);
}}

.synthesis h3 {{
  font-size: 0.95rem;
  margin-top: 0.75rem;
}}

.synthesis ul, .synthesis ol {{
  margin: 0.75rem 0 0.75rem 1.5rem;
}}

.synthesis ul ul, .synthesis ol ol, .synthesis ul ol, .synthesis ol ul {{
  margin: 0.3rem 0 0.3rem 1.25rem;
}}

.synthesis li {{
  margin-bottom: 0.4rem;
}}

.synthesis li > ul, .synthesis li > ol {{
  margin-top: 0.3rem;
}}

.synthesis li::marker {{
  color: var(--link);
}}

.synthesis table {{
  width: 100%;
  table-layout: fixed;
  border-collapse: collapse;
  margin: 1rem 0;
  font-size: 0.75rem;
}}

.synthesis th {{
  background: var(--text);
  color: white;
  padding: 0.6rem;
  text-align: left;
  font-weight: 600;
  word-break: break-word;
  overflow-wrap: break-word;
}}

.synthesis td {{
  padding: 0.5rem 0.6rem;
  border: 1px solid var(--border);
  word-break: break-word;
  overflow-wrap: break-word;
}}

.synthesis tr:nth-child(odd) {{
  background: var(--bg);
}}

.synthesis tr:nth-child(even) {{
  background: #FFFEF7;
}}

.synthesis table.cols-5 {{
  font-size: 0.7rem;
}}

.synthesis table.cols-5 th,
.synthesis table.cols-5 td {{
  padding: 0.45rem;
}}

.synthesis table.cols-6 {{
  font-size: 0.65rem;
}}

.synthesis table.cols-6 th,
.synthesis table.cols-6 td {{
  padding: 0.4rem;
}}

.synthesis table.cols-7,
.synthesis table.cols-8,
.synthesis table.cols-9,
.synthesis table.cols-10 {{
  font-size: 0.55rem;
}}

.synthesis table.cols-7 th,
.synthesis table.cols-7 td,
.synthesis table.cols-8 th,
.synthesis table.cols-8 td,
.synthesis table.cols-9 th,
.synthesis table.cols-9 td,
.synthesis table.cols-10 th,
.synthesis table.cols-10 td {{
  padding: 0.35rem;
}}

.synthesis code {{
  background: var(--code-inline-bg);
  color: var(--accent);
  padding: 0.15rem 0.4rem;
  border-radius: 3px;
  font-family: "JetBrains Mono", monospace;
  font-size: 0.85em;
}}

.synthesis pre {{
  background: var(--code-bg);
  color: #E2E8F0;
  padding: 1rem;
  border-radius: 6px;
  overflow-x: auto;
  margin: 1rem 0;
  font-family: "JetBrains Mono", monospace;
  white-space: pre-wrap;
  overflow-wrap: break-word;
}}

.synthesis pre .code-line {{
  display: block;
  white-space: pre-wrap;
  overflow-wrap: break-word;
}}

.synthesis pre code {{
  background: transparent;
  padding: 0;
  color: inherit;
}}

.synthesis blockquote {{
  background: var(--quote-bg);
  border-left: 4px solid var(--link);
  margin: 1rem 0;
  padding: 1rem;
  font-style: italic;
  color: var(--text);
}}

.synthesis a {{
  color: var(--link);
  text-decoration: underline;
}}

.synthesis strong {{
  color: var(--accent);
}}

table {{
  page-break-inside: auto;
  break-inside: auto;
}}

tr {{
  page-break-inside: avoid;
  break-inside: avoid;
}}

thead {{
  display: table-header-group;
}}

h2, h3 {{
  page-break-after: avoid;
  break-after: avoid;
}}

pre {{
  page-break-inside: avoid;
  break-inside: avoid;
}}

.source {{
  page-break-inside: avoid;
  break-inside: avoid;
}}

@page brief {{
  @bottom-center {{
    content: none;
  }}
}}

.brief {{
  page: brief;
  page-break-after: always;
  break-after: always;
}}

.brief .container {{
  padding-top: 2rem;
}}

.brief h2 {{
  font-size: 1.3rem;
  font-weight: 700;
  color: var(--heading);
  margin-bottom: 1.5rem;
  border-bottom: none;
}}

.brief .language {{
  display: inline-block;
  background: var(--quote-bg);
  color: var(--text);
  padding: 0.3rem 0.8rem;
  border-radius: 4px;
  font-size: 0.85rem;
  font-weight: 600;
  margin-bottom: 1.5rem;
}}

.brief .query {{
  background: rgba(189, 224, 254, 0.15);
  border-left: 4px solid var(--link);
  padding: 1.5rem;
  border-radius: 0 6px 6px 0;
}}

.brief .query p {{
  margin-bottom: 0.75rem;
  text-align: left;
}}

.brief .query p:last-child {{
  margin-bottom: 0;
}}

.brief .query ol, .brief .query ul {{
  margin: 0.75rem 0 0.75rem 1.5rem;
}}

.brief .query li {{
  margin-bottom: 0.4rem;
}}

.brief .query strong {{
  color: var(--link);
}}

a.cite {{
  color: var(--link);
  text-decoration: none;
  font-weight: 600;
  font-size: 0.85em;
}}

.references {{
  margin-top: 3rem;
  padding-top: 2rem;
  border-top: 2px solid var(--accent);
}}

.references h2 {{
  font-size: 1.2rem;
  color: var(--heading);
  margin-bottom: 1.5rem;
  border-bottom: none;
}}

.ref-list {{
  list-style-type: decimal;
  padding-left: 1.5rem;
  margin: 0;
}}

.ref-item {{
  margin-bottom: 0.5rem;
}}

.ref-link {{
  color: var(--link);
  text-decoration: none;
  font-size: 0.9rem;
}}

.ref-link:hover {{
  text-decoration: underline;
}}

hr {{
  border: none;
  border-top: 1px solid var(--border);
  margin: 2rem 0;
}}"""
