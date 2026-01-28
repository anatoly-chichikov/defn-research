(ns research.pdf.document.citations
  (:require [clojure.string :as str]
            [research.domain.result :as result]
            [research.pdf.document.text :as text]))

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
               (assoc map (text/trim (result/url item))
                      (result/confidence item)))
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
        rule "https?://(?:[^()\\s\\[\\]]+|\\([^\\s\\[\\]]*\\))+"
        stash (fn [items]
                (let [num (Integer/parseInt (second items))
                      link (text/trim (nth items 2))]
                  (if (str/blank? link) (first items) (push num link))))
        stage (str/replace
               text
               (re-pattern (str "\\[\\[(\\d+)\\]\\]\\((" rule ")\\)"))
               stash)
        stage (str/replace
               stage
               (re-pattern (str "\\[(\\d+)\\]\\((" rule ")\\)"))
               stash)
        render (fn [items]
                 (let [num (Integer/parseInt (second items))
                       link (get refs num "")
                       link (if (str/blank? link) "" (text/trim link))]
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
        value (str/replace
               value
               #"\\n---\\n\\*Prepared using.*?\\*"
               "")]
    value))

(defn tables
  "Add columns classes to tables."
  [text]
  (let [mark (fn [table]
               (let [head (str/index-of table "</thead>")
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
  (let [mark (fn [items]
               (let [body (second items)
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
