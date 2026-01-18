(ns research.pdf.document
  (:require [clojure.java.shell :as shell]
            [clojure.string :as str]
            [jsonista.core :as json]
            [markdown.core :as md]
            [research.api.research :as research]
            [research.api.response :as response]
            [research.api.valyu :as valyu]
            [research.domain.pending :as pending]
            [research.domain.result :as result]
            [research.domain.session :as sess]
            [research.domain.task :as task]
            [research.pdf.style :as style]
            [research.storage.organizer :as organizer])
  (:import (java.nio.file Files LinkOption)
           (java.nio.file.attribute FileAttribute)
           (org.jsoup Jsoup)
           (org.jsoup.parser Parser)))

(defprotocol Signed
  "Object with author signature."
  (html [item] "Return HTML signature."))

(defprotocol Rendered
  "Object that can render document."
  (render [item] "Return HTML document."))

(defprotocol Exported
  "Object that can export to file."
  (save [item path] "Save PDF to path.")
  (page [item path] "Save HTML to path."))

(defrecord Signature [name service]
  Signed
  (html [_]
    (str
     (if (str/blank? name)
       (str "AI generated report with " service)
       (str "AI generated report for <span class=\"author\">"
            name
            "</span> with "
            service))
     "<br>May contain inaccuracies, please verify")))

(defn escape
  "Escape HTML special characters."
  [text]
  (-> text
      (str/replace "&" "&amp;")
      (str/replace "<" "&lt;")
      (str/replace ">" "&gt;")
      (str/replace "\"" "&quot;")))

(defn decode
  "Decode HTML entities."
  [text]
  (let [value (Parser/unescapeEntities (or text "") true)]
    (Parser/unescapeEntities value true)))

(defn heading
  "Return heading text with uppercase initial letter."
  [text]
  (let [text (str/trim (or text ""))
        size (count text)
        head (if (pos? size) (subs text 0 1) "")
        tail (if (> size 1) (subs text 1) "")
        head (str/upper-case head)]
    (str head tail)))

(defn normalize
  "Add blank lines before list markers."
  [text]
  (let [text (str/replace text #"\\n" "\n")]
    (str/replace text #"([^\n])\n((?:[*+-] |\d+\. ))" "$1\n\n$2")))

(defn listify
  "Convert inline prompts into markdown lists."
  [text]
  (let [text (str/replace text #"\\n" "\n")
        text (str/replace text #"\s+Research:" "\n\nResearch:")
        text (str/replace text #"(?m)(^|\n)(\s*)(\d+)\)" "$1$2$3.")
        text (str/replace text #"[ \t]+(\d+)[\.)]\s+" "\n$1. ")
        rows (str/split text #"\n" -1)
        rows (map (fn [row]
                    (if (re-find #"^\s*(?:\d+\.|[*+-])\s+" row)
                      row
                      (str/replace row #"[ \t]+([*+-])\s+" "\n$1 ")))
                  rows)
        text (str/join "\n" rows)
        text (str/replace text #"\n{3,}" "\n\n")]
    text))

(defn rule
  "Convert markdown separators to hr tags."
  [text]
  (str/replace text #"\n---\n" "\n\n<hr />\n\n"))

(defn nested
  "Normalize list indent to four spaces."
  [text]
  (str/replace text #"(?m)^( {1,3})([*+-] )" "    $2"))

(defn paragraphs
  "Wrap list item text in paragraph tags."
  [html]
  (let [doc (Jsoup/parseBodyFragment html)
        list (.select doc "li")]
    (doseq [item list]
      (let [block (.select
                   item
                   (str "> p, > ul, > ol, > table, > pre, > div, > h1, "
                        "> h2, > h3, > h4, > h5, > h6, > blockquote"))
            body (.html item)]
        (when (and (zero? (.size block)) (not (str/blank? body)))
          (.html item (str "<p>" body "</p>")))))
    (.html (.body doc))))

(defn trim
  "Remove utm parameters from URL."
  [text]
  (try
    (let [part (java.net.URI. text)
          query (.getQuery part)
          value (if (and query (not (str/blank? query)))
                  (let [pairs (map #(str/split % #"=") (str/split query #"&"))
                        items (filter
                               (fn [item]
                                 (not
                                  (str/starts-with?
                                   (str/lower-case (first item))
                                   "utm_")))
                               pairs)
                        line (str/join "&" (map #(str/join "=" %) items))]
                    (if (= line query)
                      text
                      (str (.getScheme part)
                           "://"
                           (.getAuthority part)
                           (.getPath part)
                           (if (str/blank? line) "" (str "?" line))
                           (if (.getFragment part)
                             (str "#" (.getFragment part))
                             ""))))
                  text)]
      value)
    (catch Exception _ text)))

(defn prune
  "Remove utm fragments from text."
  [text]
  (str/replace text #"(\?utm_[^\s\)\]]+|&utm_[^\s\)\]]+)" ""))

(defn clean
  "Remove tracking parameters from text URLs."
  [text]
  (let [pattern #"https?://[^\s\)\]]+"
        items (or (re-seq pattern text) [])
        value (reduce
               (fn [note link]
                 (str/replace note link (trim link)))
               text
               items)
        value (prune value)
        mask (re-pattern (str "(?<!\\])[ \\t]*\\("
                              "(?:https?://[^\\s\\)]+"
                              "|[A-Za-z0-9.-]+\\.[A-Za-z]{2,}[^\\s\\)]*)"
                              "\\)"))
        value (str/replace value mask "")]
    value))

(defn underscorify
  "Replace outer italic asterisks with underscores when bold ends the span."
  [text]
  (str/replace text #"(?<!\*)\*([^*\n]+?)\*\*([^\n]*?)\*\*\*" "_$1**$2**_"))

(defn label
  "Return cleaned source title."
  [item name]
  (let [text (str/replace
              (decode (str/trim (or (result/title item) "")))
              #"\s+"
              " ")
        link (trim (result/url item))
        host (response/domain link)
        text (if (and (= name "parallel")
                      (= (str/lower-case text) "fetched web page"))
               (if (str/blank? host) link host)
               text)
        text (if (str/blank? text) (if (str/blank? host) link host) text)]
    text))

(defn excerpt
  "Return cleaned excerpt text."
  [text]
  (let [text (decode (str/trim (or text "")))
        text (str/replace text #"\s+" " ")
        size 220
        text (if (> (count text) size)
               (str (subs text 0 (dec size)) "...")
               text)]
    text))

(declare resultmap provider)

(defn catalog
  "Collect sources from session tasks."
  [item]
  (let [list (sess/tasks (:session item))]
    (reduce
     (fn [list task]
       (let [[_ sources] (resultmap item task)
             name (provider task)]
         (reduce
          (fn [list item]
            (let [link (trim (result/url item))]
              (if (str/blank? link)
                list
                (conj list {:source item
                            :provider name}))))
          list
          sources)))
     []
     list)))

(defn section
  "Render sources section."
  [list]
  (if (seq list)
    (let [rows (reduce
                (fn [text item]
                  (let [source (:source item)
                        name (:provider item)
                        link (trim (result/url source))
                        title (label source name)
                        note (if (= name "valyu")
                               (excerpt (result/excerpt source))
                               "")
                        link (escape link)
                        title (escape title)
                        note (escape note)
                        row (str "<li class=\"ref-item\">"
                                 "<a class=\"ref-link\" href=\""
                                 link
                                 "\" target=\"_blank\">"
                                 title
                                 "</a>"
                                 (if (str/blank? note)
                                   ""
                                   (str "<div class=\"source-excerpt\">"
                                        note
                                        "</div>"))
                                 "</li>")]
                    (str text row)))
                ""
                list)]
      (str "<section class=\"references\">"
           "<h2>Sources</h2><ol class=\"ref-list\">"
           rows
           "</ol></section>"))
    ""))

(defn emojify
  "Wrap emoji characters in spans."
  [text]
  (let [pattern (re-pattern
                 (str "([\\x{1F000}-\\x{1FAFF}"
                      "\\x{2600}-\\x{27BF}"
                      "\\x{2300}-\\x{23FF}]"
                      "\\x{FE0F}?)"))]
    (str/replace text pattern "<span class=\"emoji\">$1</span>")))

(defn badge
  "Render confidence badge."
  [text]
  (if (str/blank? text)
    ""
    (str "<span class=\"confidence-badge confidence-"
         (str/lower-case text)
         "\"></span>")))

(defn references
  "Extract reference URLs from text."
  [text]
  (let [hit (re-find #"(?s)##\s*References\s*\n(.*?)(?=\n##|\Z)" text)
        body (if (and hit (> (count hit) 1)) (second hit) "")
        lines (str/split body #"\n")]
    (reduce
     (fn [map line]
       (let [match (re-find #"(\d+)\.\s+.*?(https?://\S+)" line)]
         (if match
           (assoc map (Integer/parseInt (second match)) (nth match 2))
           map)))
     {}
     lines)))

(defn citations
  "Convert [N] to links with confidence badges."
  [text sources]
  (let [refs (references text)
        map (reduce
             (fn [map item]
               (assoc map (trim (result/url item)) (result/confidence item)))
             {}
             sources)
        hold (atom [])
        mark (atom {})
        note (atom 0)
        push (fn [num link]
               (let [token (str "@@CITE" @note "@@")
                     badge (badge (get map link ""))]
                 (swap! note inc)
                 (swap! mark
                        assoc
                        token
                        (str "<a href=\""
                             link
                             "\" class=\"cite\" "
                             "target=\"_blank\">["
                             num
                             "]</a>"
                             badge))
                 (when-not (some #(= link %) @hold)
                   (swap! hold conj link))
                 token))
        stash (fn [items]
                (let [num (Integer/parseInt (second items))
                      link (trim (nth items 2))]
                  (if (str/blank? link) (first items) (push num link))))
        stage (str/replace text #"\[\[(\d+)\]\]\((https?://[^)\s]+)\)" stash)
        stage (str/replace stage #"\[(\d+)\]\((https?://[^)\s]+)\)" stash)
        render (fn [items]
                 (let [num (Integer/parseInt (second items))
                       link (get refs num "")
                       link (if (str/blank? link) "" (trim link))]
                   (if (str/blank? link) (first items) (push num link))))
        value (str/replace stage #"\[(\d+)\]" render)]
    [value @hold @mark]))

(defn strip
  "Remove trailing sources section."
  [text]
  (let [rows (str/split text #"\n")
        size (count rows)
        idx (loop [i 0 hit -1]
              (if (< i size)
                (let [line (str/trim (nth rows i))
                      label (str/lower-case (str/replace line #"^#+\s*" ""))
                      mark (and (str/starts-with? line "#")
                                (#{"source"
                                   "sources"
                                   "reference"
                                   "references"} label))]
                  (recur (inc i) (if mark i hit)))
                hit))
        value (if (>= idx 0)
                (let [tail (drop (inc idx) rows)
                      later (some
                             (fn [line]
                               (str/starts-with? (str/trim line) "#"))
                             tail)
                      body (str/join "\n" tail)]
                  (if (and (not later) (re-find #"https?://" body))
                    (str/join "\n" (take idx rows))
                    text))
                text)
        value (clojure.string/replace
               value
               #"\\n---\\n\\*Prepared using.*?\\*"
               "")]
    value))

(defn tables
  "Add columns classes to tables."
  [text]
  (let [mark (fn [table] (let [head (str/index-of table "</thead>")
                               head (if head head (str/index-of table "</tr>"))
                               head (if head head (count table))
                               head (subs table 0 head)
                               cols (count (re-seq #"<th[^>]*>" head))
                               cols (if (pos? cols)
                                      cols
                                      (count (re-seq #"<td[^>]*>" head)))]
                           (str/replace
                            table
                            "<table>"
                            (str "<table class=\"cols-" cols "\">"))))]
    (str/replace text #"<table>.*?</table>" mark)))

(defn codeindent
  "Add hanging indent spans for code blocks."
  [text]
  (let [mark (fn [items] (let [body (second items)
                               lines (str/split body #"\n")
                               rows (map
                                     (fn [line]
                                       (if (str/blank? line)
                                         line
                                         (let [pad (- (count line)
                                                      (count (str/triml line)))
                                               hang 2
                                               indent (+ pad hang)
                                               style (str "padding-left: "
                                                          indent
                                                          "ch; text-indent: -"
                                                          hang
                                                          "ch; display: block;")
                                               value (str/triml line)]
                                           (str "<span class=\"code-line\" "
                                                "style=\""
                                                style
                                                "\">"
                                                value
                                                "</span>"))))
                                     lines)]
                           (str "<pre><code>"
                                (apply str rows)
                                "</code></pre>")))]
    (str/replace text #"<pre><code>(.*?)</code></pre>" mark)))

(defn stars
  "Replace star ratings with fractions."
  [text]
  (let [mark (fn [items]
               (let [value (if (string? items) items (first items))
                     size (count value)
                     sum (reduce (fn [sum item]
                                   (if (= item \u2605) (inc sum) sum))
                                 0
                                 value)]
                 (str sum "/" size)))]
    (str/replace text #"[★☆]+" mark)))

(defn backslash
  "Unescape encoded backslashes in HTML."
  [text]
  (str/replace text "&amp;#92;" "&#92;"))

(defn env
  "Return environment value by key."
  [key]
  (System/getenv key))

(defn emit
  "Render PDF using WeasyPrint."
  [html path]
  (let [tmp (Files/createTempFile "report" ".html" (make-array FileAttribute 0))
        _ (spit (.toFile tmp) html :encoding "UTF-8")
        vars (into {} (System/getenv))
        home (or (get vars "DYLD_FALLBACK_LIBRARY_PATH") "")
        list [home "/opt/homebrew/lib" "/usr/local/lib"]
        list (filter #(not (str/blank? %)) list)
        link (str/join ":" list)
        vars (assoc vars "DYLD_FALLBACK_LIBRARY_PATH" link)
        res (shell/sh
             "uv"
             "run"
             "--with"
             "weasyprint"
             "python"
             "-m"
             "weasyprint"
             (.toString tmp)
             (.toString path)
             :env vars)
        code (:exit res)
        _ (Files/deleteIfExists tmp)]
    (if (zero? code)
      path
      (throw (ex-info "Weasyprint failed" {:code code
                                           :out (:out res)
                                           :err (:err res)})))))

(defn author
  "Return report author from env."
  []
  (or (env "REPORT_FOR") ""))

(defn service
  "Return service name from latest task."
  [item]
  (let [list (sess/tasks item) last (last list)]
    (if last (task/service last) "parallel.ai")))

(defn coverimage
  "Render cover image html."
  [item]
  (let [cover (:cover item)]
    (if (and (.isPresent cover)
             (Files/exists (.get cover) (make-array LinkOption 0)))
      (str "<div class=\"cover-image\"><img src=\""
           (.toString (.toUri (.get cover)))
           "\" alt=\"Cover\" /></div>")
      "")))

(defn briefpath
  "Return brief file path."
  [item]
  (let [sess (:session item)
        root (:root item)
        org (organizer/organizer root)
        name (organizer/name
              org
              (sess/created sess)
              (sess/topic sess)
              (sess/id sess))
        list (sess/tasks sess)
        tail (last list)
        hold (sess/pending sess)
        slot (if (and (not tail) (.isPresent hold)) (.get hold) nil)
        service (if tail
                  (provider tail)
                  (if slot (pending/provider slot) "provider"))
        tag (organizer/slug service)
        tag (if (str/blank? tag) "provider" tag)
        base (.resolve root name)
        brief (.resolve base (str "brief-" tag ".md"))
        input (.resolve base (str "input-" tag ".md"))
        path (if (Files/exists brief (make-array LinkOption 0)) brief input)]
    (.toFile path)))

(defn brief
  "Render brief section."
  [item]
  (let [list (sess/tasks (:session item))
        head (first list)
        path (briefpath item)
        text (if (and head (.exists path))
               (slurp path :encoding "UTF-8")
               (if head (task/query head) ""))
        text (listify text)
        text (normalize text)
        text (rule text)
        text (stars text)
        html (md/md-to-html-string text)
        html (tables html)
        html (codeindent html)
        html (backslash html)]
    (if (str/blank? html)
      ""
      (str "<div class=\"brief\"><div class=\"container\">"
           "<h1>Exploration Brief</h1>"
           "<div class=\"query\">"
           html
           "</div></div></div>"))))

(defn provider
  "Return provider slug from task service."
  [item]
  (let [name (task/service item)]
    (if (str/ends-with? name ".ai") (first (str/split name #"\.")) name)))

(defn raw
  "Load raw response from output folder."
  [item task]
  (let [root (:root item)
        org (organizer/organizer root)
        name (organizer/name
              org
              (sess/created (:session item))
              (sess/topic (:session item))
              (sess/id (:session item)))
        base (.resolve root name)
        path (if task
               (let [tag (organizer/slug (provider task))
                     tag (if (str/blank? tag) "provider" tag)]
                 (.resolve base (str "response-" tag ".json")))
               (.resolve root "missing.json"))]
    (if (and task (Files/exists path (make-array LinkOption 0)))
      (json/read-value
       (.toFile path)
       (json/object-mapper {:decode-key-fn keyword}))
      {})))

(defn images
  "Append images block to markdown."
  [item text raw task]
  (let [items (or (:images raw) [])
        root (:root item)
        org (organizer/organizer root)
        name (organizer/name
              org
              (sess/created (:session item))
              (sess/topic (:session item))
              (sess/id (:session item)))
        base (.resolve root name)
        tag (organizer/slug (provider task))
        tag (if (str/blank? tag) "provider" tag)
        folder (.resolve
                base
                (str "images-" tag))
        lines (reduce
               (fn [list item]
                 (let [link (or (:image_url item) "")
                       title (or (:title item) "Chart")
                       code (or (:image_id item) "")
                       path (if (str/blank? link)
                              ""
                              (try (or (.getPath (java.net.URI. link)) "")
                                   (catch Exception _ "")))
                       part (second (re-find #"(\\.[^./]+)$" path))
                       part (if (str/blank? part) ".png" part)
                       file (if (str/blank? code)
                              ""
                              (.resolve folder (str code part)))
                       link (if (and (not (str/blank? code))
                                     (not (str/blank? (str file)))
                                     (Files/exists
                                      file
                                      (make-array LinkOption 0)))
                              (.toString (.toUri file))
                              link)
                       add (and (not (str/blank? link))
                                (not (str/includes? text link)))]
                   (if add (conj list (str "![" title "](" link ")")) list)))
               []
               items)
        block (if (seq lines) (str "## Images\n\n" (str/join "\n" lines)) "")
        rows (str/split text #"\n")
        size (count rows)
        idx (loop [i 0 hit -1]
              (if (< i size)
                (let [line (str/trim (nth rows i))
                      label (str/lower-case (str/replace line #"^#+\s*" ""))
                      mark (and (str/starts-with? line "#")
                                (#{"source"
                                   "sources"
                                   "reference"
                                   "references"} label))]
                  (recur (inc i) (if mark i hit)))
                hit))
        value (if (and (seq block) (>= idx 0))
                (let [head (str/join "\n" (take idx rows))
                      tail (str/join "\n" (drop idx rows))]
                  (str (str/trimr head) "\n\n" block "\n\n" (str/triml tail)))
                (if (seq block) (str (str/trimr text) "\n\n" block) text))]
    value))

(defn responsemap
  "Build response from raw map and task."
  [item raw task]
  (let [name (provider task)]
    (if (= name "valyu")
      (let [output (:output raw)
            text (if (map? output)
                   (or (:markdown output) (:content output) "")
                   (or output ""))
            text (images item text raw task)
            sources (or (:sources raw) [])
            val (valyu/valyu {:mode "basis"})
            base (research/basis val sources)
            state (or (:status raw) "completed")
            status (if (map? state) (or (:value state) state) state)
            code (or (:deepresearch_id raw) (:id raw) "")]
        (response/response {:id code
                            :status status
                            :output text
                            :basis base
                            :raw raw}))
      (let [output (:output raw)
            text (if (map? output) (or (:content output) "") "")
            basis (if (map? output) (or (:basis output) []) [])
            run (or (:run raw) {})
            code (if (map? run) (or (:run_id run) "") "")
            status (if (map? run) (or (:status run) "completed") "completed")]
        (response/response {:id code
                            :status status
                            :output text
                            :basis basis
                            :raw raw})))))

(defn resultmap
  "Return text and sources for task."
  [item task]
  (let [raw (raw item task)]
    (if (seq raw)
      (let [resp (responsemap item raw task)]
        [(response/text resp) (response/sources resp)])
      (let [value (task/result task)]
        [(result/summary value) (result/sources value)]))))

(defn taskhtml
  "Render task section HTML."
  [item task]
  (let [[text sources] (resultmap item task)
        text (clean text)
        text (underscorify text)
        text (stars text)
        text (emojify text)
        [text urls mark] (citations text sources)
        text (strip text)
        text (nested text)
        text (normalize text)
        text (rule text)
        html (if (str/blank? text) "" (md/md-to-html-string text))
        html (tables html)
        html (codeindent html)
        html (paragraphs html)
        html (backslash html)
        html (reduce-kv
              (fn [note token link]
                (str/replace note token link))
              html
              mark)
        body (if (str/blank? html)
               ""
               (str "<div class=\"synthesis\">" html "</div>"))]
    [(str "<section>" body "<div class=\"divider\"></div></section>") urls]))

(defn tasks
  "Render all tasks into HTML sections."
  [item]
  (let [list (sess/tasks (:session item))
        items (reduce
               (fn [note task]
                 (let [[html urls] (taskhtml item task)
                       list (concat (second note) urls)]
                   [(str (first note) html) (vec (distinct list))]))
               ["" []]
               list)]
    items))

(defrecord Document [session palette cover root]
  Rendered
  (render [item] (let [[content _] (tasks item)
                       list (catalog item)
                       extra (section list)
                       sign (->Signature (author) (service session))
                       note (html sign)
                       css (style/css (style/style palette))
                       form java.time.format.DateTimeFormatter/ISO_LOCAL_DATE
                       stamp (.format (sess/created session) form)]
                   (str "<!DOCTYPE html><html lang=\"en\"><head>"
                        "<meta charset=\"UTF-8\" />"
                        "<title>"
                        (escape (heading (sess/topic session)))
                        "</title><style>"
                        css
                        "</style></head><body>"
                        "<div class=\"page-footer\">"
                        note
                        "</div>"
                        "<div class=\"intro\">"
                        (coverimage item)
                        "<div class=\"intro-content\"><h1>"
                        (escape (heading (sess/topic session)))
                        "</h1><div class=\"meta\"><p class=\"subtitle\">"
                        note
                        "</p><p class=\"date\">"
                        stamp
                        "</p></div></div></div>"
                        (brief item)
                        "<div class=\"container content\">"
                        "<div class=\"tasks\">"
                        content
                        "</div>"
                        "</div><div class=\"container\">"
                        extra
                        "</div></body></html>")))
  Exported
  (save [item path]
    (let [html (render item)]
      (emit html path)))
  (page [item path]
    (let [html (render item)
          _ (spit (.toFile path) html :encoding "UTF-8")]
      path))
  Object
  (toString [item] (render item)))

(defn document
  "Create document instance."
  [session palette cover root]
  (->Document session palette cover root))
