(ns research.api.progress-test
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is]]
            [research.api.progress :as progress]
            [research.test.ids :as gen]))

(deftest the-progress-cleans-periods
  (let [rng (gen/ids 18101)
        text (gen/cyrillic rng 6)
        value (str text "." text ".")
        item (progress/make)
        note (progress/clean item value)]
    (is (not (str/includes? note "."))
        "progress clean did not remove periods")))

(deftest the-progress-emits-text
  (let [rng (gen/ids 18103)
        text (gen/greek rng 6)
        item (progress/make)
        flag (with-redefs [clojure.core/println (fn [& _] nil)]
               (progress/emit item text))]
    (is flag "progress emit did not return success")))
