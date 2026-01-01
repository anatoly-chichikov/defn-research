(ns research.pdf.document-test
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is]]
            [research.domain.result :as result]
            [research.domain.session :as session]
            [research.domain.task :as task]
            [research.pdf.document :as document]
            [research.pdf.palette :as palette]
            [research.storage.organizer :as organizer])
  (:import (java.nio.file Files Paths)
           (java.nio.file.attribute FileAttribute)
           (java.util Optional)))

(defn token
  "Return deterministic token string."
  [rng size base span]
  (let [build (StringBuilder.)]
    (dotimes [_ size]
      (let [pick (.nextInt rng span)
            code (+ base pick)]
        (.append build (char code))))
    (.toString build)))

(defn uuid
  "Return deterministic UUID string."
  [rng]
  (str (java.util.UUID. (.nextLong rng) (.nextLong rng))))

(deftest the-document-render-contains-doctype
  (let [rng (java.util.Random. 18001)
        topic (token rng 6 1040 32)
        item (session/session {:topic topic
                               :tasks []
                               :created (session/format (session/now))})
        root (Paths/get "output" (make-array String 0))
        doc (document/document item (palette/palette) (Optional/empty) root)
        html (document/render doc)]
    (is (str/includes? html "<!DOCTYPE html>")
        "Rendered document did not contain DOCTYPE")))

(deftest the-document-render-contains-topic
  (let [rng (java.util.Random. 18003)
        topic (token rng 6 12354 32)
        item (session/session {:topic topic
                               :tasks []
                               :created (session/format (session/now))})
        root (Paths/get "output" (make-array String 0))
        doc (document/document item (palette/palette) (Optional/empty) root)
        html (document/render doc)]
    (is (str/includes? html topic)
        "Rendered document did not contain topic")))

(deftest the-document-renders-exploration-brief-title
  (let [rng (java.util.Random. 18005)
        query (token rng 6 1040 32)
        task (task/task {:query query
                         :status "completed"
                         :result nil
                         :service "valyu.ai"
                         :created (task/format (task/now))})
        entry (task/data task)
        item (session/session {:topic query
                               :tasks [entry]
                               :created (session/format (session/now))})
        root (Paths/get "output" (make-array String 0))
        doc (document/document item (palette/palette) (Optional/empty) root)
        html (document/render doc)]
    (is (str/includes? html "<h1>Exploration Brief</h1>")
        "Exploration Brief title was missing")))

(deftest the-document-includes-palette-colors
  (let [rng (java.util.Random. 18007)
        topic (token rng 6 12354 32)
        item (session/session {:topic topic
                               :tasks []
                               :created (session/format (session/now))})
        root (Paths/get "output" (make-array String 0))
        doc (document/document item (palette/palette) (Optional/empty) root)
        html (document/render doc)
        colors ["#F6EFE3"
                "#1C2430"
                "#193D5E"
                "#3A5F88"
                "#6B645A"
                "#E3D9C6"
                "#D04A35"
                "#1C2833"
                "#DDD5C5"
                "#BFB5A3"]]
    (is (every? #(str/includes? html %) colors)
        "Rendered document did not include Hokusai palette colors")))

(deftest the-document-renders-author-name
  (let [rng (java.util.Random. 18009)
        name (token rng 6 1040 32)
        service (token rng 4 12354 32)
        value (token rng 5 880 32)
        result (result/->Result value [])
        task (task/task {:query value
                         :status "completed"
                         :result (result/data result)
                         :language value
                         :service service
                         :created (task/format (task/now))})
        entry (task/data task)
        item (session/session {:topic value
                               :tasks [entry]
                               :created (session/format (session/now))})]
    (with-redefs [document/env (fn [_] name)]
      (let [root (Paths/get "output" (make-array String 0))
            doc (document/document item (palette/palette) (Optional/empty) root)
            html (document/render doc)]
        (is (str/includes? html name) "Author name was missing")))))

(deftest the-document-renders-service-name
  (let [rng (java.util.Random. 18011)
        name (token rng 6 1040 32)
        service "parallel.ai"
        value (token rng 5 880 32)
        result (result/->Result value [])
        task (task/task {:query value
                         :status "completed"
                         :result (result/data result)
                         :language value
                         :service service
                         :created (task/format (task/now))})
        entry (task/data task)
        item (session/session {:topic value
                               :tasks [entry]
                               :created (session/format (session/now))})]
    (with-redefs [document/env (fn [_] name)]
      (let [root (Paths/get "output" (make-array String 0))
            doc (document/document item (palette/palette) (Optional/empty) root)
            html (document/render doc)]
        (is (str/includes? html service) "Service name was missing")))))

(deftest the-document-renders-parallel-domain
  (let [rng (java.util.Random. 18013)
        name (token rng 6 1040 32)
        value (token rng 5 880 32)
        result (result/->Result value [])
        task (task/task {:query value
                         :status "completed"
                         :result (result/data result)
                         :language value
                         :service "parallel.ai"
                         :created (task/format (task/now))})
        entry (task/data task)
        item (session/session {:topic value
                               :tasks [entry]
                               :created (session/format (session/now))})]
    (with-redefs [document/env (fn [_] name)]
      (let [root (Paths/get "output" (make-array String 0))
            doc (document/document item (palette/palette) (Optional/empty) root)
            html (document/render doc)]
        (is (str/includes? html "parallel.ai")
            "Parallel domain was missing")))))

(deftest the-document-renders-valyu-domain
  (let [rng (java.util.Random. 18015)
        name (token rng 6 1040 32)
        value (token rng 5 880 32)
        result (result/->Result value [])
        task (task/task {:query value
                         :status "completed"
                         :result (result/data result)
                         :language value
                         :service "valyu.ai"
                         :created (task/format (task/now))})
        entry (task/data task)
        item (session/session {:topic value
                               :tasks [entry]
                               :created (session/format (session/now))})]
    (with-redefs [document/env (fn [_] name)]
      (let [root (Paths/get "output" (make-array String 0))
            doc (document/document item (palette/palette) (Optional/empty) root)
            html (document/render doc)]
        (is (str/includes? html "valyu.ai") "Valyu domain was missing")))))

(deftest the-document-omits-author-when-missing
  (let [rng (java.util.Random. 18017)
        service (token rng 4 12354 32)
        value (token rng 5 880 32)
        result (result/->Result value [])
        task (task/task {:query value
                         :status "completed"
                         :result (result/data result)
                         :language value
                         :service service
                         :created (task/format (task/now))})
        entry (task/data task)
        item (session/session {:topic value
                               :tasks [entry]
                               :created (session/format (session/now))})]
    (with-redefs [document/env (fn [_] "")]
      (let [root (Paths/get "output" (make-array String 0))
            doc (document/document item (palette/palette) (Optional/empty) root)
            html (document/render doc)]
        (is (not (str/includes? html "<span class=\"author\">"))
            "Author span was present")))))

(deftest the-document-inserts-blank-line-before-hyphen-list
  (let [rng (java.util.Random. 18019)
        head (token rng 6 1040 32)
        item (token rng 6 12354 32)
        tail (token rng 6 256 64)
        text (str head ":\n- " item "\n- " tail)
        result (document/normalize text)]
    (is (str/includes? result ":\n\n-")
        "Normalized text did not insert blank line before hyphen list")))

(deftest the-document-normalize-converts-escaped-newlines
  (let [text "Язык\\nответа"
        result (document/normalize text)]
    (is (= "Язык\nответа" result)
        "Escaped newlines were not converted")))

(deftest the-document-wraps-emoji-characters
  "Ensure emoji characters are wrapped with emoji span."
  (let [rng (java.util.Random. 18021)
        head (token rng 6 1040 32)
        tail (token rng 6 12354 32)
        mark (String. (Character/toChars 9989))
        text (str head " " mark " " tail)
        html (document/emojify text)
        done (and (str/includes? html "class=\"emoji\"")
                  (str/includes? html mark))]
    (is done "Emoji span was not rendered")))

(deftest the-document-listify-converts-numbered-prompts
  (let [rng (java.util.Random. 18029)
        head (token rng 6 1040 32)
        left (token rng 6 12354 32)
        right (token rng 6 256 64)
        one (inc (.nextInt rng 8))
        two (+ one (inc (.nextInt rng 8)))
        text (str head " " one ") " left " " two ". " right)
        item (document/listify text)
        mark (and (str/includes? item (str "\n" one ". "))
                  (str/includes? item (str "\n" two ". ")))]
    (is mark "Numbered prompts were not converted")))

(deftest the-document-listify-converts-inline-bullets
  (let [rng (java.util.Random. 18030)
        head (token rng 6 1040 32)
        left (token rng 6 12354 32)
        mid (token rng 6 256 64)
        tail (token rng 6 880 32)
        text (str head " - " left " + " mid " * " tail)
        item (document/listify text)
        mark (and (str/includes? item "\n- ")
                  (str/includes? item "\n+ ")
                  (str/includes? item "\n* "))]
    (is mark "Inline bullets were not converted")))

(deftest the-document-wraps-list-items-in-paragraphs
  (let [rng (java.util.Random. 18027)
        head (token rng 6 1040 32)
        tail (token rng 6 12354 32)
        html (str "<ul><li><strong>"
                  head
                  "</strong> "
                  tail
                  "</li></ul>")
        item (document/paragraphs html)]
    (is (some? (re-find #"(?s)<li[^>]*>.*<p>" item))
        "List items were not wrapped")))

(deftest the-document-render-contains-task-query
  (let [rng (java.util.Random. 18021)
        query (token rng 6 12354 32)
        task (task/task {:query query
                         :status "completed"
                         :result nil
                         :created (task/format (task/now))})
        entry (task/data task)
        item (session/session {:topic "T"
                               :tasks [entry]
                               :created (session/format (session/now))})
        root (Paths/get "output" (make-array String 0))
        doc (document/document item (palette/palette) (Optional/empty) root)
        html (document/render doc)]
    (is (str/includes? html query)
        "Rendered document did not contain task query")))

(deftest the-document-render-contains-synthesis
  (let [rng (java.util.Random. 18023)
        summary (token rng 6 12354 32)
        result (result/->Result summary [])
        task (task/task {:query "q"
                         :status "completed"
                         :result (result/data result)
                         :created (task/format (task/now))})
        entry (task/data task)
        item (session/session {:topic "T"
                               :tasks [entry]
                               :created (session/format (session/now))})
        root (Paths/get "output" (make-array String 0))
        doc (document/document item (palette/palette) (Optional/empty) root)
        html (document/render doc)]
    (is (str/includes? html summary)
        "Rendered document did not contain synthesis")))

(deftest the-document-renders-confidence-badge
  (let [rng (java.util.Random. 18025)
        text (token rng 5 1040 32)
        url (str "https://example.com/"
                 (.nextInt rng 1000)
                 "?utm_source=valyu.ai&utm_medium=referral")
        source (result/->Source text url text "High")
        summary (str text "\n\n## References\n1. " url)
        result (result/->Result summary [source])
        task (task/task {:query text
                         :status "completed"
                         :result (result/data result)
                         :created (task/format (task/now))})
        entry (task/data task)
        item (session/session {:topic text
                               :tasks [entry]
                               :created (session/format (session/now))})
        root (Paths/get "output" (make-array String 0))
        doc (document/document item (palette/palette) (Optional/empty) root)
        html (document/render doc)]
    (is (str/includes? html "confidence-high")
        "Confidence badge was missing")))

(deftest the-document-tables-adds-column-class
  (let [rng (java.util.Random. 18026)
        head (token rng 4 1040 32)
        body (token rng 4 12354 32)
        html (str "<table><thead><tr><th>"
                  head
                  "</th><th>"
                  head
                  "</th></tr></thead><tbody><tr><td>"
                  body
                  "</td><td>"
                  body
                  "</td></tr></tbody></table>")
        item (document/tables html)]
    (is (str/includes? item "class=\"cols-3\"")
        "Tables did not add column class")))

(deftest the-document-strips-utm-parameters
  (let [rng (java.util.Random. 18027)
        slug (token rng 5 1040 32)
        link (str "https://example.com/"
                  (.nextInt rng 1000)
                  "?utm_source=valyu.ai&utm_medium=referral&x="
                  (.nextInt rng 9))
        summary (str "Sources\n1. " link "\n2. " slug)
        result (result/->Result summary [])
        task (task/task {:query slug
                         :status "completed"
                         :result (result/data result)
                         :created (task/format (task/now))})
        entry (task/data task)
        item (session/session {:topic slug
                               :tasks [entry]
                               :created (session/format (session/now))})
        root (Paths/get "output" (make-array String 0))
        doc (document/document item (palette/palette) (Optional/empty) root)
        html (document/render doc)]
    (is (not (str/includes? html "utm_source"))
        "utm parameters were not stripped from document")))

(deftest the-document-escapes-html
  (let [topic "<script>alert('xss')</script>"
        item (session/session {:topic topic
                               :tasks []
                               :created (session/format (session/now))})
        root (Paths/get "output" (make-array String 0))
        doc (document/document item (palette/palette) (Optional/empty) root)
        html (document/render doc)]
    (is (str/includes? html "&lt;script&gt;")
        "Rendered document did not escape HTML")))

(deftest the-document-html-creates-file
  (let [rng (java.util.Random. 18029)
        path (.resolve (Files/createTempDirectory
                        "doc"
                        (make-array FileAttribute 0))
                       (str "test-" (uuid rng) ".html"))
        item (session/session {:topic "T"
                               :tasks []
                               :created (session/format (session/now))})
        root (Paths/get "output" (make-array String 0))
        doc (document/document item (palette/palette) (Optional/empty) root)]
    (document/page doc path)
    (is (Files/exists path (make-array java.nio.file.LinkOption 0))
        "HTML file was not created")))

(deftest the-document-normalize-adds-blank-line-before-list
  (let [rng (java.util.Random. 18031)
        text (str "**"
                  (token rng 6 12354 32)
                  "**\n* "
                  (token rng 4 1040 32))
        item (document/normalize text)]
    (is (str/includes? item "**\n\n* ")
        "Normalize did not add blank line before list")))

(deftest the-document-normalize-preserves-existing-blank-lines
  (let [rng (java.util.Random. 18033)
        text (str "**"
                  (token rng 6 12354 32)
                  "**\n\n* "
                  (token rng 4 1040 32))
        item (document/normalize text)]
    (is (= text item) "Normalize modified already correct text")))

(deftest the-document-normalize-handles-multiple-lists
  (let [rng (java.util.Random. 18035)
        text (str "**"
                  (token rng 6 12354 32)
                  "**\n* 一\n**第二**\n* 二")
        item (document/normalize text)]
    (is (= 2 (count (re-seq #"\n\n\* " item)))
        "Normalize did not fix all lists")))

(deftest the-document-normalize-ignores-lists-after-blank-line
  (let [rng (java.util.Random. 18037)
        text (str "段落-"
                  (token rng 6 12354 32)
                  "\n\n* すでに正しい")
        item (document/normalize text)]
    (is (= 0 (count (re-seq #"\n\n\n" item)))
        "Normalize added extra blank lines")))

(deftest the-document-rule-replaces-separators
  (let [rng (java.util.Random. 18038)
        head (token rng 6 1040 32)
        tail (token rng 6 12354 32)
        text (str head "\n---\n" tail)
        item (document/rule text)]
    (is (str/includes? item "<hr />") "Rule did not convert separator")))

(deftest the-document-citations-convert-references
  (let [rng (java.util.Random. 18039)
        mark (uuid rng)
        text (str "テキスト-"
                  mark
                  " [1]\n\n## References\n\n1. タイトル https://example.com/"
                  mark)
        item (first (document/citations text []))
        link (str "<a href=\"https://example.com/" mark "\" class=\"cite\"")]
    (is (str/includes? item link)
        "Citations did not create link from reference")))

(deftest the-document-citations-extract-urls
  (let [rng (java.util.Random. 18041)
        mark (uuid rng)
        text (str "参照 [1]\n\n## References\n\n1. ソース https://test.jp/"
                  mark)
        urls (second (document/citations text []))]
    (is (= 1 (count urls)) "Citations did not extract URL")))

(deftest the-document-references-extract-mapping
  (let [rng (java.util.Random. 18043)
        mark (uuid rng)
        text (str "## References\n\n1. 一 https://a.jp/"
                  mark
                  "\n2. 二 https://b.jp/"
                  mark)
        refs (document/references text)]
    (is (= 2 (count refs)) "References did not extract all entries")))

(deftest the-document-path-returns-session-based-path
  (let [rng (java.util.Random. 18045)
        ident (uuid rng)
        short (first (str/split ident #"-"))
        item (session/session {:id ident
                               :topic "T"
                               :tasks []
                               :created (session/format (session/now))})
        root (Paths/get "output" (make-array String 0))
        doc (document/document item (palette/palette) (Optional/empty) root)
        path (document/briefpath doc)]
    (is (str/includes? (str path) short)
        "Path did not contain short session ID")))

(deftest the-document-brief-reads-from-file
  (let [rng (java.util.Random. 18047)
        ident (uuid rng)
        mark (str "マーカー-" (uuid rng))
        path (.resolve (Files/createTempDirectory
                        "brief"
                        (make-array FileAttribute 0))
                       (str ident ".md"))
        _ (spit (.toFile path) mark :encoding "UTF-8")
        task (task/task {:query "fallback"
                         :status "completed"
                         :result nil
                         :created (task/format (task/now))})
        entry (task/data task)
        item (session/session {:id ident
                               :topic "T"
                               :tasks [entry]
                               :created (session/format (session/now))})
        root (Paths/get "output" (make-array String 0))
        doc (document/document item (palette/palette) (Optional/empty) root)
        html (with-redefs [document/briefpath (fn [_] (.toFile path))]
               (document/brief doc))]
    (is (str/includes? html mark) "Brief did not read from file")))

(deftest the-document-brief-falls-back-to-query
  (let [rng (java.util.Random. 18049)
        mark (str "クエリ-" (uuid rng))
        task (task/task {:query mark
                         :status "completed"
                         :result nil
                         :created (task/format (task/now))})
        entry (task/data task)
        item (session/session {:topic "T"
                               :tasks [entry]
                               :created (session/format (session/now))})
        root (Paths/get "output" (make-array String 0))
        doc (document/document item (palette/palette) (Optional/empty) root)
        html (document/brief doc)]
    (is (str/includes? html mark) "Brief did not fall back to query")))

(deftest the-document-nested-converts-single-space-indent
  (let [rng (java.util.Random. 18051)
        mark (uuid rng)
        text (str "* **親-" mark ":**\n * **子要素:** 内容")
        item (document/nested text)]
    (is (str/includes? item "    * ")
        "Nested did not convert single space to four spaces")))

(deftest the-document-nested-preserves-four-space-indent
  (let [rng (java.util.Random. 18053)
        mark (uuid rng)
        text (str "* **親-" mark ":**\n    * **子:** 内容")
        item (document/nested text)]
    (is (= text item) "Nested modified already correct indentation")))

(deftest the-document-nested-handles-multiple-levels
  (let [rng (java.util.Random. 18055)
        mark (uuid rng)
        text (str "* 一-" mark "\n  * 二\n   * 三")
        item (document/nested text)]
    (is (= 2 (count (re-seq #"\n    \* " item)))
        "Nested did not normalize all indented items")))

(deftest the-document-normalize-adds-blank-line-before-numbered-list
  (let [rng (java.util.Random. 18057)
        mark (uuid rng)
        text (str "調査-" mark ":\n1. 最初の項目")
        item (document/normalize text)]
    (is (str/includes? item ":\n\n1. ")
        "Normalize did not add blank line before numbered list")))

(deftest the-document-normalize-handles-mixed-lists
  (let [rng (java.util.Random. 18059)
        mark (uuid rng)
        text (str "テキスト-"
                  mark
                  "\n* 箇条書き\n別のテキスト\n1. 番号付き")
        item (document/normalize text)]
    (is (= 2 (count (re-seq #"\n\n" item)))
        "Normalize did not add blank lines before both list types")))

(deftest the-document-brief-normalizes-numbered-lists
  (let [rng (java.util.Random. 18061)
        mark (uuid rng)
        query (str "調査-" mark ":\n1. 最初\n2. 二番目")
        task (task/task {:query query
                         :status "completed"
                         :result nil
                         :created (task/format (task/now))})
        entry (task/data task)
        item (session/session {:topic "T"
                               :tasks [entry]
                               :created (session/format (session/now))})
        root (Paths/get "output" (make-array String 0))
        doc (document/document item (palette/palette) (Optional/empty) root)
        html (document/brief doc)]
    (is (str/includes? html "<ol>")
        "Brief did not render numbered list as <ol>")))

(deftest the-document-brief-normalizes-bullet-lists
  (let [rng (java.util.Random. 18062)
        head (token rng 6 1040 32)
        left (token rng 6 12354 32)
        mid (token rng 6 256 64)
        tail (token rng 6 880 32)
        query (str head ":\n- " left "\n+ " mid "\n* " tail)
        task (task/task {:query query
                         :status "completed"
                         :result nil
                         :created (task/format (task/now))})
        entry (task/data task)
        item (session/session {:topic "T"
                               :tasks [entry]
                               :created (session/format (session/now))})
        root (Paths/get "output" (make-array String 0))
        doc (document/document item (palette/palette) (Optional/empty) root)
        html (document/brief doc)
        mark (count (re-seq #"<li" html))]
    (is (= 3 mark) "Brief did not render bullet list items")))

(deftest the-document-strips-sources-section
  (let [rng (java.util.Random. 18063)
        head (token rng 6 1040 32)
        link (str "https://example.com/" (.nextInt rng 1000))
        text (str head "\n\n## Sources\n1. " link "\n2. " link)
        item (document/strip text)]
    (is (not (str/includes? item "Sources"))
        "Sources section was not stripped")))

(deftest the-document-keeps-sources-without-links
  (let [rng (java.util.Random. 18065)
        head (token rng 6 1040 32)
        note (token rng 5 880 32)
        text (str head "\n\n## Sources\n1. " note "\n2. " note)
        item (document/strip text)]
    (is (str/includes? item "Sources")
        "Sources section was removed without links")))

(deftest the-document-keeps-sources-when-not-last-section
  (let [rng (java.util.Random. 18067)
        head (token rng 6 1040 32)
        note (token rng 5 880 32)
        url (str "https://example.com/" (.nextInt rng 1000))
        text (str head "\n\n## Sources\n1. " url "\n\n## Далее\n" note)
        item (document/strip text)]
    (is (str/includes? item "Sources")
        "Sources section was removed before end")))

(deftest the-document-inserts-images-before-sources
  (let [rng (java.util.Random. 18069)
        head (token rng 6 1040 32)
        title (token rng 5 880 32)
        url (str "https://example.com/" (.nextInt rng 1000))
        image (str "https://example.com/" (.nextInt rng 1000) ".png")
        text (str head "\n\n## Sources\n1. " url)
        raw {:images [{:image_url image
                       :title title}]}
        task (task/task {:query head
                         :status "completed"
                         :result nil
                         :service "valyu.ai"
                         :created (task/format (task/now))})
        entry (task/data task)
        item (session/session {:topic head
                               :tasks [entry]
                               :created (session/format (session/now))})
        root (Paths/get "output" (make-array String 0))
        doc (document/document item (palette/palette) (Optional/empty) root)
        result (document/images doc text raw task)
        expect (str "## Images\n\n![" title "](" image ")\n\n## Sources")]
    (is (str/includes? result expect)
        "Images were not inserted before Sources")))

(deftest the-document-preserves-signed-image-urls
  (let [rng (java.util.Random. 18071)
        key (token rng 4 880 32)
        val (token rng 4 1328 32)
        link (str "https://example.com/"
                  (.nextInt rng 1000)
                  "?"
                  key
                  "="
                  val
                  "&sig="
                  (.nextInt rng 1000))
        item (document/trim link)]
    (is (= link item)
        "Image URL was changed despite missing utm parameters")))

(deftest the-document-uses-cached-image-file
  (let [rng (java.util.Random. 18073)
        head (token rng 6 1040 32)
        title (token rng 5 880 32)
        code (token rng 4 1328 32)
        root (Files/createTempDirectory "images"
                                        (make-array FileAttribute 0))
        task (task/task {:query head
                         :status "completed"
                         :result nil
                         :service "valyu.ai"
                         :created (task/format (task/now))})
        entry (task/data task)
        item (session/session {:topic head
                               :tasks [entry]
                               :created (session/format (session/now))})
        maker (organizer/organizer root)
        name (organizer/name
              maker
              (session/created item)
              (session/topic item)
              (session/id item))
        folder (.resolve (.resolve (.resolve root name) "valyu") "images")
        _ (Files/createDirectories folder (make-array FileAttribute 0))
        path (.resolve folder (str code ".png"))
        _ (Files/write path
                       (.getBytes "image" "UTF-8")
                       (make-array java.nio.file.OpenOption 0))
        raw {:images [{:image_url "https://example.com/image.png"
                       :image_id code
                       :title title}]}
        doc (document/document item (palette/palette) (Optional/empty) root)
        text (str head "\n\n## Sources\n1. https://example.com")
        expect (.toString (.toUri path))
        result (document/images doc text raw task)]
    (is (str/includes? result expect)
        "Cached image file was not used")))

(deftest the-document-strips-utm-fragments-from-text
  (let [rng (java.util.Random. 18075)
        head (token rng 6 1040 32)
        label (token rng 5 880 32)
        value (token rng 4 1328 32)
        number (inc (.nextInt rng 90))
        text (str head " [" number "]?utm_" label "=" value ") " head)
        item (document/clean text)]
    (is (not (str/includes? item "utm_"))
        "utm fragments were not stripped from text")))
