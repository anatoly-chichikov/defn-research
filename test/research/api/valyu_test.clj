(ns research.api.valyu-test
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is]]
            [jsonista.core :as json]
            [org.httpkit.client :as http]
            [research.api.research :as research]
            [research.api.response :as response]
            [research.api.valyu :as valyu]
            [research.domain.result :as result])
  (:import (com.sun.net.httpserver HttpServer HttpHandler)
           (java.net InetSocketAddress)
           (java.util.concurrent Executors)))

(defn token
  "Return deterministic token string."
  [rng size base span]
  (let [build (StringBuilder.)]
    (dotimes [_ size]
      (let [pick (.nextInt rng span)
            code (+ base pick)]
        (.append build (char code))))
    (.toString build)))

(defn uuid
  "Return deterministic UUID string."
  [rng]
  (str (java.util.UUID. (.nextLong rng) (.nextLong rng))))

(deftest the-valyu-start-returns-run-identifier
  (let [rng (java.util.Random. 17001)
        run (str "dr_" (.nextInt rng 100000))
        query (token rng 6 880 32)
        model (str "standard-" (.nextInt rng 1000))]
    (with-redefs [http/post (fn [_ _]
                              (delay {:status 200
                                      :body (json/write-value-as-string
                                             {:deepresearch_id run})}))]
      (let [client (valyu/valyu {:key "key"
                                 :base "https://example.com"})
            result (research/start client query model)]
        (is (= run result) "start did not return expected identifier")))))

(deftest the-valyu-start-uses-versioned-endpoint
  (let [rng (java.util.Random. 17002)
        seen (atom "")
        run (str "dr_" (.nextInt rng 100000))
        query (token rng 6 1040 32)
        model (str "standard-" (.nextInt rng 1000))
        client (valyu/valyu {:key "key"
                             :base "https://api.valyu.ai"})]
    (with-redefs [http/post (fn [url _]
                              (reset! seen url)
                              (delay {:status 200
                                      :body (json/write-value-as-string
                                             {:deepresearch_id run})}))]
      (research/start client query model))
    (is (str/includes? @seen "/v1/deepresearch/tasks")
        "Valyu did not use versioned endpoint")))

(deftest ^{:doc "Ensure Valyu preserves processor."}
  the-valyu-start-preserves-processor
  (let [rng (java.util.Random. 17004)
        run (str "dr_" (.nextInt rng 100000))
        query (token rng 6 1040 32)
        key (token rng 5 880 32)
        body (atom "")
        model (token rng 5 1040 32)]
    (with-redefs [http/post (fn [_ req]
                              (reset! body (:body req))
                              (delay {:status 200
                                      :body (json/write-value-as-string
                                             {:deepresearch_id run})}))]
      (let [client (valyu/valyu {:key key
                                 :base "https://example.com"})]
        (research/start client query model)))
    (let [data (json/read-value
                @body
                (json/object-mapper {:decode-key-fn keyword}))]
      (is (= model (:model data))
          "Valyu changed processor"))))

(deftest the-valyu-maps-high-confidence
  (let [rng (java.util.Random. 17003)
        title (token rng 6 1040 32)
        text (token rng 8 880 32)
        url (str "https://example.com/" (.nextInt rng 10000))
        date (str "202"
                  (.nextInt rng 5)
                  "-0"
                  (inc (.nextInt rng 8))
                  "-1"
                  (.nextInt rng 9))
        source {:title title
                :url url
                :content text
                :source (str "source-" (.nextInt rng 1000))
                :source_type "paper"
                :authors [(str "author-" (.nextInt rng 1000))]
                :doi (str "10." (.nextInt rng 1000) "/" (.nextInt rng 10000))
                :publication_date date
                :citation_count 1
                :relevance_score 0.9}
        client (valyu/valyu {:key "key"
                             :base "https://example.com"})
        base (research/basis client [source])
        source (first
                (response/sources
                 (response/response {:id "x"
                                     :status "completed"
                                     :output ""
                                     :basis base})))]
    (is (= "High" (result/confidence source))
        "confidence was not high for paper with doi")))

(deftest the-valyu-maps-unknown-confidence
  (let [rng (java.util.Random. 17005)
        title (token rng 6 1328 32)
        text (token rng 9 1424 32)
        url (str "https://example.com/" (.nextInt rng 10000))
        source {:title title
                :url url
                :content text}
        client (valyu/valyu {:key "key"
                             :base "https://example.com"})
        base (research/basis client [source])
        source (first
                (response/sources
                 (response/response {:id "x"
                                     :status "completed"
                                     :output ""
                                     :basis base})))]
    (is (= "Unknown" (result/confidence source))
        "confidence was not unknown for missing metadata")))

(deftest the-valyu-maps-medium-confidence-trusted
  (let [rng (java.util.Random. 17007)
        title (token rng 7 1040 32)
        text (token rng 8 12354 32)
        url (str "https://www.wikipedia.org/" (.nextInt rng 10000))
        source {:title title
                :url url
                :content text}
        client (valyu/valyu {:key "key"
                             :base "https://example.com"})
        base (research/basis client [source])
        source (first
                (response/sources
                 (response/response {:id "x"
                                     :status "completed"
                                     :output ""
                                     :basis base})))]
    (is (= "Medium" (result/confidence source))
        "confidence was not medium for trusted domain")))

(deftest the-valyu-maps-medium-confidence-public
  (let [rng (java.util.Random. 17009)
        title (token rng 6 1328 32)
        text (token rng 9 1040 32)
        url (str "https://www.nasa.gov/" (.nextInt rng 10000))
        source {:title title
                :url url
                :content text}
        client (valyu/valyu {:key "key"
                             :base "https://example.com"})
        base (research/basis client [source])
        source (first
                (response/sources
                 (response/response {:id "x"
                                     :status "completed"
                                     :output ""
                                     :basis base})))]
    (is (= "Medium" (result/confidence source))
        "confidence was not medium for gov domain")))

(deftest the-valyu-maps-low-confidence
  (let [rng (java.util.Random. 17011)
        text (token rng 7 12354 32)
        url (str "https://example.com/" (.nextInt rng 10000))
        date (str "202"
                  (.nextInt rng 5)
                  "-0"
                  (inc (.nextInt rng 8))
                  "-2"
                  (.nextInt rng 9))
        source {:title text
                :url url
                :content text
                :source "web"
                :source_type "web"
                :authors []
                :publication_date date
                :relevance_score 0.2}
        client (valyu/valyu {:key "key"
                             :base "https://example.com"})
        base (research/basis client [source])
        source (first
                (response/sources
                 (response/response {:id "x"
                                     :status "completed"
                                     :output ""
                                     :basis base})))]
    (is (= "Low" (result/confidence source))
        "confidence was not low for low relevance")))

(deftest the-valyu-reads-progress-messages
  (let [rng (java.util.Random. 17013)
        token (token rng 6 1040 32)
        seen {}
        value {:messages [{:message token}]}
        result (first (valyu/message value seen "trun_x"))]
    (is (= token result) "message was not returned")))

(deftest the-valyu-formats-list-messages
  (let [rng (java.util.Random. 17015)
        left (token rng 4 1040 32)
        right (token rng 4 1328 32)
        seen {}
        value {:messages [{:message [left right]}]}
        result (first (valyu/message value seen "trun_x"))]
    (is (= (str left " " right) result) "list message was not joined")))

(deftest the-valyu-uses-raw-status-payload
  (let [rng (java.util.Random. 17017)
        ident (str "dr_" (.nextInt rng 100000))
        output (token rng 6 1040 32)
        title (token rng 5 880 32)
        url (str "http://example.com/" (.nextInt rng 1000))
        payload {:success true
                 :status "completed"
                 :output {:markdown output}
                 :sources [{:title title
                            :url url
                            :content output}]
                 :images [{:image_id (str "img_" (.nextInt rng 1000))
                           :image_type "chart"
                           :deepresearch_id ident
                           :title title
                           :description output
                           :image_url url
                           :s3_key (str "key_" (.nextInt rng 1000))
                           :created_at (.nextInt rng 1000)
                           :chart_type "doughnut"}]}
        server (HttpServer/create (InetSocketAddress. "127.0.0.1" 0) 0)
        handler (reify
                  HttpHandler
                  (handle [_ exchange]
                    (let [body (json/write-value-as-bytes payload)]
                      (.sendResponseHeaders exchange 200 (alength body))
                      (with-open [stream (.getResponseBody exchange)]
                        (.write stream body)))))
        exec (Executors/newSingleThreadExecutor)
        _ (.createContext server "/deepresearch/tasks" handler)
        _ (.setExecutor server exec)
        _ (.start server)
        port (.getPort (.getAddress server))
        base (str "http://127.0.0.1:" port)
        client (valyu/valyu {:key "key"
                             :base base})
        value (loop [count 0]
                (let [result (try
                               {:value (research/finish client ident)}
                               (catch Exception exc
                                 {:error exc}))]
                  (if-let [error (:error result)]
                    (if (>= count 2)
                      (do (.stop server 0)
                          (.shutdownNow exec)
                          (throw error))
                      (do (Thread/sleep 50) (recur (inc count))))
                    (:value result))))]
    (.stop server 0)
    (.shutdownNow exec)
    (is (= output (response/text value))
        "Markdown did not match raw payload")))
