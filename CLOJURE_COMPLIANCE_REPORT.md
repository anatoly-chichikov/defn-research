# Clojure Compliance Report

Аудит каноничности Clojure-кода проекта **defn-research**.
Анализ 43 исходных файлов, 30 тестов, конфигурации проекта.

---

## Общая оценка

| Модуль | Оценка | Вердикт |
|--------|--------|---------|
| **domain** | 6/10 | Хорошие протоколы, но java.util.Optional и дублирование |
| **storage** | 7/10 | Чистые протоколы, адекватный I/O |
| **api.http / progress / link** | 8/10 | Компактно, идиоматично |
| **api.research / response** | 6/10 | atom внутри протокола, нормальная структура |
| **api.parallel** | 7/10 | Чистая реализация провайдера |
| **api.valyu** | 6/10 | Рабочий код, но императивный polling |
| **api.xai** | 5/10 | Сложный Python interop, длинные функции |
| **main** | 5/10 | Массивное дублирование в execute.clj |
| **pdf** | 6/10 | HTML-конкатенация вместо шаблонов, re-export через def |
| **image** | 7/10 | Плотный Java interop, но структурирован |
| **config** | 9/10 | Минимальный, правильный |
| **Проект в целом** | 6.5/10 | Протокол-ориентированная архитектура каноничная, но реализация содержит системные антипаттерны |

---

## Что каноничного (сильные стороны)

### 1. Protocol-first архитектура — отлично

Каждый модуль определяет протокол, каждый протокол реализуется через `defrecord`. Это ядро идиоматичного Clojure: полиморфизм без наследования.

```clojure
;; research/api/research.clj — чистый контракт
(defprotocol Researchable
  (start [item query processor] "Start research and return run id.")
  (stream [item id] "Stream progress updates.")
  (finish [item id] "Finish research and return response."))
```

Три провайдера (Parallel, Valyu, XAI) реализуют один протокол — каноничный паттерн.

**Модули:** все 43 файла.

### 2. Иммутабельные структуры данных

Данные текут через maps и vectors. Нет `def` с мутацией, нет глобального состояния. `defrecord` используется правильно — как именованные структуры с протоколами.

### 3. Private functions через defn-

Правильное сокрытие реализации: `node`, `point`, `scan`, `place`, `nest`, `lines`, `render` в domain-модулях. `loadlib`, `fetch`, `resize`, `median`, `canny` в image/frame.

### 4. Docstrings на всех public-функциях

Каждая публичная функция имеет docstring. Это выше среднего для Clojure-проектов.

### 5. Инструментарий

- `cljfmt` с полной конфигурацией (sort-ns-references, split-keypairs)
- `clj-kondo` для линтинга
- `kaocha` для тестов с matcher-combinators
- Правильные lein-алиасы (test, lint, fmt, fmt:check)

### 6. Namespace-организация

Чистая иерархия: `domain → storage → api → main → pdf/image`. Зависимости текут сверху вниз. Нет циклических зависимостей.

### 7. EDN для конфигурации

Prompt-шаблоны, cover-параметры, test-конфигурация — всё в EDN. Это каноничный Clojure-подход.

---

## Системные антипаттерны

### 1. java.util.Optional вместо nil — критический антипаттерн

**Файлы:** `domain/task.clj:183,193`, `domain/session.clj:48,54,56`, `domain/pending.clj`, `storage/organizer.clj:119`, `storage/repository.clj:37`, `main/execute.clj:84`

```clojure
;; Антипаттерн — Java Optional в Clojure:
(Optional/of (now))
(Optional/empty)
(.isPresent done)
(.get done)

;; Каноничный Clojure:
(task/now)   ; вместо (Optional/of (now))
nil          ; вместо (Optional/empty)
(some? done) ; вместо (.isPresent done)
done         ; вместо (.get done)
```

Clojure нативно использует `nil` как отсутствие значения. `java.util.Optional` — это Java-идиома, чужеродная для Clojure. Каждый вызов `.isPresent`/`.get` — это лишний церемониал.

**Масштаб:** 15+ мест. Это самый заметный антипаттерн проекта.

### 2. Массивное дублирование кода

#### a) task.clj ↔ pending.clj — идентичный код

Функции `node`, `point`, `scan`, `place`, `nest`, `lines`, `render` дублированы **полностью** между `research.domain.task` и `research.domain.pending`.

**task.clj:38-164** и **pending.clj:14-140** — ~130 строк идентичного кода.

Каноничное решение: вынести в `research.domain.brief` (shared namespace).

#### b) document.clj ↔ document/env.clj — дублирование функций

Функции `author`, `service`, `coverimage`, `brief` дублированы между `research.pdf.document` (строки 102-160) и `research.pdf.document.env` (строки 48-118).

`document.clj` содержит свои версии этих функций И импортирует их из `env.clj` через `def`:
```clojure
(def env "Env value." docenv/env)
(def emit "Render PDF." docenv/emit)
```

Но при этом переопределяет `author`, `service`, `coverimage`, `brief` локально.

#### c) execute.clj — дублирование блока запуска

`main/execute.clj` содержит два почти идентичных блока (строки 30-115 и 116-214): один для pending-рана, другой для нового рана. ~100 строк продублировано внутри одной функции.

### 3. Перегруженные let-блоки с переприсвоением

**Файлы:** `domain/task.clj:202-254`, `main/execute.clj:22-214`, `api/xai/py_client.clj:90-215`, `api/xai/py_client/collect.clj:30-168`

```clojure
;; Антипаттерн — последовательное переприсвоение одной переменной:
(let [text (str/replace text #"\\n" "\n")
      text (str/replace text #"\s+Research:" "\n\nResearch:")
      text (str/replace text #"(?m)(^|\n)(\s*)(\d+)\)" "$1$2$3.")
      text (str/replace text #"[ \t]+(\d+)[\.)]\s+" "\n$1. ")]
  text)

;; Каноничный Clojure — threading macro:
(-> text
    (str/replace #"\\n" "\n")
    (str/replace #"\s+Research:" "\n\nResearch:")
    (str/replace #"(?m)(^|\n)(\s*)(\d+)\)" "$1$2$3.")
    (str/replace #"[ \t]+(\d+)[\.)]\s+" "\n$1. "))
```

`pdf/document.clj:brief` особенно показателен — четырёхкратное повторение `(if (seq items) "" ...)`:
```clojure
(let [text (if (seq items) "" (text/listify text))
      text (if (seq items) "" (text/normalize text))
      text (if (seq items) "" (text/rule text))
      text (if (seq items) "" (cite/stars text))]
```

Каноничное решение:
```clojure
(let [text (if (seq items)
             ""
             (-> topic text/listify text/normalize text/rule cite/stars))]
```

### 4. Shadowing имён из clojure.core

**Файлы:** 8 из 43 файлов используют `:refer-clojure :exclude`

| Файл | Исключённые имена |
|------|-------------------|
| domain/task.clj | `format` |
| domain/session.clj | `extend`, `format` |
| storage/file.clj | `read` |
| storage/organizer.clj | `name` |
| storage/repository.clj | `find`, `load`, `update` |
| api/http.clj | `get` |
| api/xai/cache.clj | `load` |
| main.clj | `list` |

Shadowing `format`, `name`, `list`, `get`, `find`, `update`, `read`, `load`, `extend` — это 9 имён из `clojure.core`. Каноничный подход: использовать более специфичные имена (`fetch-data`, `save-sessions`, `find-session`), либо обращаться через полный namespace.

### 5. Позиционные аргументы вместо maps

```clojure
;; execute.clj — 9 позиционных аргументов:
(defn execute [root data out id query processor language provider env] ...)

;; launch.clj — 9 позиционных аргументов:
(defn launch [root data out topic query processor language provider env] ...)
```

Каноничный Clojure для >3 аргументов:
```clojure
(defn execute [{:keys [root data out id query processor language provider env]}] ...)
```

### 6. loop/recur вместо reduce/map

Множество мест используют `loop/recur` там, где `reduce`, `map`, `filter` были бы идиоматичнее:

- `domain/task.clj:scan` (строки 83-96) — это `reduce`
- `domain/task.clj:lines` (строки 125-142) — это `map-indexed`
- `pdf/document/text.clj:toc` (строки 93-106) — это `reduce`
- `image/frame.clj:median` (строки 74-79) — это `reduce-kv` на гистограмме

`loop/recur` каноничен когда нужен ранний выход или множественный аккумулятор. Но в этих случаях `reduce` проще и читаемее.

### 7. atom внутри protocol-метода

**Файл:** `api/response.clj:39-67`

```clojure
(sources [_] (let [seen (atom #{})
                   policy (or (:link data) (link/make))]
               (reduce ... (swap! seen conj url) ...)))
```

Использование `atom` внутри `reduce` для трекинга "уже видели" — антипаттерн. Каноничное решение: передавать `seen` как часть аккумулятора `reduce`.

```clojure
;; Канонично:
(reduce (fn [[list seen] cite]
          (if (contains? seen url)
            [list seen]
            [(conj list item) (conj seen url)]))
        [[] #{}]
        items)
```

### 8. defrecord с единственным полем `data` — лишняя обёртка

**Файлы:** `api/http.clj`, `api/progress.clj`, `api/link.clj`, `api/xai/citations.clj`

```clojure
(defrecord Http [data]    ;; data = {:kind "httpkit"}
(defrecord Progress [data] ;; data = {:dot #"\."}
(defrecord Links [data]    ;; data = {:link ... :utm ...}
```

Эти records оборачивают конфигурацию в `data` без семантической нужды. Каноничнее: вынести поля на верхний уровень record или использовать reify/plain map.

### 9. Re-export через def

**Файл:** `pdf/document.clj:161-176`

```clojure
(def heading "Heading text." doctext/heading)
(def normalize "List blank lines." doctext/normalize)
(def listify "Inline list conversion." doctext/listify)
;; ... ещё 10 таких строк
```

Это не каноничный паттерн. Пользователь должен подключать нужный namespace напрямую. Re-export через `def` ломает навигацию, скрывает источник, дублирует документацию.

### 10. HTML через string concatenation

**Файлы:** `pdf/document.clj`, `pdf/wave.clj`, `pdf/document/text.clj`, `pdf/document/sources.clj`, `pdf/document/citations.clj`

Весь HTML генерируется через `(str "<div>" ... "</div>")`. Это работает, но не каноничный Clojure-подход. Идиоматично: hiccup-подобные структуры данных.

```clojure
;; Текущий подход (>500 строк HTML-конкатенации):
(str "<div class=\"container\">"
     "<h1>" title "</h1>"
     "</div>")

;; Каноничный Clojure (hiccup):
[:div.container [:h1 title]]
```

---

## Детальный разбор по модулям

### domain (task, session, result, pending)

| Критерий | Оценка | Детали |
|----------|--------|--------|
| Протоколы | +++ | Чёткие контракты: Tasked, Sessioned, Sourced, Summarized |
| Иммутабельность | ++ | Данные через maps/vectors |
| Дублирование | --- | task.clj ↔ pending.clj: ~130 строк копипасты |
| Java interop | -- | java.util.Optional повсюду |
| Стиль | + | Docstrings, defn- для private |
| Threading | - | let-chain вместо -> |
| Naming | - | Shadowing `format`, `extend` |

**Конкретные проблемы:**
- `task.clj:166-200` — record `ResearchRun` с 4 полями, но `data` — это map с 6 ключами, из которых `completed` — Optional. Каноничнее: вынести все поля на уровень record.
- `task.clj:202-254` — функция `task` на 52 строки с 15+ let-биндингами. Нужна декомпозиция.
- `result.clj` — чистый, компактный, каноничный. Лучший файл в domain.

### storage (file, organizer, repository)

| Критерий | Оценка | Детали |
|----------|--------|--------|
| Протоколы | +++ | Reader/Writer/Existing — классика |
| Разделение чтения/записи | ++ | repository/read.clj, repository/write.clj |
| Java NIO interop | ++ | Корректное использование Path/Files |
| Размер функций | - | repository/read.clj:items — 113 строк, одна функция |
| Side effects | - | Миграция данных внутри read (spit в read.clj:52-54) |

**Конкретные проблемы:**
- `repository/read.clj:8-112` — функция `items` на 104 строки. Содержит миграцию данных (запись в файл) внутри чтения — нарушение принципа разделения.
- `organizer.clj:25-69` — `translit` — чистая функция, каноничная, но литеральная map для транслитерации занимает 40 строк. Допустимо.

### api (http, progress, link, research, response)

| Критерий | Оценка | Детали |
|----------|--------|--------|
| Протоколы | +++ | Researchable, Grounded, Responded, Linkable |
| Компактность | +++ | http.clj — 21 строка, идеально |
| Чистота | + | link.clj — чистые трансформации |
| atom в reduce | -- | response.clj:39 — мутабельное состояние |
| Обёртки | - | parallel.clj:12-41 — 6 делегирующих функций без логики |

**Конкретные проблемы:**
- `parallel.clj:12-41` — `now`, `env`, `clean`, `emit`, `parse`, `sse` просто делегируют в `support`. Лишний уровень индирекции. Каноничнее: использовать `support` напрямую.
- `response.clj:16-29` — `clean`, `strip`, `domain` создают new `link/make` при каждом вызове. Каноничнее: передавать `Links` instance.

### api.parallel

| Критерий | Оценка | Детали |
|----------|--------|--------|
| Реализация протокола | ++ | Чистая реализация Researchable |
| SSE-парсинг | ++ | support.clj:59-79 — loop/recur оправдан для line-by-line |
| Конфигурация | + | URL/ключи через env |
| Magic strings | - | `"search-extract-2025-10-10,events-sse-2025-07-24"` — без имени |

### api.valyu

| Критерий | Оценка | Детали |
|----------|--------|--------|
| Polling loop | - | stream метод — императивный polling с Thread/sleep |
| forward declare | - | `(declare valyu-emit)` — можно избежать переупорядочиванием |
| Конструктор | + | `valyu` принимает map — хорошо |
| URL manipulation | - | Ручное склеивание `/v1` к base URL |

### api.xai

| Критерий | Оценка | Детали |
|----------|--------|--------|
| Python interop | +/- | libpython-clj — каноничный подход, но код сложный |
| py_client.clj | -- | Функция `run` на 125 строк |
| collect.clj | -- | Функция `collect` на 139 строк |
| Шаблоны | ++ | collect.clj:template, fill — чистый EDN + postwalk |
| Кэширование | ++ | cache.clj — компактный, чистый |

**py_client.clj:88-215** — самая длинная функция проекта. Смешивает Python interop, бизнес-логику, форматирование результатов, URL-обработку. Нужна декомпозиция минимум на 4-5 функций.

### main

| Критерий | Оценка | Детали |
|----------|--------|--------|
| CLI-парсинг | ++ | tools.cli — каноничный выбор |
| Entry point | ++ | -main с case — просто и ясно |
| execute.clj | --- | 214 строк с массивным дублированием |
| seed.clj | +++ | 21 строка, идеально |
| launch.clj | + | Чистая оркестрация |

**execute.clj** — наименее каноничный файл проекта. Функция `execute` на 193 строки содержит:
- Два почти идентичных блока (pending vs new)
- Вложенность 5+ уровней let
- Смешение I/O (println), бизнес-логики, генерации отчётов
- 9 позиционных аргументов

### pdf

| Критерий | Оценка | Детали |
|----------|--------|--------|
| Палитра | +++ | palette.clj — каноничный record |
| Стили | ++ | style.clj — template filling через str/replace |
| HTML | -- | Конкатенация строк вместо hiccup |
| Re-export | -- | document.clj экспортирует 14 def-алиасов |
| Дублирование | -- | document.clj ↔ env.clj |
| Text processing | + | text.clj — набор чистых трансформаций |
| Citations | + | citations.clj — atom в одном месте, но менее критично |

### image

| Критерий | Оценка | Детали |
|----------|--------|--------|
| OpenCV interop | ++ | Чистый Java interop через import |
| generator.clj | + | Gemini API через http-kit |
| frame.clj | +/- | 502 строки, но хорошо декомпозирован на private fn |
| Конфигурация | ++ | detector с defaults через merge |

---

## Статистика

| Метрика | Значение |
|---------|----------|
| Файлов (src) | 43 |
| Файлов (test) | 30 |
| Протоколов | 22 |
| Records | 19 |
| `:refer-clojure :exclude` | 8 файлов, 9 уникальных имён |
| Использований `java.util.Optional` | 15+ мест |
| Функций > 50 строк | 8 |
| Функций > 100 строк | 3 (execute, run, collect) |
| Дублированных блоков | 3 крупных (>100 строк каждый) |
| Threading macro (`->`, `->>`) | < 10 мест во всём проекте |
| `atom` внутри reduce | 2 места |

---

## Приоритетные рекомендации

### P0 — Убрать java.util.Optional

Заменить все `Optional/of`, `Optional/empty`, `.isPresent`, `.get` на `nil`/`some?`/значение. Это самый неканоничный паттерн в проекте.

### P1 — Устранить дублирование task ↔ pending

Вынести `node`, `point`, `scan`, `place`, `nest`, `lines`, `render` в shared namespace `research.domain.brief`.

### P2 — Декомпозировать execute.clj

Извлечь общий блок запуска/сохранения/генерации в отдельные функции. Переключение pending/new должно отличаться только в 10 строках, а не в 100.

### P3 — Threading macros

Заменить цепочки `let` с переприсвоением на `->` / `->>` где применимо. Особенно в text-processing функциях.

### P4 — Удалить re-export из document.clj

14 `def`-алиасов в `pdf/document.clj` — удалить, пользователи должны подключать конкретные namespaces.

---

## Заключение

Архитектура проекта — каноничная: протоколы, records, иммутабельные данные, правильная namespace-иерархия, хороший инструментарий. Это выше среднего для Clojure-проектов.

Реализация содержит системные отклонения: `java.util.Optional` вместо `nil`, значительное дублирование кода, недостаточное использование threading macros, oversized-функции. Эти проблемы — техдолг, но не архитектурные дефекты. Исправление P0-P2 поднимет оценку с 6.5 до 8/10.
