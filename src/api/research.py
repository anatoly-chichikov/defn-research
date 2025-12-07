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
    def execute(self, query: str, processor: str) -> ResearchResponse:
        """Execute research and return response."""
        ...


class DeepResearch(Researchable):
    """Deep research executor using Parallel AI SDK with SSE streaming."""

    def __init__(self, client: Connectable) -> None:
        """Initialize with SDK client wrapper."""
        self._client: Final[Connectable] = client

    def execute(self, query: str, processor: str) -> ResearchResponse:
        """Execute deep research with real-time progress streaming."""
        sdk = self._client.sdk()
        task = sdk.beta.task_run.create(
            input=query,
            processor=processor,
            task_spec=TaskSpecParam(output_schema=TextSchemaParam()),
            enable_events=True,
            betas=["events-sse-2025-07-24"],
        )
        print(f"Research started: {task.run_id}", flush=True)
        print("Streaming progress...", flush=True)
        for event in sdk.beta.task_run.events(task.run_id):
            self._emit(event)
        result = sdk.task_run.result(task.run_id, api_timeout=7200)
        return ResearchResponse.parse(result)

    def _emit(self, event) -> None:
        """Emit progress event to console."""
        kind = getattr(event, "type", "unknown")
        if kind == "task_run.state":
            status = event.run.status if hasattr(event, "run") else "unknown"
            print(f"[STATUS] {status}", flush=True)
        elif kind == "task_run.progress_stats":
            meter = min(getattr(event, "progress_meter", 0), 1.0)
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
