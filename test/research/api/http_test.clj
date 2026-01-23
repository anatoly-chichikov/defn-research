(ns research.api.http-test
  (:require [clojure.test :refer [deftest is]]
            [org.httpkit.client :as http]
            [research.api.http :as request]
            [research.test.ids :as gen]))

(deftest the-http-get-returns-response
  (let [rng (gen/ids 18201)
        host (gen/ascii rng 6)
        path (gen/cyrillic rng 4)
        code (+ 200 (.nextInt rng 50))
        body (gen/greek rng 6)
        url (str "https://" host ".com/" path)]
    (with-redefs [http/get (fn [_ _] (delay {:status code
                                             :body body}))]
      (let [item (request/make)
            result (request/get item url {:timeout 1})
            value (:status @result)]
        (is (= code value) "http get did not return response")))))

(deftest the-http-post-returns-response
  (let [rng (gen/ids 18203)
        host (gen/ascii rng 6)
        path (gen/armenian rng 4)
        code (+ 200 (.nextInt rng 50))
        body (gen/hebrew rng 6)
        url (str "https://" host ".org/" path)]
    (with-redefs [http/post (fn [_ _] (delay {:status code
                                              :body body}))]
      (let [item (request/make)
            result (request/post item url {:timeout 1})
            value (:status @result)]
        (is (= code value) "http post did not return response")))))
