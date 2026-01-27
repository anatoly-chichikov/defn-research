(ns research.domain.pending-test
  (:require [clojure.test :refer [deftest is]]
            [research.domain.pending :as pending]
            [research.test.ids :as gen]))

(deftest the-pending-returns-identifier
  (let [rng (gen/ids 13001)
        run (gen/cyrillic rng 6)
        query (str (gen/hiragana rng 6)
                   "\n\nResearch:\n1. "
                   (gen/greek rng 4))
        processor (gen/greek rng 6)
        language (gen/cyrillic rng 6)
        provider (gen/cyrillic rng 6)
        item (pending/pending {:run_id run
                               :query query
                               :processor processor
                               :language language
                               :provider provider})]
    (is (= run (pending/id item))
        "Pending identifier did not match provided value")))

(deftest the-pending-returns-query
  (let [rng (gen/ids 13003)
        run (gen/cyrillic rng 6)
        query (str (gen/hiragana rng 6)
                   "\n\nResearch:\n1. "
                   (gen/greek rng 4))
        processor (gen/greek rng 6)
        language (gen/cyrillic rng 6)
        provider (gen/cyrillic rng 6)
        item (pending/pending {:run_id run
                               :query query
                               :processor processor
                               :language language
                               :provider provider})
        expect (str "Язык ответа: " language ".\n\n" query)]
    (is (= expect (pending/query item))
        "Pending query did not match provided value")))

(deftest the-pending-returns-processor
  (let [rng (gen/ids 13005)
        run (gen/cyrillic rng 6)
        query (str (gen/hiragana rng 6)
                   "\n\nResearch:\n1. "
                   (gen/greek rng 4))
        processor (gen/greek rng 6)
        language (gen/cyrillic rng 6)
        provider (gen/cyrillic rng 6)
        item (pending/pending {:run_id run
                               :query query
                               :processor processor
                               :language language
                               :provider provider})]
    (is (= processor (pending/processor item))
        "Pending processor did not match provided value")))

(deftest the-pending-returns-language
  (let [rng (gen/ids 13007)
        run (gen/cyrillic rng 6)
        query (str (gen/hiragana rng 6)
                   "\n\nResearch:\n1. "
                   (gen/greek rng 4))
        processor (gen/greek rng 6)
        language (gen/cyrillic rng 6)
        provider (gen/cyrillic rng 6)
        item (pending/pending {:run_id run
                               :query query
                               :processor processor
                               :language language
                               :provider provider})]
    (is (= language (pending/language item))
        "Pending language did not match provided value")))

(deftest the-pending-serializes-correctly
  (let [rng (gen/ids 13009)
        run (gen/cyrillic rng 6)
        query (str (gen/hiragana rng 6)
                   "\n\nResearch:\n1. "
                   (gen/greek rng 4))
        processor (gen/greek rng 6)
        language (gen/cyrillic rng 6)
        provider (gen/cyrillic rng 6)
        item (pending/pending {:run_id run
                               :query query
                               :processor processor
                               :language language
                               :provider provider})
        data (pending/data item)
        brief (:brief data)
        items (:items brief)
        node (first items)]
    (is (and (contains? data :run_id)
             (contains? data :processor)
             (contains? data :language)
             (contains? data :brief)
             (contains? brief :topic)
             (contains? brief :items)
             (contains? node :text)
             (contains? node :items)
             (not (contains? brief :text))
             (not (contains? data :query)))
        "Pending serialize did not include brief or still included query")))

(deftest the-pending-deserializes-correctly
  (let [rng (gen/ids 13011)
        run (gen/cyrillic rng 6)
        query (gen/hiragana rng 6)
        processor (gen/greek rng 6)
        language (gen/cyrillic rng 6)
        provider (gen/cyrillic rng 6)
        item (pending/pending {:run_id run
                               :query query
                               :processor processor
                               :language language
                               :provider provider})]
    (is (= run (pending/id item))
        "Pending deserialize did not restore identifier")))

(deftest the-pending-returns-provider
  (let [rng (gen/ids 13013)
        run (gen/cyrillic rng 6)
        query (gen/hiragana rng 6)
        processor (gen/greek rng 6)
        language (gen/cyrillic rng 6)
        name (gen/cyrillic rng 6)
        item (pending/pending {:run_id run
                               :query query
                               :processor processor
                               :language language
                               :provider name})]
    (is (= name (pending/provider item))
        "Pending provider did not match provided value")))

(deftest the-pending-serializes-provider
  (let [rng (gen/ids 13015)
        run (gen/cyrillic rng 6)
        query (gen/hiragana rng 6)
        processor (gen/greek rng 6)
        language (gen/cyrillic rng 6)
        name (gen/hiragana rng 6)
        item (pending/pending {:run_id run
                               :query query
                               :processor processor
                               :language language
                               :provider name})
        data (pending/data item)]
    (is (= name (:provider data))
        "Pending serialize did not include provider")))
