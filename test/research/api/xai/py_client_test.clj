(ns research.api.xai.py-client-test
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is]]
            [research.api.xai.py-client :as py-client]
            [research.test.ids :as gen]))

(deftest the-py-client-note-stringifies-tools
  (let [rng (gen/ids 18301)
        model (gen/latin rng 6)
        tool (gen/arabic rng 5)
        text (gen/greek rng 7)
        turn (inc (.nextInt rng 4))
        token (+ 1 (.nextInt rng 1000))
        items [(gen/armenian rng 4) (gen/hebrew rng 4)]
        node (reify Object
               (toString [_] tool)
               (hashCode [_] (throw (RuntimeException. tool))))
        note (#'py-client/note model turn token items [node] text)]
    (is (= [tool] (:tools note)) "tools were not stringified")))

(deftest the-py-client-renumbers-citations
  (let [rng (gen/ids 18321)
        url1 (str "https://example.com/" (gen/ascii rng 6))
        url2 (str "https://example.org/" (gen/ascii rng 6))
        head (gen/greek rng 4)
        text (str head " [[1]](" url1 ") x [[1]](" url2 ") y [[1]](" url1 ")")
        name (gen/greek rng 5)
        marks [{:url url1
                :title name}
               {:url url2
                :title ""}]
        data (#'py-client/order text marks)
        body (:text data)
        list (:list data)
        names (:name data)
        check (and (str/includes? body (str "[[1]](" url1 ")"))
                   (str/includes? body (str "[[2]](" url2 ")"))
                   (= [url1 url2] list)
                   (= name (get names url1)))]
    (is check "citations were not renumbered")))
