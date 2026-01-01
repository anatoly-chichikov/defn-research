# Python deps audit

Дата: 2025-12-26

## Декларированные зависимости (pyproject.toml)

- weasyprint >=64.0
- PyHamcrest >=2.1.0
- markdown >=3.10
- httpx >=0.28.0
- parallel-web >=0.3.4
- google-genai >=1.0.0
- pillow >=12.0.0
- valyu >=2.3.4

Dev:
- pytest >=8.0.0
- ruff >=0.9.0

## Фактические импорты в коде

- weasyprint (HTML -> PDF)
- markdown (md -> html)
- parallel (SDK через parallel-web)
- google.genai (Gemini SDK)
- PIL.Image (JPEG сохранение)
- valyu (SDK)
- requests (HTTP GET для изображений Valyu)
- argparse (CLI)
- json (std)

## Несоответствия

- httpx декларирован, но в коде не используется
- requests используется, но не декларирован как зависимость

## Ключевые поведения

- PDF: HTML -> PDF через WeasyPrint
- Research providers: Parallel (SSE events), Valyu (SDK wait + прямой статус эндпоинт через requests)
- Cover image: Gemini image generation, сохранение JPEG
- CLI команды: list, show, generate, create, run, research
