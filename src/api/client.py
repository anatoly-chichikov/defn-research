"""Client wrapper for Parallel AI SDK."""
from __future__ import annotations

from abc import ABC
from abc import abstractmethod
from typing import Final

from parallel import Parallel


class Connectable(ABC):
    """Object that provides Parallel SDK client."""

    @abstractmethod
    def sdk(self) -> Parallel:
        """Return SDK client instance."""
        ...


class ParallelClient(Connectable):
    """Wrapper for Parallel AI SDK client."""

    def __init__(self, client: Parallel) -> None:
        """Initialize with SDK client instance."""
        self._client: Final[Parallel] = client

    def sdk(self) -> Parallel:
        """Return SDK client instance."""
        return self._client

    @classmethod
    def create(cls) -> ParallelClient:
        """Create client from environment variables."""
        return cls(Parallel())
