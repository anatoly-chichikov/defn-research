"""Output folder organizer for research artifacts."""
from __future__ import annotations

import json
import re
from abc import ABC
from abc import abstractmethod
from datetime import datetime
from pathlib import Path
from typing import Final


class Organized(ABC):
    """Object that organizes output files."""

    @abstractmethod
    def folder(self, name: str) -> Path:
        """Return output folder path for session."""
        ...


class OutputOrganizer(Organized):
    """Manages per-session output folders with all artifacts."""

    def __init__(self, root: Path) -> None:
        """Initialize with output root directory."""
        self._root: Final[Path] = root

    def name(self, created: datetime, topic: str, identifier: str) -> str:
        """Build folder name from date, topic slug, and short ID."""
        date = created.strftime("%Y-%m-%d")
        slug = self._slug(topic)
        short = identifier[:8]
        return f"{date}_{slug}_{short}"

    def folder(self, name: str) -> Path:
        """Return output folder path, creating if needed."""
        path = self._root / name
        path.mkdir(parents=True, exist_ok=True)
        return path

    def _slug(self, text: str) -> str:
        """Convert text to filename-safe slug."""
        transliterated = self._transliterate(text)
        lower = transliterated.lower()
        cleaned = re.sub(r"[^a-z0-9\s-]", "", lower)
        normalized = re.sub(r"[\s]+", "-", cleaned)
        return normalized[:40] if normalized else "untitled"

    def _transliterate(self, text: str) -> str:
        """Transliterate Cyrillic to Latin."""
        mapping = {
            'а': 'a', 'б': 'b', 'в': 'v', 'г': 'g', 'д': 'd',
            'е': 'e', 'ё': 'yo', 'ж': 'zh', 'з': 'z', 'и': 'i',
            'й': 'y', 'к': 'k', 'л': 'l', 'м': 'm', 'н': 'n',
            'о': 'o', 'п': 'p', 'р': 'r', 'с': 's', 'т': 't',
            'у': 'u', 'ф': 'f', 'х': 'h', 'ц': 'ts', 'ч': 'ch',
            'ш': 'sh', 'щ': 'sch', 'ъ': '', 'ы': 'y', 'ь': '',
            'э': 'e', 'ю': 'yu', 'я': 'ya'
        }
        result = []
        for char in text:
            lower = char.lower()
            if lower in mapping:
                mapped = mapping[lower]
                result.append(mapped.upper() if char.isupper() else mapped)
            else:
                result.append(char)
        return "".join(result)

    def response(self, name: str, data: dict) -> Path:
        """Save raw API response as JSON."""
        folder = self.folder(name)
        path = folder / "response.json"
        with path.open("w", encoding="utf-8") as handle:
            json.dump(data, handle, indent=2, ensure_ascii=False)
        return path

    def cover(self, name: str) -> Path:
        """Return path for cover image."""
        return self.folder(name) / "cover.jpg"

    def report(self, name: str) -> Path:
        """Return path for PDF report."""
        prefix = name.rsplit("_", 1)[0]
        return self.folder(name) / f"{prefix}.pdf"

    def html(self, name: str) -> Path:
        """Return path for HTML preview."""
        return self.folder(name) / "report.html"

    def brief(self, name: str, content: str) -> Path:
        """Save brief markdown to output folder."""
        folder = self.folder(name)
        path = folder / "brief.md"
        with path.open("w", encoding="utf-8") as handle:
            handle.write(content)
        return path

    def existing(self, name: str) -> Path | None:
        """Return cover path if exists, None otherwise."""
        path = self.cover(name)
        if path.exists():
            return path
        return None
