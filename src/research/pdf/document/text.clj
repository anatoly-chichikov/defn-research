(ns research.pdf.document.text
  (:require [clojure.string :as str]
            [research.api.response :as response]
            [research.domain.result :as result])
  (:import (org.jsoup Jsoup)
           (org.jsoup.parser Parser)))

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

(defn item
  "Normalize brief item."
  [node]
  (let [text (str (or (:text node) node))
        items (or (:items node) [])
        items (mapv item items)
        text (str/trim text)
        items (vec (remove
                    (fn [item]
                      (and (str/blank? (:text item))
                           (empty? (:items item))))
                    items))]
    {:text text
     :items items}))

(defn outline
  "Render nested list html."
  [items]
  (let [items (mapv item (or items []))
        rows (reduce
              (fn [list entry]
                (let [text (escape (str (or (:text entry) "")))
                      nest (outline (:items entry))
                      body (if (str/blank? nest) "" nest)
                      part (str "<li>" text body "</li>")]
                  (conj list part)))
              []
              items)
        body (str/join "" rows)]
    (if (str/blank? body) "" (str "<ol>" body "</ol>"))))

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
        mask (re-pattern
              (str "(?<!\\])[ \t]*\\("
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
