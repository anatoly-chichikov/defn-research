"""JSON file storage."""
from __future__ import annotations

import json
from abc import ABC
from abc import abstractmethod
from pathlib import Path
from typing import Final


class Readable(ABC):
    """Object that can be read."""

    @abstractmethod
    def read(self) -> dict:
        """Return content as dictionary."""
        ...


class Writable(ABC):
    """Object that can be written."""

    @abstractmethod
    def write(self, data: dict) -> None:
        """Persist data."""
        ...


class JsonFile(Readable, Writable):
    """Immutable JSON file accessor."""

    def __init__(self, path: Path) -> None:
        """Initialize with file path."""
        self._path: Final[Path] = path

    def read(self) -> dict:
        """Return file content as dictionary."""
        if not self._path.exists():
            raise FileNotFoundError(f"File not found at {self._path}")
        with self._path.open("r", encoding="utf-8") as handle:
            return json.load(handle)

    def write(self, data: dict) -> None:
        """Persist dictionary to file."""
        self._path.parent.mkdir(parents=True, exist_ok=True)
        with self._path.open("w", encoding="utf-8") as handle:
            json.dump(data, handle, indent=2, ensure_ascii=False)

    def path(self) -> Path:
        """Return file path."""
        return self._path

    def exists(self) -> bool:
        """Return True if file exists."""
        return self._path.exists()
