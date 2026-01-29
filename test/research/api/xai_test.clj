(ns research.api.xai-test
  (:require [clojure.test :refer [deftest is]]
            [jsonista.core :as json]
            [research.api.research :as research]
            [research.api.response :as response]
            [research.api.xai :as xai]
            [research.api.xai.py-client :as py-client]
            [research.test.ids :as gen])
  (:import (java.nio.file Files)
           (java.nio.file.attribute FileAttribute)))

(deftest ^{:doc "Xai start stores query in cache."}
  the-xai-start-stores-query
  (let [rng (gen/ids 17001)
        root (Files/createTempDirectory (gen/ascii rng 8)
                                        (make-array FileAttribute 0))
        query (gen/cyrillic rng 6)
        model (gen/latin rng 6)
        mode (gen/greek rng 5)
        unit (reify py-client/Bound
               (run [_ _ _] {}))
        item (xai/xai {:root root
                       :model model
                       :mode mode
                       :turns 2
                       :window 3
                       :tokens 4
                       :follow false
                       :section false
                       :domains [(gen/armenian rng 4)]
                       :unit unit})
        path (.resolve root "tmp_cache")
        path (.resolve path "xai")]
    (research/start item query "365")
    (let [file (with-open [stream (Files/newDirectoryStream path)]
                 (first (iterator-seq (.iterator stream))))
          data (json/read-value
                (.toFile file)
                (json/object-mapper {:decode-key-fn keyword}))]
      (is (= query (:query data)) "Query was not stored"))))

(deftest ^{:doc "Xai start uses fixed window."}
  the-xai-start-uses-window
  (let [rng (gen/ids 17002)
        root (Files/createTempDirectory (gen/ascii rng 8)
                                        (make-array FileAttribute 0))
        query (gen/cyrillic rng 5)
        unit (reify py-client/Bound
               (run [_ _ _] {}))
        item (xai/xai {:root root
                       :model (gen/latin rng 6)
                       :mode (gen/greek rng 4)
                       :turns 2
                       :window 365
                       :tokens 4
                       :follow false
                       :section false
                       :domains [(gen/armenian rng 4)]
                       :unit unit})
        path (.resolve root "tmp_cache")
        path (.resolve path "xai")]
    (research/start item query "90")
    (let [file (with-open [stream (Files/newDirectoryStream path)]
                 (first (iterator-seq (.iterator stream))))
          data (json/read-value
                (.toFile file)
                (json/object-mapper {:decode-key-fn keyword}))
          pack (:config data)
          days (:window pack)]
      (is (= 365 days) "Window did not use fixed year"))))

(deftest ^{:doc "Xai start ignores processor."}
  the-xai-start-ignores-processor
  (let [rng (gen/ids 17004)
        root (Files/createTempDirectory (gen/ascii rng 8)
                                        (make-array FileAttribute 0))
        query (gen/cyrillic rng 5)
        bad (gen/hiragana rng 4)
        unit (reify py-client/Bound
               (run [_ _ _] {}))
        item (xai/xai {:root root
                       :model (gen/latin rng 6)
                       :mode (gen/greek rng 4)
                       :turns 2
                       :window 365
                       :tokens 4
                       :follow false
                       :section false
                       :domains [(gen/armenian rng 4)]
                       :unit unit})
        raised (atom false)]
    (try
      (research/start item query bad)
      (catch Exception _ (reset! raised true)))
    (is (false? @raised) "Invalid processor was not ignored")))

(deftest ^{:doc "Xai finish returns markdown output."}
  the-xai-finish-returns-markdown
  (let [rng (gen/ids 17003)
        root (Files/createTempDirectory (gen/ascii rng 8)
                                        (make-array FileAttribute 0))
        query (gen/cyrillic rng 7)
        model (gen/latin rng 7)
        text (gen/hebrew rng 8)
        code (gen/ascii rng 10)
        unit (reify py-client/Bound
               (run [_ _ _]
                 {:run {:run_id code
                        :status "completed"}
                  :output {:content text
                           :basis []}}))
        item (xai/xai {:root root
                       :model model
                       :mode (gen/greek rng 4)
                       :turns 5
                       :window 6
                       :tokens 7
                       :follow true
                       :section true
                       :domains [(gen/arabic rng 4)]
                       :unit unit})
        run (research/start item query "365")
        result (research/finish item run)]
    (is (= text (response/text result)) "Markdown did not match output")))
