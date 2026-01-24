(ns research.pdf.document-test
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is]]
            [research.domain.result :as result]
            [research.domain.session :as session]
            [research.domain.task :as task]
            [research.pdf.document :as document]
            [research.pdf.palette :as palette]
            [research.pdf.style :as style]
            [research.storage.organizer :as organizer]
            [research.test.ids :as gen])
  (:import (java.nio.file Files Paths)
           (java.nio.file.attribute FileAttribute)
           (java.util Optional)
           (org.jsoup Jsoup)))
(deftest the-document-render-contains-doctype
  (let [rng (gen/ids 18001)
        topic (gen/cyrillic rng 6)
        item (session/session {:topic topic
                               :tasks []
                               :created (session/format (session/now))})
        root (Paths/get "output" (make-array String 0))
        doc (document/document item (palette/palette) (Optional/empty) root)
        html (document/render doc)]
    (is (str/includes? html "<!DOCTYPE html>")
        "Rendered document did not contain DOCTYPE")))

(deftest the-document-render-contains-topic
  (let [rng (gen/ids 18003)
        topic (gen/hiragana rng 6)
        head (document/heading topic)
        item (session/session {:topic topic
                               :tasks []
                               :created (session/format (session/now))})
        root (Paths/get "output" (make-array String 0))
        doc (document/document item (palette/palette) (Optional/empty) root)
        html (document/render doc)]
    (is (str/includes? html head)
        "Rendered document did not contain heading")))

(deftest the-document-heading-uppercases-initial-letter
  (let [rng (gen/ids 18004)
        text (gen/cyrillic rng 6)
        head (subs text 0 1)
        tail (subs text 1)
        goal (str (str/upper-case head) tail)
        value (document/heading text)]
    (is (= goal value)
        "Heading did not uppercase initial letter")))

(deftest the-document-renders-exploration-brief-title
  (let [rng (gen/ids 18005)
        day (inc (.nextInt rng 8))
        hour (inc (.nextInt rng 8))
        time (str "2026-01-0" day "T0" hour ":00:00")
        query (gen/cyrillic rng 6)
        status (gen/cyrillic rng 6)
        language (gen/cyrillic rng 6)
        service (gen/cyrillic rng 6)
        entry {:id (gen/uuid rng)
               :query query
               :status status
               :language language
               :service service
               :created time}
        item (session/session {:topic query
                               :tasks [entry]
                               :created time})
        root (Paths/get "output" (make-array String 0))
        doc (document/document item (palette/palette) (Optional/empty) root)
        html (document/render doc)]
    (is (str/includes? html "<h1>Exploration Brief</h1>")
        "Exploration Brief title was missing")))

(deftest the-document-includes-palette-colors
  (let [rng (gen/ids 18007)
        topic (gen/hiragana rng 6)
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
(deftest the-document-style-includes-italic-bold-weights
  (let [rng (gen/ids 18008)
        mark (gen/cyrillic rng 6)
        css (str (style/css (style/style (palette/palette))) mark)]
    (is (str/includes? css "1,600;1,700")
        "Style did not include italic bold weights")))

(deftest the-document-renders-author-name
  (let [rng (gen/ids 18009)
        name (gen/cyrillic rng 6)
        service (gen/hiragana rng 4)
        value (gen/greek rng 5)
        result (result/->ResearchReport value [])
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
  (let [rng (gen/ids 18011)
        name (gen/cyrillic rng 6)
        service "parallel.ai"
        value (gen/greek rng 5)
        result (result/->ResearchReport value [])
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
  (let [rng (gen/ids 18013)
        name (gen/cyrillic rng 6)
        value (gen/greek rng 5)
        result (result/->ResearchReport value [])
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
  (let [rng (gen/ids 18015)
        name (gen/cyrillic rng 6)
        value (gen/greek rng 5)
        result (result/->ResearchReport value [])
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
  (let [rng (gen/ids 18017)
        service (gen/hiragana rng 4)
        value (gen/greek rng 5)
        result (result/->ResearchReport value [])
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
  (let [rng (gen/ids 18019)
        head (gen/cyrillic rng 6)
        item (gen/hiragana rng 6)
        tail (gen/latin rng 6)
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
  (let [rng (gen/ids 18021)
        head (gen/cyrillic rng 6)
        tail (gen/hiragana rng 6)
        mark (String. (Character/toChars 9989))
        text (str head " " mark " " tail)
        html (document/emojify text)
        done (and (str/includes? html "class=\"emoji\"")
                  (str/includes? html mark))]
    (is done "Emoji span was not rendered")))

(deftest the-document-listify-converts-numbered-prompts
  (let [rng (gen/ids 18029)
        head (gen/cyrillic rng 6)
        left (gen/hiragana rng 6)
        right (gen/latin rng 6)
        one (inc (.nextInt rng 8))
        two (+ one (inc (.nextInt rng 8)))
        text (str head " " one ") " left " " two ". " right)
        item (document/listify text)
        mark (and (str/includes? item (str "\n" one ". "))
                  (str/includes? item (str "\n" two ". ")))]
    (is mark "Numbered prompts were not converted")))

(deftest the-document-listify-converts-inline-bullets
  (let [rng (gen/ids 18030)
        head (gen/cyrillic rng 6)
        left (gen/hiragana rng 6)
        mid (gen/latin rng 6)
        tail (gen/greek rng 6)
        text (str head " - " left " + " mid " * " tail)
        item (document/listify text)
        mark (and (str/includes? item "\n- ")
                  (str/includes? item "\n+ ")
                  (str/includes? item "\n* "))]
    (is mark "Inline bullets were not converted")))

(deftest the-document-listify-keeps-numbered-hyphens
  (let [rng (gen/ids 18031)
        head (gen/cyrillic rng 6)
        left (gen/hiragana rng 6)
        right (gen/latin rng 6)
        mid (gen/greek rng 5)
        one (inc (.nextInt rng 8))
        two (+ one (inc (.nextInt rng 8)))
        part (str left " - " right)
        tail (str mid " - " head)
        text (str head " " one ") " part " " two ") " tail)
        item (document/listify text)
        mark (and (str/includes? item part)
                  (str/includes? item tail)
                  (not (str/includes? item "\n- ")))]
    (is mark "Numbered prompts were split into bullet lines")))

(deftest the-document-wraps-list-items-in-paragraphs
  (let [rng (gen/ids 18027)
        head (gen/cyrillic rng 6)
        tail (gen/hiragana rng 6)
        html (str "<ul><li><strong>"
                  head
                  "</strong> "
                  tail
                  "</li></ul>")
        item (document/paragraphs html)]
    (is (some? (re-find #"(?s)<li[^>]*>.*<p>" item))
        "List items were not wrapped")))

(deftest ^{:doc "Ensures code spans keep backslashes."}
  the-document-unescapes-backslashes-in-code
  (let [rng (gen/ids 18032)
        day (inc (.nextInt rng 8))
        hour (inc (.nextInt rng 8))
        time (str "2026-01-0" day "T0" hour ":00:00")
        head (gen/cyrillic rng 6)
        left (gen/latin rng 4)
        mid (gen/hiragana rng 4)
        right (gen/greek rng 4)
        path (str "C:\\" left "\\" mid "\\" right ".exe")
        text (str head " `" path "`")
        result (result/->ResearchReport text [])
        task (task/task {:query head
                         :status "completed"
                         :result (result/data result)
                         :language head
                         :service "valyu.ai"
                         :created time})
        entry (task/data task)
        item (session/session {:topic head
                               :tasks [entry]
                               :created time})
        root (Paths/get "output" (make-array String 0))
        doc (document/document
             item
             (palette/palette)
             (Optional/empty)
             root)
        pair (document/taskhtml doc task)
        html (first pair)
        tree (Jsoup/parseBodyFragment html)
        code (.text (.selectFirst tree "code"))]
    (is (= path code)
        "Backslashes were still escaped in code")))

(deftest ^{:doc "Ensures star ratings are normalized."}
  the-document-replaces-star-ratings
  (let [rng (gen/ids 18033)
        day (inc (.nextInt rng 8))
        hour (inc (.nextInt rng 8))
        time (str "2026-01-0" day "T0" hour ":00:00")
        head (gen/cyrillic rng 6)
        left (gen/hiragana rng 5)
        right (gen/greek rng 5)
        ink (inc (.nextInt rng 4))
        void (inc (.nextInt rng 4))
        core (apply str (repeat ink (char 0x2605)))
        shell (apply str (repeat void (char 0x2606)))
        stars (str core shell)
        rate (str ink "/" (+ ink void))
        rows (str "| " head " | " left " |\n"
                  "|---|---|\n"
                  "| " right " (" stars ") | " head " |")
        result (result/->ResearchReport rows [])
        task (task/task {:query head
                         :status "completed"
                         :result (result/data result)
                         :language head
                         :service "valyu.ai"
                         :created time})
        entry (task/data task)
        item (session/session {:topic head
                               :tasks [entry]
                               :created time})
        root (Paths/get "output" (make-array String 0))
        doc (document/document item (palette/palette) (Optional/empty) root)
        pair (document/taskhtml doc task)
        html (first pair)
        tree (Jsoup/parseBodyFragment html)
        text (.text (.selectFirst tree "table"))
        mark (and (str/includes? text rate) (not (re-find #"[★☆]" text)))]
    (is mark "Star ratings were not converted")))

(deftest ^{:doc "Ensures source excerpts render decoded entities."}
  the-document-normalizes-source-entities
  (let [rng (gen/ids 18034)
        day (inc (.nextInt rng 8))
        hour (inc (.nextInt rng 8))
        time (str "2026-01-0" day "T0" hour ":00:00")
        head (gen/cyrillic rng 6)
        left (gen/hiragana rng 4)
        right (gen/greek rng 4)
        title (str head " " left)
        link (str "https://example.com/" (gen/latin rng 6))
        excerpt (str "&gt;&gt;&gt; df = pd.DataFrame({&#x27;A&#x27; : ['"
                     right
                     "']})")
        source (result/source {:title title
                               :url link
                               :excerpt excerpt
                               :confidence "Unknown"})
        result (result/->ResearchReport head [source])
        task (task/task {:query head
                         :status "completed"
                         :result (result/data result)
                         :language head
                         :service "valyu.ai"
                         :created time})
        entry (task/data task)
        item (session/session {:topic head
                               :tasks [entry]
                               :created time})
        root (Paths/get "output" (make-array String 0))
        doc (document/document item (palette/palette) (Optional/empty) root)
        html (document/render doc)
        tree (Jsoup/parseBodyFragment html)
        text (.text (.selectFirst tree ".source-excerpt"))
        mark (and (str/includes? text ">>> df = pd.DataFrame({'A' : ['")
                  (str/includes? text right))]
    (is mark "Source excerpts were not normalized")))

(deftest the-document-render-contains-task-query
  (let [rng (gen/ids 18021)
        day (inc (.nextInt rng 8))
        hour (inc (.nextInt rng 8))
        time (str "2026-01-0" day "T0" hour ":00:00")
        query (gen/hiragana rng 6)
        status (gen/cyrillic rng 6)
        language (gen/cyrillic rng 6)
        service (gen/cyrillic rng 6)
        entry {:id (gen/uuid rng)
               :query query
               :status status
               :language language
               :service service
               :created time}
        topic (gen/cyrillic rng 5)
        item (session/session {:topic topic
                               :tasks [entry]
                               :created time})
        root (Paths/get "output" (make-array String 0))
        doc (document/document item (palette/palette) (Optional/empty) root)
        html (document/render doc)]
    (is (str/includes? html query)
        "Rendered document did not contain task query")))

(deftest the-document-render-contains-synthesis
  (let [rng (gen/ids 18023)
        summary (gen/hiragana rng 6)
        result (result/->ResearchReport summary [])
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

(deftest the-document-renders-outputs-for-multiple-providers
  (let [rng (gen/ids 18024)
        topic (gen/cyrillic rng 6)
        first (gen/cyrillic rng 8)
        second (gen/cyrillic rng 8)
        a (task/task {:query topic
                      :status "completed"
                      :result nil
                      :service "parallel.ai"
                      :created (task/format (task/now))})
        b (task/task {:query topic
                      :status "completed"
                      :result nil
                      :service "valyu.ai"
                      :created (task/format (task/now))})
        item (session/session {:topic topic
                               :tasks [(task/data a) (task/data b)]
                               :created (session/format (session/now))})
        root (Files/createTempDirectory "doc"
                                        (make-array FileAttribute 0))
        maker (organizer/organizer root)
        name (organizer/name
              maker
              (session/created item)
              (session/topic item)
              (session/id item))
        _ (organizer/response
           maker
           name
           "parallel"
           {:run {:run_id "run_a"
                  :status "completed"}
            :output {:content first
                     :basis []}})
        _ (organizer/response
           maker
           name
           "valyu"
           {:output {:markdown second}
            :sources []
            :status "completed"
            :deepresearch_id "run_b"})
        doc (document/document item (palette/palette) (Optional/empty) root)
        html (document/render doc)
        seen (and (str/includes? html first) (str/includes? html second))]
    (is seen "Rendered document did not include provider outputs")))

(deftest the-document-renders-confidence-badge
  (let [rng (gen/ids 18025)
        text (gen/cyrillic rng 5)
        url (str "https://example.com/"
                 (.nextInt rng 1000)
                 "?utm_source=valyu.ai&utm_medium=referral")
        source (result/->CitationSource text url text "High")
        summary (str text "\n\n## References\n1. " url)
        result (result/->ResearchReport summary [source])
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

(deftest the-document-renders-sources-section
  (let [rng (gen/ids 18028)
        head (gen/cyrillic rng 6)
        note (gen/cyrillic rng 6)
        link (str "https://example.com/" (.nextInt rng 1000))
        source (result/->CitationSource head link note "High")
        value (result/->ResearchReport head [source])
        task (task/task {:query head
                         :status "completed"
                         :result (result/data value)
                         :service "valyu.ai"
                         :created (task/format (task/now))})
        entry (task/data task)
        item (session/session {:topic head
                               :tasks [entry]
                               :created (session/format (session/now))})
        root (Paths/get "output" (make-array String 0))
        doc (document/document item (palette/palette) (Optional/empty) root)
        html (document/render doc)]
    (is (str/includes? html "<h2>Sources</h2>")
        "Sources section was missing")))

(deftest the-document-uses-domain-for-placeholder-title
  (let [rng (gen/ids 18029)
        name (gen/ascii rng 6)
        host (str name ".com")
        link (str "https://" host "/" (gen/ascii rng 5))
        head (gen/cyrillic rng 6)
        mark (gen/cyrillic rng 6)
        source (result/->CitationSource "Fetched web page" link "" "High")
        value (result/->ResearchReport head [source])
        task (task/task {:query mark
                         :status "completed"
                         :result (result/data value)
                         :service "parallel.ai"
                         :created (task/format (task/now))})
        entry (task/data task)
        item (session/session {:topic mark
                               :tasks [entry]
                               :created (session/format (session/now))})
        root (Paths/get "output" (make-array String 0))
        doc (document/document item (palette/palette) (Optional/empty) root)
        html (document/render doc)]
    (is (str/includes? html host)
        "Source domain was missing")))

(deftest the-document-tables-adds-column-class
  (let [rng (gen/ids 18026)
        head (gen/cyrillic rng 4)
        body (gen/hiragana rng 4)
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
  (let [rng (gen/ids 18027)
        slug (gen/cyrillic rng 5)
        link (str "https://example.com/"
                  (.nextInt rng 1000)
                  "?utm_source=valyu.ai&utm_medium=referral&x="
                  (.nextInt rng 9))
        summary (str "Sources\n1. " link "\n2. " slug)
        result (result/->ResearchReport summary [])
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

(deftest the-document-removes-parenthetical-urls
  (let [rng (gen/ids 18028)
        head (gen/cyrillic rng 6)
        name (gen/ascii rng 5)
        part (gen/ascii rng 5)
        tail (gen/ascii rng 4)
        link (str name ".net/" part "_radio_" tail ".htm")
        text (str head " (" link "): " head)
        item (document/clean text)]
    (is (not (str/includes? item link))
        "Parenthetical url was not removed")))

(deftest the-document-preserves-markdown-link-urls
  (let [rng (gen/ids 18029)
        head (gen/cyrillic rng 6)
        host (gen/ascii rng 6)
        path (gen/ascii rng 6)
        link (str "https://" host ".com/" path "_radio?utm_source=valyu.ai")
        text (str "[" head "](" link ")")
        item (document/clean text)
        expect (document/trim link)]
    (is (str/includes? item expect)
        "Markdown link url was truncated")))

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
  (let [rng (gen/ids 18029)
        path (.resolve (Files/createTempDirectory
                        "doc"
                        (make-array FileAttribute 0))
                       (str "test-" (gen/uuid rng) ".html"))
        item (session/session {:topic "T"
                               :tasks []
                               :created (session/format (session/now))})
        root (Paths/get "output" (make-array String 0))
        doc (document/document item (palette/palette) (Optional/empty) root)]
    (document/page doc path)
    (is (Files/exists path (make-array java.nio.file.LinkOption 0))
        "HTML file was not created")))

(deftest the-document-normalize-adds-blank-line-before-list
  (let [rng (gen/ids 18031)
        text (str "**"
                  (gen/hiragana rng 6)
                  "**\n* "
                  (gen/cyrillic rng 4))
        item (document/normalize text)]
    (is (str/includes? item "**\n\n* ")
        "Normalize did not add blank line before list")))

(deftest the-document-normalize-preserves-existing-blank-lines
  (let [rng (gen/ids 18033)
        text (str "**"
                  (gen/hiragana rng 6)
                  "**\n\n* "
                  (gen/cyrillic rng 4))
        item (document/normalize text)]
    (is (= text item) "Normalize modified already correct text")))

(deftest the-document-normalize-handles-multiple-lists
  (let [rng (gen/ids 18035)
        text (str "**"
                  (gen/hiragana rng 6)
                  "**\n* 一\n**第二**\n* 二")
        item (document/normalize text)]
    (is (= 2 (count (re-seq #"\n\n\* " item)))
        "Normalize did not fix all lists")))

(deftest the-document-normalize-ignores-lists-after-blank-line
  (let [rng (gen/ids 18037)
        text (str "段落-"
                  (gen/hiragana rng 6)
                  "\n\n* すでに正しい")
        item (document/normalize text)]
    (is (= 0 (count (re-seq #"\n\n\n" item)))
        "Normalize added extra blank lines")))

(deftest the-document-rule-replaces-separators
  (let [rng (gen/ids 18038)
        head (gen/cyrillic rng 6)
        tail (gen/hiragana rng 6)
        text (str head "\n---\n" tail)
        item (document/rule text)]
    (is (str/includes? item "<hr />") "Rule did not convert separator")))

(deftest the-document-citations-convert-references
  (let [rng (gen/ids 18039)
        mark (gen/uuid rng)
        head (gen/hiragana rng 5)
        host (gen/ascii rng 6)
        link (str "https://" host ".com/" mark)
        text (str head "-" mark " [1]\n\n## References\n\n1. "
                  head
                  " "
                  link)
        data (document/citations text [])
        item (first data)
        pool (nth data 2)
        href (str "<a href=\""
                  link
                  "\" class=\"cite\" target=\"_blank\">[1]</a>")
        seen (and (str/includes? item "@@CITE")
                  (some #(str/includes? % href) (vals pool)))]
    (is seen "Citations did not create link from reference")))

(deftest the-document-citations-extract-urls
  (let [rng (gen/ids 18041)
        mark (gen/uuid rng)
        text (str "参照 [1]\n\n## References\n\n1. ソース https://test.jp/"
                  mark)
        urls (second (document/citations text []))]
    (is (= 1 (count urls)) "Citations did not extract URL")))

(deftest the-document-render-avoids-italic-leak-after-snake-case
  (let [rng (gen/ids 18042)
        head (gen/cyrillic rng 5)
        left (gen/hiragana rng 4)
        right (gen/greek rng 4)
        word (str left "_" right)
        host (gen/ascii rng 6)
        path (gen/ascii rng 6)
        link (str "https://" host ".com/" path)
        text (str head " " word " [1]\n\n## References\n\n1. "
                  left
                  " "
                  link)
        value (result/->ResearchReport text [])
        task (task/task {:query head
                         :status "completed"
                         :result (result/data value)
                         :created (task/format (task/now))})
        entry (task/data task)
        item (session/session {:topic head
                               :tasks [entry]
                               :created (session/format (session/now))})
        root (Paths/get "output" (make-array String 0))
        doc (document/document item (palette/palette) (Optional/empty) root)
        html (document/render doc)
        seen (and (str/includes? html word)
                  (not (str/includes? html "target=\"</i>blank\"")))]
    (is seen "HTML contained broken italic target")))

(deftest the-document-references-extract-mapping
  (let [rng (gen/ids 18043)
        mark (gen/uuid rng)
        text (str "## References\n\n1. 一 https://a.jp/"
                  mark
                  "\n2. 二 https://b.jp/"
                  mark)
        refs (document/references text)]
    (is (= 2 (count refs)) "References did not extract all entries")))

(deftest the-document-path-returns-session-based-path
  (let [rng (gen/ids 18045)
        ident (gen/uuid rng)
        short (first (str/split ident #"-"))
        item (session/session {:id ident
                               :topic "T"
                               :tasks []
                               :created (session/format (session/now))})
        root (Paths/get "output" (make-array String 0))
        doc (document/document item (palette/palette) (Optional/empty) root)
        path (document/briefpath doc)
        text (str path)
        seen (and (str/includes? text short)
                  (str/includes? text "output")
                  (or (str/includes? text "input-")
                      (str/includes? text "brief-")))]
    (is seen "Path did not target output brief or input file")))

(deftest ^{:doc "Uses pending provider for brief path"}
  the-document-briefpath-uses-pending-provider
  (let [rng (gen/ids 18046)
        mark (gen/cyrillic rng 6)
        entry {:run_id (gen/uuid rng)
               :query mark
               :processor "pro"
               :language mark
               :provider "valyu"}
        item (session/session {:id (gen/uuid rng)
                               :topic mark
                               :tasks []
                               :created (session/format (session/now))
                               :pending entry})
        root (Paths/get "output" (make-array String 0))
        doc (document/document item (palette/palette) (Optional/empty) root)
        path (document/briefpath doc)
        seen (str/includes? (str path) "input-valyu.md")]
    (is seen "Brief path did not use pending provider")))

(deftest the-document-brief-reads-from-file
  (let [rng (gen/ids 18047)
        ident (gen/uuid rng)
        mark (str "マーカー-" (gen/uuid rng))
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
  (let [rng (gen/ids 18049)
        day (inc (.nextInt rng 8))
        hour (inc (.nextInt rng 8))
        time (str "2026-01-0" day "T0" hour ":00:00")
        mark (str "クエリ-" (gen/uuid rng))
        status (gen/cyrillic rng 6)
        language (gen/cyrillic rng 6)
        service (gen/cyrillic rng 6)
        entry {:id (gen/uuid rng)
               :query mark
               :status status
               :language language
               :service service
               :created time}
        topic (gen/cyrillic rng 5)
        item (session/session {:topic topic
                               :tasks [entry]
                               :created time})
        root (Paths/get "output" (make-array String 0))
        doc (document/document item (palette/palette) (Optional/empty) root)
        html (document/brief doc)]
    (is (str/includes? html mark) "Brief did not fall back to query")))

(deftest the-document-nested-converts-single-space-indent
  (let [rng (gen/ids 18051)
        mark (gen/uuid rng)
        text (str "* **親-" mark ":**\n * **子要素:** 内容")
        item (document/nested text)]
    (is (str/includes? item "    * ")
        "Nested did not convert single space to four spaces")))

(deftest the-document-nested-preserves-four-space-indent
  (let [rng (gen/ids 18053)
        mark (gen/uuid rng)
        text (str "* **親-" mark ":**\n    * **子:** 内容")
        item (document/nested text)]
    (is (= text item) "Nested modified already correct indentation")))

(deftest the-document-nested-handles-multiple-levels
  (let [rng (gen/ids 18055)
        mark (gen/uuid rng)
        text (str "* 一-" mark "\n  * 二\n   * 三")
        item (document/nested text)]
    (is (= 2 (count (re-seq #"\n    \* " item)))
        "Nested did not normalize all indented items")))
(deftest ^{:doc "underscorify rewrites nested bold at end of italic bullets"}
  the-document-underscorify-rewrites-nested-bold-in-bullets
  (let [rng (gen/ids 18061)
        mark (gen/uuid rng)
        text (str "- *"
                  mark
                  " **to be fed*** — "
                  "Ребёнка нужно покормить")
        item (document/underscorify text)
        goal (str "- _"
                  mark
                  " **to be fed**_ — "
                  "Ребёнка нужно покормить")]
    (is (= goal item)
        "underscorify failed to rewrite nested bold in bullet")))
(deftest ^{:doc (str "underscorify rewrites nested bold at end of italic "
                     "inline text")}
  the-document-underscorify-rewrites-nested-bold-inline
  (let [rng (gen/ids 18063)
        mark (gen/uuid rng)
        text (str "Present Perfect — *The report "
                  mark
                  " **has been written***")
        item (document/underscorify text)
        goal (str "Present Perfect — _The report "
                  mark
                  " **has been written**_")]
    (is (= goal item)
        "underscorify failed to rewrite nested bold inline")))
(deftest ^{:doc "underscorify avoids matching inside bold headings"}
  the-document-underscorify-does-not-touch-bold-heading
  (let [rng (gen/ids 18065)
        mark (gen/uuid rng)
        text (str "**Заголовок-"
                  mark
                  "** — *The report **has been being written***")
        item (document/underscorify text)
        goal (str "**Заголовок-"
                  mark
                  "** — _The report **has been being written**_")]
    (is (= goal item)
        "underscorify modified bold heading unexpectedly")))

(deftest the-document-normalize-adds-blank-line-before-numbered-list
  (let [rng (gen/ids 18057)
        mark (gen/uuid rng)
        text (str "調査-" mark ":\n1. 最初の項目")
        item (document/normalize text)]
    (is (str/includes? item ":\n\n1. ")
        "Normalize did not add blank line before numbered list")))

(deftest the-document-normalize-handles-mixed-lists
  (let [rng (gen/ids 18059)
        mark (gen/uuid rng)
        text (str "テキスト-"
                  mark
                  "\n* 箇条書き\n別のテキスト\n1. 番号付き")
        item (document/normalize text)]
    (is (= 2 (count (re-seq #"\n\n" item)))
        "Normalize did not add blank lines before both list types")))

(deftest the-document-brief-normalizes-numbered-lists
  (let [rng (gen/ids 18061)
        day (inc (.nextInt rng 8))
        hour (inc (.nextInt rng 8))
        time (str "2026-01-0" day "T0" hour ":00:00")
        mark (gen/uuid rng)
        query (str "調査-" mark ":\n1. 最初\n2. 二番目")
        status (gen/cyrillic rng 6)
        language (gen/cyrillic rng 6)
        service (gen/cyrillic rng 6)
        entry {:id (gen/uuid rng)
               :query query
               :status status
               :language language
               :service service
               :created time}
        topic (gen/cyrillic rng 5)
        item (session/session {:topic topic
                               :tasks [entry]
                               :created time})
        root (Paths/get "output" (make-array String 0))
        doc (document/document item (palette/palette) (Optional/empty) root)
        html (document/brief doc)]
    (is (str/includes? html "<ol>")
        "Brief did not render numbered list as <ol>")))

(deftest the-document-brief-normalizes-bullet-lists
  (let [rng (gen/ids 18062)
        day (inc (.nextInt rng 8))
        hour (inc (.nextInt rng 8))
        time (str "2026-01-0" day "T0" hour ":00:00")
        head (gen/cyrillic rng 6)
        left (gen/hiragana rng 6)
        mid (gen/latin rng 6)
        tail (gen/greek rng 6)
        query (str head ":\n- " left "\n+ " mid "\n* " tail)
        status (gen/cyrillic rng 6)
        language (gen/cyrillic rng 6)
        service (gen/cyrillic rng 6)
        entry {:id (gen/uuid rng)
               :query query
               :status status
               :language language
               :service service
               :created time}
        topic (gen/cyrillic rng 5)
        item (session/session {:topic topic
                               :tasks [entry]
                               :created time})
        root (Paths/get "output" (make-array String 0))
        doc (document/document item (palette/palette) (Optional/empty) root)
        html (document/brief doc)
        mark (count (re-seq #"<li" html))]
    (is (= 3 mark) "Brief did not render bullet list items")))

(deftest the-document-strips-sources-section
  (let [rng (gen/ids 18063)
        head (gen/cyrillic rng 6)
        link (str "https://example.com/" (.nextInt rng 1000))
        text (str head "\n\n## Sources\n1. " link "\n2. " link)
        item (document/strip text)]
    (is (not (str/includes? item "Sources"))
        "Sources section was not stripped")))

(deftest the-document-keeps-sources-without-links
  (let [rng (gen/ids 18065)
        head (gen/cyrillic rng 6)
        note (gen/greek rng 5)
        text (str head "\n\n## Sources\n1. " note "\n2. " note)
        item (document/strip text)]
    (is (str/includes? item "Sources")
        "Sources section was removed without links")))

(deftest the-document-keeps-sources-when-not-last-section
  (let [rng (gen/ids 18067)
        head (gen/cyrillic rng 6)
        note (gen/greek rng 5)
        url (str "https://example.com/" (.nextInt rng 1000))
        text (str head "\n\n## Sources\n1. " url "\n\n## Далее\n" note)
        item (document/strip text)]
    (is (str/includes? item "Sources")
        "Sources section was removed before end")))

(deftest the-document-inserts-images-before-sources
  (let [rng (gen/ids 18069)
        head (gen/cyrillic rng 6)
        title (gen/greek rng 5)
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
  (let [rng (gen/ids 18071)
        key (gen/greek rng 4)
        val (gen/armenian rng 4)
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
  (let [rng (gen/ids 18073)
        head (gen/cyrillic rng 6)
        title (gen/greek rng 5)
        code (gen/ascii rng 8)
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
        tag (organizer/slug "valyu")
        folder (.resolve
                (organizer/folder maker name "valyu")
                (str "images-" tag))
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
  (let [rng (gen/ids 18075)
        head (gen/cyrillic rng 6)
        label (gen/greek rng 5)
        value (gen/armenian rng 4)
        number (inc (.nextInt rng 90))
        text (str head " [" number "]?utm_" label "=" value ") " head)
        item (document/clean text)]
    (is (not (str/includes? item "utm_"))
        "utm fragments were not stripped from text")))
