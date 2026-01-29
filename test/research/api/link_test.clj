(ns research.api.link-test
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is]]
            [research.api.link :as link]
            [research.test.ids :as gen]))

(deftest the-link-removes-utm-params
  (let [rng (gen/ids 18001)
        host (gen/ascii rng 6)
        path (gen/cyrillic rng 4)
        token (gen/greek rng 4)
        mark (.nextInt rng 9)
        url (str "https://"
                 host
                 ".com/"
                 path
                 "?utm_source="
                 token
                 "&x="
                 mark)
        item (link/make)
        value (link/clean item url)]
    (is (not (str/includes? value "utm_source"))
        "utm parameters were not removed")))

(deftest the-link-strips-utm-from-text
  (let [rng (gen/ids 18003)
        host (gen/ascii rng 6)
        path (gen/armenian rng 4)
        token (gen/hebrew rng 4)
        url (str "https://"
                 host
                 ".org/"
                 path
                 "?utm_medium="
                 token)
        text (str (gen/cyrillic rng 5) " " url)
        item (link/make)
        value (link/strip item text)]
    (is (not (str/includes? value "utm_medium"))
        "utm parameters were not stripped from text")))

(deftest the-link-extracts-domain
  (let [rng (gen/ids 18005)
        host (gen/ascii rng 6)
        path (gen/greek rng 4)
        url (str "https://www."
                 host
                 ".net/"
                 path)
        item (link/make)
        name (link/domain item url)]
    (is (= (str host ".net") name)
        "domain did not strip www prefix")))

(deftest the-link-collects-urls
  (let [rng (gen/ids 18007)
        left (gen/ascii rng 6)
        right (gen/ascii rng 5)
        path (gen/hiragana rng 4)
        one (str "https://"
                 left
                 ".com/"
                 path)
        two (str "http://"
                 right
                 ".org/"
                 path)
        text (str (gen/cyrillic rng 5) " " one " " two)
        item (link/make)
        items (link/links item text)]
    (is (= [one two] items) "urls were not extracted")))
