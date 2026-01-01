(ns research.domain.pending-test
  (:require [clojure.test :refer [deftest is]]
            [research.domain.pending :as pending]))

(defn token
  "Return deterministic token string."
  [rng size base span]
  (let [build (StringBuilder.)]
    (dotimes [_ size]
      (let [pick (.nextInt rng span)
            code (+ base pick)]
        (.append build (char code))))
    (.toString build)))

(deftest the-pending-returns-identifier
  (let [rng (java.util.Random. 13001)
        run (token rng 6 1040 32)
        item (pending/pending {:run_id run
                               :query "test"
                               :processor "pro"
                               :language "english"
                               :provider "parallel"})]
    (is (= run (pending/id item))
        "Pending identifier did not match provided value")))

(deftest the-pending-returns-query
  (let [rng (java.util.Random. 13003)
        query (token rng 6 12354 32)
        item (pending/pending {:run_id "trun_x"
                               :query query
                               :processor "pro"
                               :language "english"
                               :provider "parallel"})]
    (is (= query (pending/query item))
        "Pending query did not match provided value")))

(deftest the-pending-returns-processor
  (let [rng (java.util.Random. 13005)
        processor (token rng 6 945 24)
        item (pending/pending {:run_id "trun_x"
                               :query "test"
                               :processor processor
                               :language "english"
                               :provider "parallel"})]
    (is (= processor (pending/processor item))
        "Pending processor did not match provided value")))

(deftest the-pending-returns-language
  (let [rng (java.util.Random. 13007)
        language (token rng 6 1040 32)
        item (pending/pending {:run_id "trun_x"
                               :query "test"
                               :processor "pro"
                               :language language
                               :provider "parallel"})]
    (is (= language (pending/language item))
        "Pending language did not match provided value")))

(deftest the-pending-serializes-correctly
  (let [rng (java.util.Random. 13009)
        run (token rng 6 1040 32)
        item (pending/pending {:run_id run
                               :query "test"
                               :processor "ultra"
                               :language "русский"
                               :provider "parallel"})
        data (pending/data item)]
    (is (and (contains? data :run_id)
             (contains? data :query)
             (contains? data :processor)
             (contains? data :language))
        "Pending serialize missing fields")))

(deftest the-pending-deserializes-correctly
  (let [rng (java.util.Random. 13011)
        run (token rng 6 1040 32)
        item (pending/pending {:run_id run
                               :query "test"
                               :processor "pro"
                               :language "english"})]
    (is (= run (pending/id item))
        "Pending deserialize did not restore identifier")))

(deftest the-pending-returns-provider
  (let [rng (java.util.Random. 13013)
        name (token rng 6 1040 32)
        item (pending/pending {:run_id "trun_x"
                               :query "test"
                               :processor "pro"
                               :language "english"
                               :provider name})]
    (is (= name (pending/provider item))
        "Pending provider did not match provided value")))

(deftest the-pending-serializes-provider
  (let [rng (java.util.Random. 13015)
        name (token rng 6 12354 32)
        item (pending/pending {:run_id "trun_x"
                               :query "test"
                               :processor "pro"
                               :language "english"
                               :provider name})
        data (pending/data item)]
    (is (= name (:provider data))
        "Pending serialize did not include provider")))
