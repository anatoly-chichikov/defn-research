# Research

An experiment in agent-first software.

## What is this?

This is a deep research tool where an AI coding agent (Claude Code, Cursor, etc.) acts as the interface between you and the program. You don't run commands directly — you describe what you want to research, and the agent handles everything else.

## How it works

1. **You describe the task** — in natural language, to your AI agent
2. **Agent asks follow-ups** — clarifying questions are part of the design, not a bug
3. **Agent runs the research** — handles Docker and parameters
4. **You get a PDF report** — Hokusai-inspired aesthetic, pleasant to read

The follow-up dialogue is intentional. The agent learns what you actually need before executing — language, depth level, specific angles to explore.

![Human-in-the-loop dialogue](./examples/human-in-the-loop.png)

**Tip:** Use plan mode for the best experience. Follow-up questions appear as selectors, making choices easier.

## Agent adaptability

If something breaks (environment issues, missing dependencies, config problems), the agent can fix it. You don't need to understand the infrastructure details — that's the agent's job.

## The DSL

Check `CLAUDE.md` for the agent's instruction set. It's simple — a few commands like:
- `rs <topic>` for new research
- `st` for status
- `pdf <topic>` for regeneration if needed. 
The agent will understand what to do even with incomplete or vague inputs.

When you run `rs <topic>`, the agent walks you through the entire flow — asks about language, depth, refines the topic with you. At the end, you get a ready PDF in the output folder.

## Testing

Run tests in Docker:

```bash
docker build -t research-test -f Dockerfile.test .
docker run --rm research-test
```

For agents: use `tst` command.

## Requirements

- Python 3.11+
- Docker
- An AI coding agent (Claude Code, Cursor, Windsurf, etc.)

## Envs

- `PARALLEL_API_KEY`: Parallel AI API access for research runs
- `VALYU_API_KEY`: Valyu API access for research runs
- `GEMINI_API_KEY` (optional): Gemini API access for cover image generation, empty means no image
- `REPORT_FOR` (optional): name inserted into report attribution line, empty means no name

## Providers

Two deep research engines are available:

| Aspect | Parallel | Valyu |
|--------|----------|-------|
| **Sources** | Open internet | Academic & proprietary sources |
| **Strength** | Strategic synthesis, executive summaries | Data-rich analysis, better citations |
| **Best for** | Business decisions, implementation planning | Academic research, evidence gathering |
| **Processors** | pro, ultra, ultra2x, ultra4x, ultra8x | lite, heavy |
| **Speed** | 2-30 min | 15-30 min |
| **Price** | Affordable | Higher (~3-4x) |

### Examples

Both engines researching "AI transformation of academic research":
- [Parallel example](./examples/parallel-ai-academic-research.pdf) — 21 pages, strategic focus
- [Valyu example](./examples/valyu-ai-academic-research.pdf) — 25 pages, data-rich

### When to choose

- **Parallel**: You need actionable recommendations, broad internet coverage, or faster turnaround
- **Valyu**: You need academic sources, proper citations, or comprehensive data analysis

## License

Apache 2.0
