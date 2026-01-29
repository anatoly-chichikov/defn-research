(ns research.api.xai.py-client.fetch-test
  (:require [clojure.test :refer [deftest is]]
            [research.api.xai.py-client.fetch :as fetch]
            [research.test.ids :as gen]))

(deftest the-line-removes-periods-from-prompts
  (let [rng (gen/ids 18313)
        alpha (gen/greek rng 5)
        beta (gen/greek rng 4)
        dot (char 46)
        gap (str (char 10) (char 10))
        text (str alpha dot gap beta dot)
        line (#'fetch/line text)
        expect (str "xai prompt " text)]
    (is (= expect line) "prompt line was not sanitized")))
