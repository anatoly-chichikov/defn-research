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

**Tip:** Use plan mode for the best experience. Follow-up questions appear as selectors, making choices easier.

## Agent adaptability

If something breaks (environment issues, missing dependencies, config problems), the agent can fix it. You don't need to understand the infrastructure details — that's the agent's job.

## The DSL

Check `CLAUDE.md` for the agent's instruction set. It's simple — a few commands like `rs <topic>` for new research, `st` for status, `pdf <topic>` for generation. The agent picks it up quickly.

When you run `rs <topic>`, the agent walks you through the entire flow — asks about language, depth, refines the topic with you. At the end, you get a ready PDF in the output folder.

## Requirements

- Python 3.11+
- Docker
- API keys: `PARALLEL_API_KEY`, `GEMINI_API_KEY`
- An AI coding agent (Claude Code, Cursor, Windsurf, etc.)

## License

Apache 2.0
