(ns research.domain.pending-test
  (:require [clojure.test :refer [deftest is]]
            [research.domain.pending :as pending]))

(defn token
  "Return deterministic token string."
  [dice size base span]
  (let [build (StringBuilder.)]
    (dotimes [_ size]
      (let [pick (.nextInt dice span)
            code (+ base pick)]
        (.append build (char code))))
    (.toString build)))

(deftest the-pending-returns-identifier
  (let [dice (java.util.Random. 13001)
        run (token dice 6 1040 32)
        query (token dice 6 12354 32)
        processor (token dice 6 945 24)
        language (token dice 6 1040 32)
        provider (token dice 6 1040 32)
        item (pending/pending {:run_id run
                               :query query
                               :processor processor
                               :language language
                               :provider provider})]
    (is (= run (pending/id item))
        "Pending identifier did not match provided value")))

(deftest the-pending-returns-query
  (let [dice (java.util.Random. 13003)
        run (token dice 6 1040 32)
        query (token dice 6 12354 32)
        processor (token dice 6 945 24)
        language (token dice 6 1040 32)
        provider (token dice 6 1040 32)
        item (pending/pending {:run_id run
                               :query query
                               :processor processor
                               :language language
                               :provider provider})]
    (is (= query (pending/query item))
        "Pending query did not match provided value")))

(deftest the-pending-returns-processor
  (let [dice (java.util.Random. 13005)
        run (token dice 6 1040 32)
        query (token dice 6 12354 32)
        processor (token dice 6 945 24)
        language (token dice 6 1040 32)
        provider (token dice 6 1040 32)
        item (pending/pending {:run_id run
                               :query query
                               :processor processor
                               :language language
                               :provider provider})]
    (is (= processor (pending/processor item))
        "Pending processor did not match provided value")))

(deftest the-pending-returns-language
  (let [dice (java.util.Random. 13007)
        run (token dice 6 1040 32)
        query (token dice 6 12354 32)
        processor (token dice 6 945 24)
        language (token dice 6 1040 32)
        provider (token dice 6 1040 32)
        item (pending/pending {:run_id run
                               :query query
                               :processor processor
                               :language language
                               :provider provider})]
    (is (= language (pending/language item))
        "Pending language did not match provided value")))

(deftest the-pending-serializes-correctly
  (let [dice (java.util.Random. 13009)
        run (token dice 6 1040 32)
        query (token dice 6 12354 32)
        processor (token dice 6 945 24)
        language (token dice 6 1040 32)
        provider (token dice 6 1040 32)
        item (pending/pending {:run_id run
                               :query query
                               :processor processor
                               :language language
                               :provider provider})
        data (pending/data item)]
    (is (and (contains? data :run_id)
             (contains? data :processor)
             (contains? data :language)
             (not (contains? data :query)))
        "Pending serialize included query")))

(deftest the-pending-deserializes-correctly
  (let [dice (java.util.Random. 13011)
        run (token dice 6 1040 32)
        query (token dice 6 12354 32)
        processor (token dice 6 945 24)
        language (token dice 6 1040 32)
        provider (token dice 6 1040 32)
        item (pending/pending {:run_id run
                               :query query
                               :processor processor
                               :language language
                               :provider provider})]
    (is (= run (pending/id item))
        "Pending deserialize did not restore identifier")))

(deftest the-pending-returns-provider
  (let [dice (java.util.Random. 13013)
        run (token dice 6 1040 32)
        query (token dice 6 12354 32)
        processor (token dice 6 945 24)
        language (token dice 6 1040 32)
        name (token dice 6 1040 32)
        item (pending/pending {:run_id run
                               :query query
                               :processor processor
                               :language language
                               :provider name})]
    (is (= name (pending/provider item))
        "Pending provider did not match provided value")))

(deftest the-pending-serializes-provider
  (let [dice (java.util.Random. 13015)
        run (token dice 6 1040 32)
        query (token dice 6 12354 32)
        processor (token dice 6 945 24)
        language (token dice 6 1040 32)
        name (token dice 6 12354 32)
        item (pending/pending {:run_id run
                               :query query
                               :processor processor
                               :language language
                               :provider name})
        data (pending/data item)]
    (is (= name (:provider data))
        "Pending serialize did not include provider")))
