"""Deep research business logic."""
from __future__ import annotations

from abc import ABC
from abc import abstractmethod
from typing import Final

from parallel.types import TaskSpecParam
from parallel.types import TextSchemaParam

from src.api.client import Connectable
from src.api.response import ResearchResponse


class Researchable(ABC):
    """Object that can perform deep research."""

    @abstractmethod
    def start(self, query: str, processor: str) -> str:
        """Create task and return run_id."""
        ...

    @abstractmethod
    def stream(self, identifier: str) -> None:
        """Stream SSE events for progress display."""
        ...

    @abstractmethod
    def finish(self, identifier: str) -> ResearchResponse:
        """Get final result by run_id."""
        ...


class DeepResearch(Researchable):
    """Deep research executor using Parallel AI SDK with SSE streaming."""

    def __init__(self, client: Connectable) -> None:
        """Initialize with SDK client wrapper."""
        self._client: Final[Connectable] = client

    def start(self, query: str, processor: str) -> str:
        """Create task and return run_id for persistence."""
        sdk = self._client.sdk()
        task = sdk.beta.task_run.create(
            input=query,
            processor=processor,
            task_spec=TaskSpecParam(output_schema=TextSchemaParam()),
            enable_events=True,
            betas=["events-sse-2025-07-24"],
        )
        return task.run_id

    def stream(self, identifier: str) -> None:
        """Stream SSE events, gracefully handles disconnection."""
        sdk = self._client.sdk()
        try:
            for event in sdk.beta.task_run.events(identifier):
                self._emit(event)
        except Exception as err:
            print(f"[WARN] SSE disconnected: {err}", flush=True)
            print("[INFO] Will fetch result directly", flush=True)

    def finish(self, identifier: str) -> ResearchResponse:
        """Get final result, works even if SSE failed."""
        sdk = self._client.sdk()
        result = sdk.task_run.result(identifier, api_timeout=7200)
        return ResearchResponse.parse(result)

    def _emit(self, event) -> None:
        """Emit progress event to console."""
        kind = getattr(event, "type", "unknown")
        if kind == "task_run.state":
            status = event.run.status if hasattr(event, "run") else "unknown"
            print(f"[STATUS] {status}", flush=True)
        elif kind == "task_run.progress_stats":
            meter = getattr(event, "progress_meter", 0) / 100.0
            stats = getattr(event, "source_stats", None)
            sources = getattr(stats, "num_sources_read", 0) if stats else 0
            print(f"[PROGRESS] {meter:.0%} | Sources: {sources}", flush=True)
        elif kind.startswith("task_run.progress_msg"):
            message = getattr(event, "message", "")
            truncated = message[:120] + "..." if len(message) > 120 else message
            label = kind.split(".")[-1].upper()
            print(f"[{label}] {truncated}", flush=True)
        elif kind == "error":
            message = getattr(event, "message", "Unknown error")
            print(f"[ERROR] {message}", flush=True)
