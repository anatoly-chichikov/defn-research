(ns research.image.generator-test
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is]]
            [research.image.generator :as gen]))

(deftest the-generator-replaces-topic
  (let [rng (java.util.Random. 6093)
        size 7
        build (StringBuilder.)]
    (dotimes [_ size]
      (let [pick (.nextInt rng 27)
            code (if (= pick 26) 1040 (+ 97 pick))]
        (.append build (char code))))
    (let [text (.toString build)
          spec "prefix %s suffix"
          out (gen/prompt spec text)]
      (is (str/includes? out text) "Prompt replacement is incorrect"))))
