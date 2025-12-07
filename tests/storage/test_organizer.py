"""Tests for OutputOrganizer storage."""
from __future__ import annotations

import json
import tempfile
import uuid
from datetime import datetime
from pathlib import Path

from hamcrest import assert_that
from hamcrest import contains_string
from hamcrest import equal_to
from hamcrest import is_
from hamcrest import is_not
from hamcrest import matches_regexp
from hamcrest import none

from src.storage.organizer import OutputOrganizer


class TestOrganizerCreatesFolderForSession:
    """OutputOrganizer creates folder for session identifier."""

    def test(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            identifier = str(uuid.uuid4())
            organizer = OutputOrganizer(Path(tmp))
            folder = organizer.folder(identifier)
            assert_that(
                folder.exists(),
                is_(True),
                "Folder was not created for session",
            )


class TestOrganizerFolderContainsIdentifier:
    """OutputOrganizer folder path contains session identifier."""

    def test(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            identifier = str(uuid.uuid4())
            organizer = OutputOrganizer(Path(tmp))
            folder = organizer.folder(identifier)
            assert_that(
                str(folder),
                contains_string(identifier),
                "Folder path did not contain session identifier",
            )


class TestOrganizerSavesResponseAsJson:
    """OutputOrganizer saves response data as JSON file."""

    def test(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            identifier = str(uuid.uuid4())
            organizer = OutputOrganizer(Path(tmp))
            key = f"test-{uuid.uuid4()}"
            path = organizer.response(identifier, {key: "données"})
            with path.open("r", encoding="utf-8") as handle:
                data = json.load(handle)
            assert_that(
                data[key],
                equal_to("données"),
                "Response JSON did not contain expected data",
            )


class TestOrganizerResponseCreatesFolder:
    """OutputOrganizer response method creates folder if missing."""

    def test(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            identifier = str(uuid.uuid4())
            organizer = OutputOrganizer(Path(tmp))
            path = organizer.response(identifier, {"created": True})
            assert_that(
                path.parent.exists(),
                is_(True),
                "Response did not create parent folder",
            )


class TestOrganizerCoverReturnsJpgPath:
    """OutputOrganizer cover returns path ending with jpg."""

    def test(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            identifier = str(uuid.uuid4())
            organizer = OutputOrganizer(Path(tmp))
            path = organizer.cover(identifier)
            assert_that(
                path.suffix,
                equal_to(".jpg"),
                "Cover path did not have jpg extension",
            )


class TestOrganizerReportReturnsPdfPath:
    """OutputOrganizer report returns path ending with pdf."""

    def test(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            identifier = str(uuid.uuid4())
            organizer = OutputOrganizer(Path(tmp))
            path = organizer.report(identifier)
            assert_that(
                path.suffix,
                equal_to(".pdf"),
                "Report path did not have pdf extension",
            )


class TestOrganizerHtmlReturnsHtmlPath:
    """OutputOrganizer html returns path ending with html."""

    def test(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            identifier = str(uuid.uuid4())
            organizer = OutputOrganizer(Path(tmp))
            path = organizer.html(identifier)
            assert_that(
                path.suffix,
                equal_to(".html"),
                "HTML path did not have html extension",
            )


class TestOrganizerExistingReturnsNoneForMissing:
    """OutputOrganizer existing returns None when cover missing."""

    def test(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            identifier = str(uuid.uuid4())
            organizer = OutputOrganizer(Path(tmp))
            result = organizer.existing(identifier)
            assert_that(
                result,
                is_(none()),
                "Existing returned path for missing cover",
            )


class TestOrganizerExistingReturnsPathWhenExists:
    """OutputOrganizer existing returns path when cover exists."""

    def test(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            identifier = str(uuid.uuid4())
            organizer = OutputOrganizer(Path(tmp))
            cover = organizer.cover(identifier)
            cover.parent.mkdir(parents=True, exist_ok=True)
            cover.write_text("fake image")
            result = organizer.existing(identifier)
            assert_that(
                result,
                is_not(none()),
                "Existing returned None for existing cover",
            )


class TestOrganizerSavesBriefAsMarkdown:
    """OutputOrganizer saves brief content as markdown file."""

    def test(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            identifier = str(uuid.uuid4())
            organizer = OutputOrganizer(Path(tmp))
            content = f"# Test Brief\n\nСодержимое {uuid.uuid4()}"
            path = organizer.brief(identifier, content)
            assert_that(
                path.read_text(encoding="utf-8"),
                equal_to(content),
                "Brief content was not saved correctly",
            )


class TestOrganizerNameFormatsCorrectly:
    """OutputOrganizer name builds date_slug_shortid format."""

    def test(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            organizer = OutputOrganizer(Path(tmp))
            created = datetime(2025, 12, 7, 15, 30)
            topic = "Coffee vs Tea"
            identifier = "589a125c-8ae7-4c28-ac95-7c1127b601d3"
            name = organizer.name(created, topic, identifier)
            assert_that(
                name,
                equal_to("2025-12-07_coffee-vs-tea_589a125c"),
                "Name format did not match expected pattern",
            )


class TestOrganizerNameHandlesSpecialCharacters:
    """OutputOrganizer name strips special characters from topic."""

    def test(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            organizer = OutputOrganizer(Path(tmp))
            created = datetime(2025, 1, 15, 10, 0)
            topic = "What's the deal with: émojis & symbols?"
            identifier = str(uuid.uuid4())
            name = organizer.name(created, topic, identifier)
            assert_that(
                name,
                matches_regexp(r"^2025-01-15_[a-z0-9-]+_[a-f0-9]{8}$"),
                "Name contained invalid characters",
            )
