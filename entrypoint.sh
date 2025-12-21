#!/bin/sh
set -eu

command="${1:-}"
if [ -z "${command}" ]; then
  echo "Request file path is required" >&2
  exit 2
fi

case "${command}" in
  list|show|generate|create|research)
    exec uv run --frozen --no-dev --no-sync python -m src.main "$@"
    ;;
esac

request="${command}"
if [ ! -f "${request}" ]; then
  echo "Request file was not found at path: ${request}" >&2
  exit 2
fi

processor="${PROCESSOR:-pro}"
language="${LANGUAGE:-русский}"

first="$(awk '{gsub("\r",""); if ($0 ~ /^[[:space:]]*$/) next; line=$0; sub(/^[[:space:]]+/,"",line); sub(/[[:space:]]+$/,"",line); print line; exit}' "${request}")"
declared="$(printf '%s\n' "${first}" | awk '{sub(/^[[:space:]]*(Язык ответа:|Language:)[[:space:]]*/,""); sub(/[[:space:]]*[.]?[[:space:]]*$/,""); print}')"
if printf '%s' "${first}" | awk 'BEGIN{ok=1} /^[[:space:]]*(Язык ответа:|Language:)/{ok=0} END{exit ok}'; then
  if [ -n "${declared}" ] && [ "${declared}" != "${language}" ]; then
    echo "Request file language did not match LANGUAGE env var: ${declared} vs ${language}" >&2
    exit 2
  fi
fi

brief="$(awk 'BEGIN{state=0} {gsub("\r",""); if (state==0) { if ($0 ~ /^[[:space:]]*$/) next; if ($0 ~ /^[[:space:]]*(Язык ответа:|Language:)/) {state=1; next} state=1 } print $0 }' "${request}")"
topic="$(printf '%s\n' "${brief}" | awk '{if ($0 ~ /^[[:space:]]*$/) next; line=$0; sub(/^[[:space:]]+/,"",line); sub(/^#+[[:space:]]*/,"",line); sub(/[[:space:]]+$/,"",line); print line; exit}')"
if [ -z "${topic}" ]; then
  echo "Request file topic was not found" >&2
  exit 2
fi

prefix="$(uv run --frozen --no-dev --no-sync python -m src.main create "${topic}" | sed -n 's/^Created session: //p' | head -n 1)"
if [ -z "${prefix}" ]; then
  echo "Session identifier was not produced during create" >&2
  exit 2
fi

full="$(python -c "import json, pathlib, sys; data=json.loads(pathlib.Path('data/research.json').read_text(encoding='utf-8')); pre=sys.argv[1]; sessions=data.get('sessions', []); matches=[s.get('id','') for s in sessions if str(s.get('id','')).startswith(pre)]; print(matches[-1] if matches else '')" "${prefix}")"
if [ -z "${full}" ]; then
  echo "Session identifier was not found in repository for prefix: ${prefix}" >&2
  exit 2
fi

mkdir -p data/briefs
printf "%s\n" "${brief}" > "data/briefs/${full}.md"

query="$(printf "Язык ответа: %s.\n\n%s" "${language}" "${brief}")"

exec uv run --frozen --no-dev --no-sync python -m src.main research "${prefix}" "${query}" --processor "${processor}" --language "${language}"
