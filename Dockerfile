FROM clojure:temurin-21-lein

WORKDIR /app

RUN apt-get update && apt-get install -y --no-install-recommends python3 python3-venv python3-dev curl libpango-1.0-0 libpangocairo-1.0-0 libcairo2 libgdk-pixbuf2.0-0 libffi8 libssl3 fonts-noto-cjk fonts-noto-color-emoji && rm -rf /var/lib/apt/lists/*
RUN curl -Ls https://astral.sh/uv/install.sh | sh
ENV PATH="/root/.cargo/bin:/root/.local/bin:${PATH}"
ENV UV_CACHE_DIR=/app/.uv-cache
ENV PDF_ENGINE=weasyprint
ENV LANG=C.UTF-8
ENV LC_ALL=C.UTF-8
ENV JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8"
RUN uv run --with weasyprint weasyprint --version

COPY project.clj ./project.clj
RUN lein deps
COPY deps.edn ./deps.edn
COPY pyproject.toml ./pyproject.toml
RUN uv sync

COPY resources ./resources
COPY src ./src
COPY data ./data

ENTRYPOINT ["lein", "run"]
