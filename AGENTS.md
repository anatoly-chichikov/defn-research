# AGENTS.md

Agent instructions for research automation.

## Commands

| Command | Action |
|---------|--------|
| `rs <topic>` | New research |
| `st` | List sessions with PDF paths |
| `pdf <topic>` | Generate PDF |
| `tst` | Run tests in Docker |

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

ask provider Which data provider?
  - parallel
  - valyu

ask processor What compute level? (parallel: pro/ultra/ultra2x, valyu: lite/heavy)

do Help refine the topic
do Identify blind spots
do Suggest non-obvious angles
dont Launch immediately — dialog first

If user asks for two runs at once:
- ask the same questions twice, explicitly for run A then run B (no multi-select)
- collect params for run A and run B (topic, language, provider, processor)
- start two docker containers (different names) and report both

brief format:
- short title (max 120 chars, no colons/subtitles) + "Research:" + flat numbered list
- title = noun phrase, not a question or full sentence
- bad: "Is AI image generation real creativity or just entertainment?"
- good: "AI art as creativity"
- dense single-line items, all details via dash/colon in one line
- no bold, no subheadings, no nested lists, no extra sections
- language = result language

run docker build -t research .
query="Язык ответа: {language}.\n\n{brief}"
run docker run -d --name "research-{timestamp}-{slug}" \
    -v "$(pwd)/output:/app/output" \
    -v "$(pwd)/data:/app/data" \
    -e PARALLEL_API_KEY -e VALYU_API_KEY -e GEMINI_API_KEY \
    research run "{topic}" "{query}" --processor "{processor}" --language "{language}" --provider "{provider}"

If two runs requested, run the command twice with different {timestamp}-{slug} values.

notify container_name
notify estimated_time
notify pdf_path — exact full path (no wildcards!), build after getting session ID

Example output:
```
Container: research-20241221-1430-clojure-pdf
Processor: ultra2x
Provider: parallel
Time: 5-50 min
PDF: /Users/chichikov/Work/research/output/2025-12-21_clojure-pdf_3e4fc072/parallel/2025-12-21_clojure-pdf.pdf [NOT READY]
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
  PDF: /Users/chichikov/Work/research/output/2025-12-21_hitl-startups_3e4fc072/parallel/2025-12-21_hitl-startups.pdf [NOT READY]

[AI coding assistants] completed
  PDF: /Users/chichikov/Work/research/output/2025-12-20_ai-coding_8f2a1b3c/parallel/2025-12-20_ai-coding.pdf
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

## tst

Run tests in Docker container.

run docker build -t research-test -f Dockerfile.test .
run docker run --rm research-test

notify test results (pass/fail count)

---

## Parallel processors

| Name | Time | Use case |
|------|------|----------|
| `pro` | 2-10 min | Default, exploratory |
| `ultra` | 5-25 min | Multi-source deep |
| `ultra2x` | 5-50 min | Complex deep research |
| `ultra4x` | 5-90 min | Very complex |
| `ultra8x` | 5 min-2 h | Maximum depth |

Tip: add `-fast` for speed (pro-fast, ultra-fast)

## Valyu models

| Name | Use case |
|------|----------|
| `lite` | Faster, lighter research |
| `heavy` | Deeper, more thorough |

---

## Environment

```bash
export PARALLEL_API_KEY="..."
export VALYU_API_KEY="..."
export GEMINI_API_KEY="..."
```
