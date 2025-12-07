"""Tests for JsonFile storage."""
from __future__ import annotations

import tempfile
import uuid
from pathlib import Path

from hamcrest import assert_that
from hamcrest import calling
from hamcrest import equal_to
from hamcrest import is_
from hamcrest import raises

from src.storage.file import JsonFile


class TestJsonFileWritesAndReadsData:
    """JsonFile writes data that can be read back."""

    def test(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / f"test-{uuid.uuid4()}.json"
            file = JsonFile(path)
            key = f"key-{uuid.uuid4()}"
            file.write({key: "value"})
            assert_that(
                file.read()[key],
                equal_to("value"),
                "Read data did not match written data",
            )


class TestJsonFileRaisesOnMissingFile:
    """JsonFile raises FileNotFoundError for missing file."""

    def test(self) -> None:
        path = Path(f"/nonexistent-{uuid.uuid4()}.json")
        file = JsonFile(path)
        assert_that(
            calling(file.read),
            raises(FileNotFoundError),
            "Reading missing file did not raise FileNotFoundError",
        )


class TestJsonFileExistsReturnsFalseForMissing:
    """JsonFile exists returns False for missing file."""

    def test(self) -> None:
        path = Path(f"/nonexistent-{uuid.uuid4()}.json")
        file = JsonFile(path)
        assert_that(
            file.exists(),
            is_(False),
            "Exists returned True for missing file",
        )


class TestJsonFileExistsReturnsTrueForExisting:
    """JsonFile exists returns True for existing file."""

    def test(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / f"test-{uuid.uuid4()}.json"
            file = JsonFile(path)
            file.write({"test": True})
            assert_that(
                file.exists(),
                is_(True),
                "Exists returned False for existing file",
            )


class TestJsonFileCreatesParentDirectories:
    """JsonFile creates parent directories on write."""

    def test(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            nested = f"a-{uuid.uuid4()}/b-{uuid.uuid4()}"
            path = Path(tmp) / nested / "test.json"
            file = JsonFile(path)
            file.write({"nested": True})
            assert_that(
                file.exists(),
                is_(True),
                "File was not created in nested directory",
            )


class TestJsonFileHandlesUnicode:
    """JsonFile handles unicode content correctly."""

    def test(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / f"unicode-{uuid.uuid4()}.json"
            file = JsonFile(path)
            text = f"日本語テスト-{uuid.uuid4()}"
            file.write({"text": text})
            assert_that(
                file.read()["text"],
                equal_to(text),
                "Unicode content was corrupted",
            )
