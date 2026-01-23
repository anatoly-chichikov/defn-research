(ns research.api.xai.bridge-test
  (:require [clojure.test :refer [deftest is]]
            [research.api.xai.bridge :as bridge]
            [research.test.ids :as gen]))

(deftest the-bridge-note-stringifies-tools
  (let [rng (gen/ids 18301)
        model (gen/latin rng 6)
        tool (gen/cyrillic rng 5)
        text (gen/greek rng 7)
        turn (inc (.nextInt rng 4))
        token (+ 1 (.nextInt rng 1000))
        items [(gen/armenian rng 4) (gen/hebrew rng 4)]
        node (reify Object
               (toString [_] tool)
               (hashCode [_] (throw (RuntimeException. tool))))
        note (#'bridge/note model turn token items [node] text)]
    (is (= [tool] (:tools note)) "tools were not stringified")))
