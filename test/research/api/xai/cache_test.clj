(ns research.api.xai.cache-test
  (:require [clojure.test :refer [deftest is]]
            [research.api.xai.cache :as cache]
            [research.test.ids :as gen])
  (:import (java.nio.file Files)
           (java.nio.file.attribute FileAttribute)))

(deftest the-cache-loads-saved-payload
  (let [rng (gen/ids 18309)
        root (Files/createTempDirectory (gen/ascii rng 8)
                                        (make-array FileAttribute 0))
        item (cache/make root)
        text (gen/cyrillic rng 6)
        id (gen/ascii rng 6)
        data {:query text
              :config {:mode (gen/greek rng 4)}}
        _ (cache/save item id data)
        value (cache/load item id)]
    (is (= text (:query value)) "cache did not load payload")))
