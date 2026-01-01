(ns research.api.response-test
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is]]
            [research.api.response :as response]
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

(deftest the-response-returns-identifier
  (let [rng (java.util.Random. 15001)
        ident (str "trun_" (uuid rng))
        item (response/response {:id ident
                                 :status "completed"
                                 :output ""
                                 :basis []})]
    (is (= ident (response/id item))
        "Response identifier did not match provided value")))

(deftest the-response-detects-completed
  (let [rng (java.util.Random. 15003)
        ident (str "trun_" (uuid rng))
        item (response/response {:id ident
                                 :status "completed"
                                 :output ""
                                 :basis []})]
    (is (response/completed item)
        "Response was not detected as completed")))

(deftest the-response-detects-failed
  (let [rng (java.util.Random. 15005)
        ident (str "trun_" (uuid rng))
        item (response/response {:id ident
                                 :status "failed"
                                 :output ""
                                 :basis []})]
    (is (response/failed item) "Response was not detected as failed")))

(deftest the-response-returns-markdown
  (let [rng (java.util.Random. 15007)
        output (str "# "
                    (token rng 6 1040 32)
                    "\n\n"
                    (token rng 5 12354 32))
        item (response/response {:id (uuid rng)
                                 :status "completed"
                                 :output output
                                 :basis []})]
    (is (= output (response/text item))
        "Response markdown did not match output")))

(deftest the-response-extracts-sources
  (let [rng (java.util.Random. 15009)
        url (str "https://example.com/" (uuid rng))
        text (token rng 6 1040 32)
        basis [{:citations [{:url url
                             :title "Test"
                             :excerpts [text]}]}]
        item (response/response {:id (uuid rng)
                                 :status "completed"
                                 :output ""
                                 :basis basis})]
    (is (= 1 (count (response/sources item)))
        "Response did not extract one source")))

(deftest the-response-deduplicates-sources
  (let [rng (java.util.Random. 15011)
        url (str "https://example.com/" (uuid rng))
        text (token rng 6 1040 32)
        basis [{:citations [{:url url
                             :title "A"
                             :excerpts [text]}]}
               {:citations [{:url url
                             :title "B"
                             :excerpts [text]}]}]
        item (response/response {:id (uuid rng)
                                 :status "completed"
                                 :output ""
                                 :basis basis})]
    (is (= 1 (count (response/sources item)))
        "Response did not deduplicate sources")))

(deftest the-response-parses-data
  (let [rng (java.util.Random. 15013)
        ident (str "trun_" (uuid rng))
        item (response/response {:id ident
                                 :status "completed"
                                 :output "markdown"
                                 :basis []})]
    (is (= ident (response/id item))
        "Parsed response identifier did not match")))

(deftest the-response-handles-empty-basis
  (let [rng (java.util.Random. 15015)
        item (response/response {:id (uuid rng)
                                 :status "completed"
                                 :output ""
                                 :basis []})]
    (is (= 0 (count (response/sources item)))
        "Response sources was not empty for empty basis")))

(deftest the-response-extracts-confidence
  (let [rng (java.util.Random. 15017)
        confidence "High"
        url (str "https://example.com/" (uuid rng))
        text (token rng 6 1040 32)
        basis [{:citations [{:url url
                             :title "T"
                             :excerpts [text]}]
                :confidence confidence}]
        item (response/response {:id (uuid rng)
                                 :status "completed"
                                 :output ""
                                 :basis basis})
        source (first (response/sources item))]
    (is (= confidence (result/confidence source))
        "Source confidence did not match basis")))

(deftest the-response-handles-missing-confidence
  (let [rng (java.util.Random. 15019)
        url (str "https://example.com/" (uuid rng))
        text (token rng 6 1040 32)
        basis [{:citations [{:url url
                             :title "T"
                             :excerpts [text]}]}]
        item (response/response {:id (uuid rng)
                                 :status "completed"
                                 :output ""
                                 :basis basis})
        source (first (response/sources item))]
    (is (= "" (result/confidence source))
        "Source confidence was not empty when missing")))

(deftest the-response-returns-cost
  (let [rng (java.util.Random. 15021)
        value (/ (.nextInt rng 10000) 100.0)
        output (token rng 6 1040 32)
        item (response/response {:id (uuid rng)
                                 :status "completed"
                                 :output output
                                 :basis []
                                 :cost value})]
    (is (= value (response/cost item))
        "Cost did not return expected value")))

(deftest the-response-strips-utm-from-markdown
  (let [rng (java.util.Random. 15023)
        slug (token rng 6 1040 32)
        link (str "https://example.com/"
                  (.nextInt rng 1000)
                  "?utm_source=valyu.ai&utm_medium=referral&x="
                  (.nextInt rng 9))
        output (str "Источники " slug "\n1. " link)
        item (response/response {:id (uuid rng)
                                 :status "completed"
                                 :output output
                                 :basis []})]
    (is (not (str/includes? (response/text item) "utm_source"))
        "utm parameters were not stripped from markdown")))

(deftest the-response-strips-utm-from-sources
  (let [rng (java.util.Random. 15025)
        slug (token rng 5 12354 32)
        link (str "https://example.com/"
                  (.nextInt rng 1000)
                  "?utm_source=valyu.ai&utm_medium=referral&x="
                  (.nextInt rng 9))
        basis [{:citations [{:url link
                             :title slug
                             :excerpts [slug]}]}]
        item (response/response {:id (uuid rng)
                                 :status "completed"
                                 :output slug
                                 :basis basis})
        source (first (response/sources item))]
    (is (not (str/includes? (result/url source) "utm_source"))
        "utm parameters were not stripped from sources")))

(deftest the-response-preserves-signed-urls
  (let [rng (java.util.Random. 15027)
        text (token rng 6 1040 32)
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
        output (str text " " link)
        item (response/response {:id (uuid rng)
                                 :status "completed"
                                 :output output
                                 :basis []})]
    (is (= output (response/text item))
        "URL was changed despite missing utm parameters")))
