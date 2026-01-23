(ns research.api.valyu.sources-test
  (:require [clojure.test :refer [deftest is]]
            [research.api.link :as link]
            [research.api.valyu.sources :as sources]
            [research.test.ids :as gen]))

(deftest the-sources-maps-high-confidence
  (let [rng (gen/ids 18405)
        title (gen/cyrillic rng 6)
        text (gen/greek rng 8)
        url (str "https://example.com/" (.nextInt rng 10000))
        date (str "202"
                  (.nextInt rng 5)
                  "-0"
                  (inc (.nextInt rng 8))
                  "-1"
                  (.nextInt rng 9))
        data {:title title
              :url url
              :content text
              :source (str "source-" (.nextInt rng 1000))
              :source_type "paper"
              :authors [(str "author-" (.nextInt rng 1000))]
              :doi (str "10." (.nextInt rng 1000) "/" (.nextInt rng 10000))
              :publication_date date
              :citation_count 1
              :relevance_score 0.9}
        item (sources/make {:science #{}
                            :finance #{}
                            :trust #{}
                            :link (link/make)})
        value (sources/level item data)]
    (is (= "High" value) "confidence was not high for paper with doi")))

(deftest the-sources-maps-unknown-confidence
  (let [rng (gen/ids 18407)
        title (gen/armenian rng 6)
        text (gen/hebrew rng 9)
        url (str "https://example.com/" (.nextInt rng 10000))
        data {:title title
              :url url
              :content text}
        item (sources/make {:science #{}
                            :finance #{}
                            :trust #{}
                            :link (link/make)})
        value (sources/level item data)]
    (is (= "Unknown" value)
        "confidence was not unknown for missing metadata")))

(deftest the-sources-map-medium-confidence-trusted
  (let [rng (gen/ids 18409)
        title (gen/cyrillic rng 7)
        text (gen/hiragana rng 8)
        url (str "https://www.wikipedia.org/" (.nextInt rng 10000))
        data {:title title
              :url url
              :content text}
        item (sources/make {:science #{}
                            :finance #{}
                            :trust #{"wikipedia.org"}
                            :link (link/make)})
        value (sources/level item data)]
    (is (= "Medium" value)
        "confidence was not medium for trusted domain")))

(deftest the-sources-map-medium-confidence-public
  (let [rng (gen/ids 18411)
        title (gen/armenian rng 6)
        text (gen/cyrillic rng 9)
        url (str "https://www.nasa.gov/" (.nextInt rng 10000))
        data {:title title
              :url url
              :content text}
        item (sources/make {:science #{}
                            :finance #{}
                            :trust #{}
                            :link (link/make)})
        value (sources/level item data)]
    (is (= "Medium" value)
        "confidence was not medium for gov domain")))

(deftest the-sources-map-low-confidence
  (let [rng (gen/ids 18413)
        text (gen/hiragana rng 7)
        url (str "https://example.com/" (.nextInt rng 10000))
        date (str "202"
                  (.nextInt rng 5)
                  "-0"
                  (inc (.nextInt rng 8))
                  "-2"
                  (.nextInt rng 9))
        data {:title text
              :url url
              :content text
              :source "web"
              :source_type "web"
              :authors []
              :publication_date date
              :relevance_score 0.2}
        item (sources/make {:science #{}
                            :finance #{}
                            :trust #{}
                            :link (link/make)})
        value (sources/level item data)]
    (is (= "Low" value) "confidence was not low for low relevance")))
