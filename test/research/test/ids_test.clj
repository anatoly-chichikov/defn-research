(ns research.test.ids-test
  (:require [clojure.test :refer [deftest is]]
            [research.test.ids :as gen]))

(deftest the-ids-return-deterministic-values
  (let [base (gen/ids 19001)
        copy (gen/ids 19001)
        token (gen/ascii base 6)
        twin (gen/ascii copy 6)
        mark (gen/uuid base)
        echo (gen/uuid copy)
        match (and (= token twin) (= mark echo))]
    (is match "Seed did not produce deterministic ids")))
