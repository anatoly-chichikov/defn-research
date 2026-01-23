(ns research.api.valyu-test
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is]]
            [jsonista.core :as json]
            [org.httpkit.client :as http]
            [research.api.research :as research]
            [research.api.response :as response]
            [research.api.valyu :as valyu]
            [research.api.valyu.status :as status]
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

(deftest ^{:doc "Ensure Valyu polls every three minutes"}
  the-valyu-polls-every-three-minutes
  (let [rng (gen/ids 17023)
        id (gen/cyrillic rng 6)
        key (gen/greek rng 5)
        base (gen/latin rng 6)
        mark (atom 0)
        count (atom 0)
        phase (gen/arabic rng 6)
        unit (reify status/Statused
               (status [_ _]
                 (swap! count inc)
                 (if (= @count 1)
                   {:status phase}
                   {:status "completed"}))
               (pause [_ span] (reset! mark span)))
        client (valyu/valyu {:key key
                             :base base
                             :state unit})]
    (with-redefs-fn {#'clojure.core/println (fn [& _] nil)}
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
