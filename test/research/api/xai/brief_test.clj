(ns research.api.xai.brief-test
  (:require [clojure.test :refer [deftest is]]
            [research.api.xai.brief :as brief]
            [research.test.ids :as gen]))

(deftest the-brief-parses-items
  (let [rng (gen/ids 18307)
        head (gen/cyrillic rng 5)
        left (gen/greek rng 4)
        right (gen/armenian rng 4)
        text (str head "\n\nResearch:\n1. " left "\n2. " right)
        item (brief/make)
        info (brief/parts item text)
        items (:items info)
        expect [(str "1. " left) (str "2. " right)]]
    (is (= expect items) "brief did not parse items")))
