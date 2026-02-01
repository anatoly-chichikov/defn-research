(ns research.domain.result-test
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is]]
            [research.domain.result :as result]
            [research.test.ids :as gen]))

(deftest the-source-returns-provided-title
  (let [rng (gen/ids 14001)
        title (gen/cyrillic rng 6)
        item (result/->CitationSource title "https://example.com" "text")]
    (is (= title (result/title item))
        "Source title did not match provided value")))

(deftest the-source-returns-provided-url
  (let [rng (gen/ids 14003)
        url (str "https://example.com/" (gen/uuid rng))
        item (result/->CitationSource "Title" url "text")]
    (is (= url (result/url item)) "Source URL did not match provided value")))

(deftest the-source-serializes-all-fields
  (let [rng (gen/ids 14005)
        excerpt (gen/cyrillic rng 6)
        item (result/->CitationSource "T" "https://x.com" excerpt)
        data (result/data item)]
    (is (= excerpt (:excerpt data))
        "Serialized excerpt did not match original")))

(deftest the-source-deserializes-from-map
  (let [rng (gen/ids 14007)
        title (gen/cyrillic rng 6)
        data {:title title
              :url "https://x.com"
              :excerpt "e"}
        item (result/source data)]
    (is (= title (result/title item))
        "Deserialized source title did not match")))

(deftest the-result-returns-summary
  (let [rng (gen/ids 14009)
        summary (gen/cyrillic rng 6)
        item (result/->ResearchReport summary [])]
    (is (= summary (result/summary item))
        "Result summary did not match provided value")))

(deftest the-result-strips-sources-section
  (let [rng (gen/ids 14011)
        slug (gen/cyrillic rng 6)
        link (str "https://example.com/" (.nextInt rng 1000))
        summary (str "Introduction " slug "\n\n## Sources\n1. " link)
        item (result/->ResearchReport summary [])]
    (is (not (str/includes? (result/summary item) "## Sources"))
        "Sources section was not stripped")))

(deftest the-result-returns-sources
  (let [rng (gen/ids 14013)
        text (gen/cyrillic rng 6)
        item (result/->ResearchReport
              "s"
              [(result/->CitationSource
                "T"
                "https://x.com"
                text)])]
    (is (= 1 (count (result/sources item)))
        "Result sources count was not one")))

(deftest the-result-serializes-correctly
  (let [rng (gen/ids 14015)
        summary (gen/cyrillic rng 6)
        item (result/->ResearchReport summary [])
        data (result/data item)]
    (is (= summary (:summary data))
        "Serialized summary did not match original")))

(deftest the-result-deserializes-correctly
  (let [rng (gen/ids 14017)
        summary (gen/cyrillic rng 6)
        item (result/result {:summary summary
                             :sources []})]
    (is (= summary (result/summary item))
        "Deserialized summary did not match")))

(deftest the-source-omits-confidence-on-serialization
  (let [rng (gen/ids 14019)
        excerpt (gen/cyrillic rng 5)
        item (result/->CitationSource "T" "https://example.com" excerpt)
        data (result/data item)]
    (is (not (contains? data :confidence))
        "Serialized source contained confidence")))

(deftest the-source-ignores-confidence-from-map
  (let [rng (gen/ids 14021)
        confidence (gen/cyrillic rng 5)
        data {:title "T"
              :url "https://x.com"
              :excerpt "e"
              :confidence confidence}
        item (result/source data)
        data (result/data item)]
    (is (not (contains? data :confidence))
        "Deserialized source contained confidence")))
