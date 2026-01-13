(ns research.api.valyu-test
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is]]
            [jsonista.core :as json]
            [org.httpkit.client :as http]
            [research.api.research :as research]
            [research.api.response :as response]
            [research.api.valyu :as valyu]
            [research.domain.result :as result]
            [research.test.ids :as gen])
  (:import (com.sun.net.httpserver HttpServer HttpHandler)
           (java.net InetSocketAddress)
           (java.util.concurrent Executors)))

(deftest the-valyu-start-returns-run-identifier
  (let [rng (gen/ids 17001)
        run (str "dr_" (.nextInt rng 100000))
        query (gen/greek rng 6)
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
  (let [rng (gen/ids 17002)
        seen (atom "")
        run (str "dr_" (.nextInt rng 100000))
        query (gen/cyrillic rng 6)
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
  (let [rng (gen/ids 17004)
        run (str "dr_" (.nextInt rng 100000))
        query (gen/cyrillic rng 6)
        key (gen/greek rng 5)
        body (atom "")
        model (gen/cyrillic rng 5)]
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
  (let [rng (gen/ids 17003)
        title (gen/cyrillic rng 6)
        text (gen/greek rng 8)
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
  (let [rng (gen/ids 17005)
        title (gen/armenian rng 6)
        text (gen/hebrew rng 9)
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
  (let [rng (gen/ids 17007)
        title (gen/cyrillic rng 7)
        text (gen/hiragana rng 8)
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
  (let [rng (gen/ids 17009)
        title (gen/armenian rng 6)
        text (gen/cyrillic rng 9)
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
  (let [rng (gen/ids 17011)
        text (gen/hiragana rng 7)
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
  (let [rng (gen/ids 17013)
        token (gen/cyrillic rng 6)
        seen {}
        value {:messages [{:message token}]}
        result (first (valyu/message value seen "trun_x"))]
    (is (= token result) "message was not returned")))

(deftest the-valyu-formats-list-messages
  (let [rng (gen/ids 17015)
        left (gen/cyrillic rng 4)
        right (gen/armenian rng 4)
        seen {}
        value {:messages [{:message [left right]}]}
        result (first (valyu/message value seen "trun_x"))]
    (is (= (str left " " right) result) "list message was not joined")))

(deftest ^{:doc "Ensure Valyu retries transient errors"}
  the-valyu-retries-transient-errors
  (let [rng (gen/ids 17019)
        id (gen/cyrillic rng 6)
        key (gen/greek rng 5)
        base (gen/latin rng 6)
        fault (+ 500 (.nextInt rng 50))
        success (+ 200 (.nextInt rng 50))
        state (gen/armenian rng 6)
        body (json/write-value-as-string {:status state})
        count (atom 0)]
    (with-redefs-fn {#'http/get (fn [_ _]
                                  (swap! count inc)
                                  (delay (if (= @count 1)
                                           {:status fault
                                            :body (gen/greek rng 3)}
                                           {:status success
                                            :body body})))
                     #'valyu/pause (fn [_] nil)
                     #'clojure.core/println (fn [& _] nil)}
      (fn []
        (let [data (valyu/valyu-status {:base base
                                        :key key} id)]
          (is (= state (:status data))
              "valyu did not recover from transient error"))))))

(deftest ^{:doc "Ensure Valyu retries missing status"}
  the-valyu-retries-missing-status
  (let [rng (gen/ids 17021)
        id (gen/cyrillic rng 6)
        key (gen/hebrew rng 5)
        base (gen/latin rng 6)
        success (+ 200 (.nextInt rng 50))
        state (gen/hiragana rng 6)
        body (json/write-value-as-string {:status state})
        count (atom 0)]
    (with-redefs-fn {#'http/get (fn [_ _]
                                  (swap! count inc)
                                  (delay (if (= @count 1)
                                           nil
                                           {:status success
                                            :body body})))
                     #'valyu/pause (fn [_] nil)
                     #'clojure.core/println (fn [& _] nil)}
      (fn []
        (let [data (valyu/valyu-status {:base base
                                        :key key} id)]
          (is (= state (:status data))
              "valyu did not recover from missing status"))))))

(deftest ^{:doc "Ensure Valyu polls every three minutes"}
  the-valyu-polls-every-three-minutes
  (let [rng (gen/ids 17023)
        id (gen/cyrillic rng 6)
        key (gen/greek rng 5)
        base (gen/latin rng 6)
        mark (atom 0)
        count (atom 0)
        client (valyu/valyu {:key key
                             :base base})
        phase (gen/arabic rng 6)]
    (with-redefs-fn {#'valyu/pause (fn [span] (reset! mark span))
                     #'valyu/valyu-status (fn [_ _]
                                            (swap! count inc)
                                            (if (= @count 1)
                                              {:status phase}
                                              {:status "completed"}))
                     #'clojure.core/println (fn [& _] nil)}
      (fn []
        (research/stream client id)))
    (is (= 180000 @mark) "valyu did not wait three minutes between polls")))

(deftest the-valyu-uses-raw-status-payload
  (let [rng (gen/ids 17017)
        ident (str "dr_" (.nextInt rng 100000))
        output (gen/cyrillic rng 6)
        title (gen/greek rng 5)
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
