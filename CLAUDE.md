# CLAUDE.md

## Commands

| Command | Action |
|---------|--------|
| `rs <topic>` | New research |
| `st` | List sessions with PDF paths |
| `pdf <topic>` | Generate PDF |

If message starts with these commands — it's a research operation, not development.

---

## rs

New research. Dialog first, then launch.

ask language Which language for the result?
  - English
  - 中文 (Chinese)
  - Español (Spanish)
  - हिन्दी (Hindi)
  - Русский (Russian)

After language selected — switch all follow-up questions to that language.

ask processor What compute level? (pro/ultra/ultra2x)

do Help refine the topic
do Identify blind spots
do Suggest non-obvious angles
dont Launch immediately — dialog first

brief format:
- short title (max 120 chars, no colons/subtitles) + "Research:" + flat numbered list
- title = noun phrase, not a question or full sentence
- bad: "Is AI image generation real creativity or just entertainment?"
- good: "AI art as creativity"
- dense single-line items, all details via dash/colon in one line
- no bold, no subheadings, no nested lists, no extra sections
- language = result language

run docker build -t research .
run docker run -d --name "research-{timestamp}-{slug}" \
    -v "$(pwd)/output:/app/output" \
    -v "$(pwd)/data:/app/data" \
    -e PARALLEL_API_KEY -e GEMINI_API_KEY \
    -e PROCESSOR="{processor}" -e LANGUAGE="{language}" \
    research /app/data/requests/{slug}.md

notify container_name
notify estimated_time
notify pdf_path — exact full path (no wildcards!), build after getting session ID

Example output:
```
Container: research-20241221-1430-clojure-pdf
Processor: ultra2x
Time: 5-50 min
PDF: /Users/chichikov/Work/research/output/2025-12-21_clojure-pdf_3e4fc072/clojure-pdf.pdf [NOT READY]
```

---

## st

List sessions. For each:
- Topic
- Status (in_progress % / completed)
- Full PDF path
- If file missing — mark [NOT READY]

Example:
```
[HITL startups] in_progress (67%)
  PDF: /Users/chichikov/Work/research/output/2025-12-21_hitl-startups_3e4fc072/hitl-startups.pdf [NOT READY]

[AI coding assistants] completed
  PDF: /Users/chichikov/Work/research/output/2025-12-20_ai-coding_8f2a1b3c/ai-coding.pdf
```

---

## pdf

Generate PDF by topic. Find session by meaning (not by ID).

run docker run --rm \
    -v "$(pwd)/output:/app/output" \
    -v "$(pwd)/data:/app/data" \
    research generate {id}

notify pdf_path (full path)

---

## Processors

| Name | Time | Use case |
|------|------|----------|
| `pro` | 2-10 min | Default, exploratory |
| `ultra` | 5-25 min | Multi-source deep |
| `ultra2x` | 5-50 min | Complex deep research |
| `ultra4x` | 5-90 min | Very complex |
| `ultra8x` | 5 min-2 h | Maximum depth |

Tip: add `-fast` for speed (pro-fast, ultra-fast)

---

## Environment

```bash
export PARALLEL_API_KEY="..."
export GEMINI_API_KEY="..."
```
