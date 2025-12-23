"""PDF document generator for research results."""
from __future__ import annotations

import os
import re
from abc import ABC
from abc import abstractmethod
from pathlib import Path
from typing import Final
from urllib.parse import urlparse

import markdown
from weasyprint import HTML

from src.domain.session import ResearchSession
from src.pdf.palette import HokusaiPalette
from src.pdf.style import HokusaiStyle
from src.pdf.wave import WaveFooter
from src.pdf.wave import WavePattern


class Signed(ABC):
    """Object with author signature."""

    @abstractmethod
    def html(self) -> str:
        """Return signature as HTML with styling."""
        ...


class Signature(Signed):
    """Author signature for research documents."""

    def __init__(self, name: str, service: str) -> None:
        """Initialize with author name and service."""
        self._name: Final[str] = name
        self._service: Final[str] = service

    def html(self) -> str:
        """Return signature as HTML with author highlighted."""
        return f"{self._label()}<br>May contain inaccuracies, please verify"

    def _label(self) -> str:
        """Return attribution label with optional author."""
        if self._name:
            return f'AI generated report for <span class="author">{self._name}</span> with {self._service}'
        return f"AI generated report with {self._service}"


class Exportable(ABC):
    """Object that can export to file."""

    @abstractmethod
    def save(self, path: Path) -> Path:
        """Save to file and return path."""
        ...


class ResearchDocument(Exportable):
    """PDF document with Hokusai-style design."""

    def __init__(
        self,
        session: ResearchSession,
        palette: HokusaiPalette,
        cover: Path | None = None,
    ) -> None:
        """Initialize with session, palette and optional cover image."""
        self._session: Final[ResearchSession] = session
        self._palette: Final[HokusaiPalette] = palette
        self._cover: Final[Path | None] = cover
        self._style: Final[HokusaiStyle] = HokusaiStyle(palette)
        self._wave: Final[WavePattern] = WavePattern(palette)
        self._wavefooter: Final[WaveFooter] = WaveFooter(palette)

    def render(self) -> str:
        """Return complete HTML document."""
        content, urls = self._tasks()
        signature = Signature(self._author(), self._service())
        html = signature.html()
        return f"""<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <title>{self._escape(self._session.topic())}</title>
  <style>{self._style.css()}</style>
</head>
<body>
  <div class="page-footer">{html}</div>
  <div class="intro">
    {self._coverimage()}
    <div class="intro-content">
      <h1>{self._escape(self._session.topic())}</h1>
      <div class="meta">
        <p class="subtitle">{html}</p>
        <p class="date">{self._session.created().strftime("%Y-%m-%d")}</p>
      </div>
    </div>
  </div>
  {self._brief()}
  <div class="container content">
    {content}
  </div>
  <div class="container"></div>
</body>
</html>"""

    def save(self, path: Path) -> Path:
        """Generate and save PDF document."""
        path.parent.mkdir(parents=True, exist_ok=True)
        HTML(string=self.render()).write_pdf(path)
        return path

    def html(self, path: Path) -> Path:
        """Save HTML version for preview."""
        path.parent.mkdir(parents=True, exist_ok=True)
        with path.open("w", encoding="utf-8") as handle:
            handle.write(self.render())
        return path

    def _coverimage(self) -> str:
        """Render cover image if available."""
        if not self._cover or not self._cover.exists():
            return ""
        return f'<div class="cover-image"><img src="file://{self._cover.absolute()}" alt="Cover"></div>'

    def _author(self) -> str:
        """Return report author name from environment."""
        name = os.getenv("REPORT_FOR") or ""
        return name

    def _service(self) -> str:
        """Return service name from latest task or fallback."""
        tasks = self._session.tasks()
        if tasks:
            task = tasks[-1]
            return task.service()
        return "parallel.ai"

    def _brief(self) -> str:
        """Render brief page from file or query fallback."""
        tasks = self._session.tasks()
        if not tasks:
            return ""
        task = tasks[0]
        path = self._path()
        if path.exists():
            content = path.read_text(encoding="utf-8")
        else:
            content = task.query()
        content = self._normalize(content)
        html = markdown.markdown(
            content,
            extensions=["tables", "fenced_code"],
        )
        html = self._tables(html)
        html = self._codeindent(html)
        return f"""<div class="brief">
  <div class="container">
    <h2>Introduction</h2>
    <div class="query">{html}</div>
  </div>
</div>"""

    def _path(self) -> Path:
        """Return path to brief markdown file."""
        short = self._session.id().split("-")[0]
        return Path("data/briefs") / f"{short}.md"

    def _tasks(self) -> tuple[str, list[str]]:
        """Render all tasks as HTML sections and collect URLs."""
        sections: list[str] = []
        urls: list[str] = []
        for task in self._session.tasks():
            section, extracted = self._task(task)
            sections.append(section)
            for url in extracted:
                if url not in urls:
                    urls.append(url)
        return "\n".join(sections), urls

    def _task(self, task) -> tuple[str, list[str]]:
        """Render single task as HTML section."""
        result = task.result()
        synthesis = ""
        urls: list[str] = []
        if result:
            text = result.summary()
            text, urls = self._citations(text, result.sources())
            text = self._strip(text)
            text = self._nested(text)
            text = self._normalize(text)
            html = markdown.markdown(text, extensions=["tables", "fenced_code"])
            html = self._tables(html)
            html = self._codeindent(html)
            synthesis = f'<div class="synthesis">{html}</div>'
        section = f"""<section>
  {synthesis}
  <div class="divider"></div>
</section>"""
        return section, urls

    def _badge(self, confidence: str | None) -> str:
        """Render inline confidence dot or empty string if not available."""
        if not confidence:
            return ""
        level = confidence.lower()
        return f'<span class="confidence-badge confidence-{level}"></span>'

    def _escape(self, text: str) -> str:
        """Escape HTML special characters."""
        return (
            text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace('"', "&quot;")
        )

    def _citations(self, text: str, sources: tuple = ()) -> tuple[str, list[str]]:
        """Convert [N] references to clickable links with confidence badges."""
        refs = self._references(text)
        confidence_map = {s.url(): s.confidence() for s in sources}
        urls: list[str] = []
        def replace(match: re.Match) -> str:
            num = int(match.group(1))
            url = refs.get(num)
            if url:
                if url not in urls:
                    urls.append(url)
                badge = self._badge(confidence_map.get(url))
                return f'<a href="{url}" class="cite" target="_blank">[{num}]</a>{badge}'
            return match.group(0)
        processed = re.sub(r'\[(\d+)\]', replace, text)
        return processed, urls

    def _references(self, text: str) -> dict[int, str]:
        """Extract reference URLs from References section."""
        match = re.search(r'##\s*References\s*\n(.*?)(?=\n##|\Z)', text, re.DOTALL | re.IGNORECASE)
        if not match:
            return {}
        refs: dict[int, str] = {}
        for line in match.group(1).split('\n'):
            m = re.match(r'(\d+)\.\s+.*?(https?://\S+)', line)
            if m:
                refs[int(m.group(1))] = m.group(2)
        return refs

    def _strip(self, text: str) -> str:
        """Remove References section and trailing boilerplate from markdown."""
        text = re.sub(r'\n##\s*References\s*\n.*', '', text, flags=re.DOTALL | re.IGNORECASE)
        text = re.sub(r'\n---\n\*Prepared using.*?\*', '', text, flags=re.DOTALL)
        return text

    def _normalize(self, text: str) -> str:
        """Add blank lines before list markers that follow text directly."""
        return re.sub(r'([^\n])\n((?:\* |\d+\. ))', r'\1\n\n\2', text)

    def _nested(self, text: str) -> str:
        """Convert 1-3 space indents to 4 spaces for proper markdown nesting."""
        return re.sub(r'^( {1,3})([*+-] )', r'    \2', text, flags=re.MULTILINE)

    def _tables(self, html: str) -> str:
        """Add column count classes to tables for responsive font sizing."""
        def classify(match: re.Match) -> str:
            table = match.group(0)
            head = table.split('</thead>')[0] if '</thead>' in table else table.split('</tr>')[0]
            cols = len(re.findall(r'<th[^>]*>', head)) or len(re.findall(r'<td[^>]*>', head))
            return table.replace('<table>', f'<table class="cols-{cols}">', 1)
        return re.sub(r'<table>.*?</table>', classify, html, flags=re.DOTALL)

    def _codeindent(self, html: str) -> str:
        """Wrap code lines in spans with hanging indent for wrapped lines."""
        def process(match: re.Match) -> str:
            content = match.group(1)
            lines = content.split('\n')
            wrapped = []
            for line in lines:
                if not line:
                    wrapped.append(line)
                    continue
                indent = len(line) - len(line.lstrip(' '))
                hang = 2
                padding = indent + hang
                style = (
                    f"padding-left: {padding}ch; "
                    f"text-indent: -{hang}ch; "
                    "display: block;"
                )
                stripped = line.lstrip(' ')
                wrapped.append(f'<span class="code-line" style="{style}">{stripped}</span>')
            return f'<pre><code>{"".join(wrapped)}</code></pre>'
        return re.sub(r'<pre><code>(.*?)</code></pre>', process, html, flags=re.DOTALL)
