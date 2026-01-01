(ns research.domain.result-test
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is]]
            [research.domain.result :as result]))

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

(deftest the-source-returns-provided-title
  (let [rng (java.util.Random. 14001)
        title (token rng 6 1040 32)
        item (result/->Source title "https://example.com" "text" "")]
    (is (= title (result/title item))
        "Source title did not match provided value")))

(deftest the-source-returns-provided-url
  (let [rng (java.util.Random. 14003)
        url (str "https://example.com/" (uuid rng))
        item (result/->Source "Title" url "text" "")]
    (is (= url (result/url item)) "Source URL did not match provided value")))

(deftest the-source-serializes-all-fields
  (let [rng (java.util.Random. 14005)
        excerpt (token rng 6 1040 32)
        item (result/->Source "T" "https://x.com" excerpt "")
        data (result/data item)]
    (is (= excerpt (:excerpt data))
        "Serialized excerpt did not match original")))

(deftest the-source-deserializes-from-map
  (let [rng (java.util.Random. 14007)
        title (token rng 6 1040 32)
        data {:title title
              :url "https://x.com"
              :excerpt "e"}
        item (result/source data)]
    (is (= title (result/title item))
        "Deserialized source title did not match")))

(deftest the-result-returns-summary
  (let [rng (java.util.Random. 14009)
        summary (token rng 6 1040 32)
        item (result/->Result summary [])]
    (is (= summary (result/summary item))
        "Result summary did not match provided value")))

(deftest the-result-strips-sources-section
  (let [rng (java.util.Random. 14011)
        slug (token rng 6 1040 32)
        link (str "https://example.com/" (.nextInt rng 1000))
        summary (str "Введение " slug "\n\n## Sources\n1. " link)
        item (result/->Result summary [])]
    (is (not (str/includes? (result/summary item) "## Sources"))
        "Sources section was not stripped")))

(deftest the-result-returns-sources
  (let [rng (java.util.Random. 14013)
        text (token rng 6 1040 32)
        item (result/->Result "s" [(result/->Source "T" "https://x.com"
                                                    text
                                                    "")])]
    (is (= 1 (count (result/sources item)))
        "Result sources count was not one")))

(deftest the-result-serializes-correctly
  (let [rng (java.util.Random. 14015)
        summary (token rng 6 1040 32)
        item (result/->Result summary [])
        data (result/data item)]
    (is (= summary (:summary data))
        "Serialized summary did not match original")))

(deftest the-result-deserializes-correctly
  (let [rng (java.util.Random. 14017)
        summary (token rng 6 1040 32)
        item (result/result {:summary summary
                             :sources []})]
    (is (= summary (result/summary item))
        "Deserialized summary did not match")))

(deftest the-source-returns-provided-confidence
  (let [rng (java.util.Random. 14019)
        confidence (token rng 5 1040 32)
        item (result/->Source "T" "https://example.com" "e" confidence)]
    (is (= confidence (result/confidence item))
        "Source confidence did not match provided value")))

(deftest the-source-returns-empty-when-confidence-missing
  (let [rng (java.util.Random. 14021)
        text (token rng 6 1040 32)
        item (result/->Source "T" "https://example.com" text "")]
    (is (= "" (result/confidence item))
        "Source confidence was not empty when not provided")))

(deftest the-source-serializes-confidence-when-provided
  (let [rng (java.util.Random. 14023)
        confidence (token rng 5 1040 32)
        item (result/->Source "T" "https://x.com" "e" confidence)
        data (result/data item)]
    (is (= confidence (:confidence data))
        "Serialized confidence did not match provided value")))

(deftest the-source-omits-confidence-when-missing
  (let [item (result/->Source "T" "https://x.com" "e" "")
        data (result/data item)]
    (is (not (contains? data :confidence))
        "Serialized source contained confidence when not provided")))

(deftest the-source-deserializes-confidence
  (let [rng (java.util.Random. 14027)
        confidence (token rng 5 1040 32)
        data {:title "T"
              :url "https://x.com"
              :excerpt "e"
              :confidence confidence}
        item (result/source data)]
    (is (= confidence (result/confidence item))
        "Deserialized confidence did not match")))

(deftest the-source-deserializes-without-confidence
  (let [data {:title "T"
              :url "https://x.com"
              :excerpt "e"}
        item (result/source data)]
    (is (= "" (result/confidence item))
        "Deserialized confidence was not empty when absent")))
