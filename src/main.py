"""CLI entry point for research workflow."""
from __future__ import annotations

import os

os.environ.setdefault("DYLD_LIBRARY_PATH", "/opt/homebrew/lib")

import argparse
import sys
from pathlib import Path
from typing import Final

from src.api.client import ParallelClient
from src.api.research import DeepResearch
from src.domain.result import TaskResult
from src.domain.session import ResearchSession
from src.domain.task import ResearchTask
from src.image.generator import CoverGenerator
from src.pdf.document import ResearchDocument
from src.pdf.palette import HokusaiPalette
from src.storage.file import JsonFile
from src.storage.repository import SessionsRepository


class Application:
    """Main application for research workflow."""

    def __init__(self, root: Path) -> None:
        """Initialize with project root path."""
        self._root: Final[Path] = root
        self._data: Final[Path] = root / "data" / "research.json"
        self._covers: Final[Path] = root / "data" / "covers"
        self._output: Final[Path] = root / "output" / "reports"

    def list(self) -> None:
        """List all research sessions."""
        repository = SessionsRepository(JsonFile(self._data))
        sessions = repository.load()
        if not sessions:
            print("No research sessions found")
            return
        for session in sessions:
            count = len(session.tasks())
            print(f"[{session.id()[:8]}] {session.topic()}")
            print(f"  Created: {session.created().strftime('%Y-%m-%d %H:%M')}")
            print(f"  Tasks: {count}")
            print()

    def show(self, identifier: str) -> None:
        """Show details of specific session."""
        repository = SessionsRepository(JsonFile(self._data))
        session = self._match(repository, identifier)
        if not session:
            print(f"Session not found: {identifier}")
            return
        print(f"Topic: {session.topic()}")
        print(f"ID: {session.id()}")
        print(f"Created: {session.created().isoformat()}")
        print(f"\nTasks ({len(session.tasks())}):")
        for task in session.tasks():
            print(f"\n  [{task.status()}] {task.query()}")
            if task.result():
                print(f"  Summary: {task.result().summary()[:100]}...")
                print(f"  Sources: {len(task.result().sources())}")

    def generate(self, identifier: str, html: bool = False) -> None:
        """Generate PDF report for session."""
        repository = SessionsRepository(JsonFile(self._data))
        session = self._match(repository, identifier)
        if not session:
            print(f"Session not found: {identifier}")
            return
        cover = self._cover(session.id())
        document = ResearchDocument(session, HokusaiPalette(), cover)
        name = self._slug(session.topic())
        if html:
            path = self._output / f"{name}.html"
            document.html(path)
            print(f"HTML saved: {path}")
        else:
            path = self._output / f"{name}.pdf"
            document.save(path)
            print(f"PDF saved: {path}")

    def create(self, topic: str) -> None:
        """Create new research session."""
        repository = SessionsRepository(JsonFile(self._data))
        session = ResearchSession(topic=topic, tasks=tuple())
        repository.append(session)
        print(f"Created session: {session.id()[:8]}")
        print(f"Topic: {topic}")

    def research(
        self, identifier: str, query: str, processor: str, language: str
    ) -> None:
        """Execute deep research and generate PDF."""
        repository = SessionsRepository(JsonFile(self._data))
        session = self._match(repository, identifier)
        if not session:
            print(f"Session not found: {identifier}")
            return
        print(f"Session: {session.topic()}")
        print(f"Query: {query}")
        print(f"Processor: {processor}")
        print(f"Language: {language}")
        client = ParallelClient.create()
        executor = DeepResearch(client)
        response = executor.execute(query, processor)
        if response.failed():
            print("Research failed")
            return
        result = TaskResult(
            summary=response.markdown(),
            sources=response.sources(),
        )
        task = ResearchTask(
            query=query,
            status="completed",
            result=result,
            language=language,
        )
        updated = session.extend(task)
        repository.update(updated)
        print(f"Results saved: {len(response.sources())} sources")
        print("Generating cover image...")
        generator = CoverGenerator(self._covers)
        cover = generator.generate(session.topic(), session.id())
        print(f"Cover generated: {cover}")
        name = self._slug(session.topic())
        path = self._output / f"{name}.pdf"
        document = ResearchDocument(updated, HokusaiPalette(), cover)
        document.save(path)
        print(f"PDF generated: {path}")

    def _cover(self, identifier: str) -> Path | None:
        """Find cover image by session ID."""
        path = self._covers / f"{identifier}.jpg"
        if path.exists():
            return path
        return None

    def _match(
        self, repository: SessionsRepository, identifier: str
    ) -> ResearchSession | None:
        """Find session by partial ID match."""
        for session in repository.load():
            if session.id().startswith(identifier):
                return session
        return None

    def _slug(self, text: str) -> str:
        """Convert text to filename-safe slug."""
        return (
            text.lower()
            .replace(" ", "-")
            .replace(":", "")
            .replace("/", "-")[:50]
        )


def main() -> None:
    """Parse arguments and run application."""
    parser = argparse.ArgumentParser(
        prog="research",
        description="Research workflow with Hokusai-style PDF reports",
    )
    subparsers = parser.add_subparsers(dest="command", required=True)
    subparsers.add_parser("list", help="List all sessions")
    show = subparsers.add_parser("show", help="Show session details")
    show.add_argument("id", help="Session ID (partial match)")
    gen = subparsers.add_parser("generate", help="Generate PDF report")
    gen.add_argument("id", help="Session ID (partial match)")
    gen.add_argument("--html", action="store_true", help="Generate HTML instead")
    create = subparsers.add_parser("create", help="Create new session")
    create.add_argument("topic", help="Research topic")
    research = subparsers.add_parser("research", help="Execute deep research")
    research.add_argument("id", help="Session ID (partial match)")
    research.add_argument("query", help="Research query")
    research.add_argument("--processor", default="pro", help="Compute: lite, base, core, core2x, pro, ultra, ultra2x, ultra4x, ultra8x (add -fast for speed)")
    research.add_argument("--language", default="русский", help="Research language")
    args = parser.parse_args()
    app = Application(Path(__file__).parent.parent)
    if args.command == "list":
        app.list()
    elif args.command == "show":
        app.show(args.id)
    elif args.command == "generate":
        app.generate(args.id, args.html)
    elif args.command == "create":
        app.create(args.topic)
    elif args.command == "research":
        app.research(args.id, args.query, args.processor, args.language)


if __name__ == "__main__":
    main()
