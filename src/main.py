"""CLI entry point for research workflow."""
from __future__ import annotations

import os

os.environ.setdefault("DYLD_LIBRARY_PATH", "/opt/homebrew/lib")

import argparse
import sys
from pathlib import Path
from typing import Final
from urllib.parse import urlparse

import requests
from src.api.client import ParallelClient
from src.api.research import DeepResearch
from src.api.valyu import ValyuResearch
from src.domain.pending import PendingRun
from src.domain.result import TaskResult
from src.domain.session import ResearchSession
from src.domain.task import ResearchTask
from src.image.generator import CoverGenerator
from src.pdf.document import ResearchDocument
from src.pdf.palette import HokusaiPalette
from src.storage.file import JsonFile
from src.storage.organizer import OutputOrganizer
from src.storage.repository import SessionsRepository


class Application:
    """Main application for research workflow."""

    def __init__(self, root: Path) -> None:
        """Initialize with project root path."""
        self._root: Final[Path] = root
        self._data: Final[Path] = root / "data" / "research.json"
        self._output: Final[Path] = root / "output"

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
        provider = self._provider(session)
        organizer = self._organizer()
        name = organizer.name(session.created(), session.topic(), session.id())
        cover = organizer.existing(name, provider)
        document = ResearchDocument(session, HokusaiPalette(), cover)
        if html:
            path = organizer.html(name, provider)
            document.html(path)
            print(f"HTML saved: {path}")
        else:
            path = organizer.report(name, provider)
            document.save(path)
            print(f"PDF saved: {path}")

    def create(self, topic: str) -> str:
        """Create new research session."""
        repository = SessionsRepository(JsonFile(self._data))
        session = ResearchSession(topic=topic, tasks=tuple())
        repository.append(session)
        token = session.id()[:8]
        print(f"Created session: {token}")
        print(f"Topic: {topic}")
        return token

    def research(
        self, identifier: str, query: str, processor: str, language: str, provider: str
    ) -> None:
        """Execute deep research and generate PDF."""
        repository = SessionsRepository(JsonFile(self._data))
        session = self._match(repository, identifier)
        if not session:
            print(f"Session not found: {identifier}")
            return
        print(f"Session: {session.topic()}")
        pending = session.pending()
        if pending:
            run_id = pending.identifier()
            query = pending.query()
            processor = pending.processor()
            language = pending.language()
            provider = pending.provider()
            print(f"Resuming run: {run_id[:16]}...")
            print(f"Query: {query}")
            print(f"Processor: {processor}")
        else:
            print(f"Query: {query}")
            print(f"Processor: {processor}")
            print(f"Language: {language}")
            provider = self._resolve(provider, processor)
            processor = self._model(provider, processor)
            executor = self._executor(provider)
            run_id = executor.start(query, processor)
            print(f"Research started: {run_id}")
            pending = PendingRun(run_id, query, processor, language, provider)
            session = session.start(pending)
            repository.update(session)
        executor = self._executor(provider)
        print("Streaming progress...", flush=True)
        executor.stream(run_id)
        print("Fetching result...", flush=True)
        response = executor.finish(run_id)
        if response.failed():
            print("Research failed")
            return
        session = session.clear()
        repository.update(session)
        service = f"{provider}.ai"
        organizer = self._organizer()
        name = organizer.name(session.created(), session.topic(), session.id())
        organizer.response(name, provider, response.raw())
        self._store(name, provider, response.raw(), organizer)
        brief = self._root / "data" / "briefs" / f"{session.id()}.md"
        if brief.exists():
            organizer.brief(name, provider, brief.read_text(encoding="utf-8"))
        print(f"Response saved: {organizer.folder(name, provider)}")
        result = TaskResult(
            summary=response.markdown(),
            sources=response.sources(),
        )
        task = ResearchTask(
            query=query,
            status="completed",
            result=result,
            language=language,
            service=service,
        )
        updated = session.extend(task)
        repository.update(updated)
        print(f"Results saved: {len(response.sources())} sources")
        cover = organizer.cover(name, provider)
        key = os.getenv("GEMINI_API_KEY")
        if not key:
            print("Gemini API key not set, skipping image generation")
        else:
            print("Generating cover image...")
            generator = CoverGenerator()
            generator.generate(updated.topic(), cover)
            print(f"Cover generated: {cover}")
        self._pdf(updated, cover, name, provider, organizer)

    def _run(self, topic: str, query: str, processor: str, language: str, provider: str) -> None:
        """Create session and execute research."""
        token = self.create(topic)
        self.research(token, query, processor, language, provider)

    def _match(
        self, repository: SessionsRepository, identifier: str
    ) -> ResearchSession | None:
        """Find session by partial ID match."""
        for session in repository.load():
            if session.id().startswith(identifier):
                return session
        return None

    def _organizer(self) -> OutputOrganizer:
        """Return output organizer."""
        return OutputOrganizer(self._output)

    def _pdf(
        self,
        session: ResearchSession,
        cover: Path,
        name: str,
        provider: str,
        organizer: OutputOrganizer,
    ) -> Path:
        """Generate PDF report and return path."""
        path = organizer.report(name, provider)
        document = ResearchDocument(session, HokusaiPalette(), cover)
        document.save(path)
        print(f"PDF generated: {path}")
        return path

    def _store(self, name: str, provider: str, data: dict, organizer: OutputOrganizer) -> None:
        """Store valyu images in output folder."""
        if provider != "valyu":
            return
        items = data.get("images", []) or []
        if not items:
            return
        folder = organizer.folder(name, provider) / "images"
        folder.mkdir(parents=True, exist_ok=True)
        for item in items:
            url = item.get("image_url", "") if isinstance(item, dict) else ""
            code = item.get("image_id", "") if isinstance(item, dict) else ""
            if not url or not code:
                continue
            part = urlparse(url).path
            suffix = Path(part).suffix or ".png"
            path = folder / f"{code}{suffix}"
            if path.exists():
                continue
            try:
                reply = requests.get(url, timeout=30)
                reply.raise_for_status()
                path.write_bytes(reply.content)
            except Exception:
                continue

    def _provider(self, session: ResearchSession) -> str:
        """Return provider from latest task or default."""
        tasks = session.tasks()
        if tasks:
            service = tasks[-1].service()
            if service.endswith(".ai"):
                return service.split(".")[0]
        return "parallel"

    def _resolve(self, provider: str, processor: str) -> str:
        """Return provider, validating known values."""
        if provider in {"parallel", "valyu"}:
            if provider == "valyu" and processor == "pro":
                return "valyu"
            return provider
        raise ValueError("Provider must be parallel or valyu")

    def _model(self, provider: str, processor: str) -> str:
        """Return model name for provider."""
        if provider == "valyu":
            if processor in {"lite", "heavy"}:
                return processor
            if processor == "pro":
                return "heavy"
            raise ValueError("Processor must be lite or heavy for valyu")
        return processor

    def _executor(self, provider: str) -> DeepResearch | ValyuResearch:
        """Return executor for provider."""
        if provider == "parallel":
            client = ParallelClient.create()
            return DeepResearch(client)
        if provider == "valyu":
            key = os.getenv("VALYU_API_KEY")
            if not key:
                raise RuntimeError("VALYU_API_KEY is required for valyu provider")
            from valyu import Valyu
            client = Valyu(api_key=key)
            return ValyuResearch(client.deepresearch)
        raise ValueError("Provider must be parallel or valyu")


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
    run = subparsers.add_parser("run", help="Create session and execute deep research")
    run.add_argument("topic", help="Research topic")
    run.add_argument("query", help="Research query")
    run.add_argument("--processor", default="pro", help="Compute: lite, base, core, core2x, pro, ultra, ultra2x, ultra4x, ultra8x (add -fast for speed)")
    run.add_argument("--language", default="русский", help="Research language")
    run.add_argument("--provider", default="parallel", choices=["parallel", "valyu"], help="Data provider: parallel or valyu")
    research = subparsers.add_parser("research", help="Execute deep research")
    research.add_argument("id", help="Session ID (partial match)")
    research.add_argument("query", help="Research query")
    research.add_argument("--processor", default="pro", help="Compute: lite, base, core, core2x, pro, ultra, ultra2x, ultra4x, ultra8x (add -fast for speed)")
    research.add_argument("--language", default="русский", help="Research language")
    research.add_argument("--provider", default="parallel", choices=["parallel", "valyu"], help="Data provider: parallel or valyu")
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
    elif args.command == "run":
        app._run(args.topic, args.query, args.processor, args.language, args.provider)
    elif args.command == "research":
        app.research(args.id, args.query, args.processor, args.language, args.provider)


if __name__ == "__main__":
    main()
