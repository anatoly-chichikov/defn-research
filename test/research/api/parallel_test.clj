(ns research.api.parallel-test
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is]]
            [jsonista.core :as json]
            [org.httpkit.client :as http]
            [research.api.parallel :as parallel]
            [research.api.research :as research]
            [research.api.response :as response]))

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

(deftest the-parallel-returns-client
  (let [rng (java.util.Random. 16001)
        key (token rng 6 1040 32)
        client (with-redefs [parallel/env (fn [_] key)]
                 (parallel/parallel))]
    (is (instance?
         research.api.parallel.Parallel
         client)
        "Parallel client was not created")))

(deftest the-parallel-uses-environment
  (let [rng (java.util.Random. 16003)
        key (token rng 6 1040 32)
        client (with-redefs [parallel/env (fn [_] key)]
                 (parallel/parallel))]
    (is (= key (:key client)) "Parallel key did not match environment")))

(deftest the-parallel-raises-without-key
  (let [raised (atom false)]
    (try
      (with-redefs [parallel/env (fn [_] "")]
        (parallel/parallel))
      (catch Exception _ (reset! raised true)))
    (is @raised "Parallel did not raise when key missing")))

(deftest the-parallel-start-returns-run-id
  (let [rng (java.util.Random. 16007)
        run (str "trun_" (uuid rng))]
    (with-redefs [parallel/env (fn [_] "key")
                  http/post (fn [_ _]
                             (delay {:status 200
                                     :body (json/write-value-as-string
                                            {:run_id run})}))]
      (let [client (parallel/parallel)
            result (research/start client "query" "pro")]
        (is (= run result) "start did not return expected run_id")))))

(deftest the-parallel-start-accepts-accepted-status
  (let [rng (java.util.Random. 16008)
        run (str "trun_" (uuid rng))]
    (with-redefs [parallel/env (fn [_] "key")
                  http/post (fn [_ _]
                             (delay {:status 202
                                     :body (json/write-value-as-string
                                            {:run_id run})}))]
      (let [client (parallel/parallel)
            result (research/start client "query" "pro")]
        (is (= run result) "Accepted status was not handled")))))

(deftest the-parallel-start-passes-query
  (let [rng (java.util.Random. 16009)
        query (token rng 6 1040 32)
        holder (atom "")]
    (with-redefs [parallel/env (fn [_] "key")
                  http/post (fn [_ opts]
                             (reset! holder (:body opts))
                             (delay {:status 200
                                     :body (json/write-value-as-string
                                            {:run_id "trun_x"})}))]
      (let [client (parallel/parallel)]
        (research/start client query "pro")))
    (let [data (json/read-value
                @holder
                (json/object-mapper {:decode-key-fn keyword}))]
      (is (= query (:input data)) "Query was not passed to create"))))

(deftest the-parallel-start-passes-processor
  (let [rng (java.util.Random. 16011)
        processor (token rng 5 12354 32)
        holder (atom "")]
    (with-redefs [parallel/env (fn [_] "key")
                  http/post (fn [_ opts]
                             (reset! holder (:body opts))
                             (delay {:status 200
                                     :body (json/write-value-as-string
                                            {:run_id "trun_x"})}))]
      (let [client (parallel/parallel)]
        (research/start client "query" processor)))
    (let [data (json/read-value
                @holder
                (json/object-mapper {:decode-key-fn keyword}))]
      (is (= processor (:processor data))
          "Processor was not passed to create"))))

(deftest the-parallel-finish-returns-completed-response
  (let [rng (java.util.Random. 16013)
        run (str "trun_" (uuid rng))
        body {:run {:run_id run
                    :status "completed"}
              :output {:content "result"
                       :basis []}}]
    (with-redefs [parallel/env (fn [_] "key")
                  http/get (fn [_ _]
                             (delay {:status 200
                                     :body (json/write-value-as-string
                                            body)}))]
      (let [client (parallel/parallel)
            item (research/finish client run)]
        (is (response/completed item)
            "Response was not marked as completed")))))

(deftest the-parallel-finish-returns-markdown
  (let [rng (java.util.Random. 16015)
        run (str "trun_" (uuid rng))
        output (str "# " (token rng 6 1040 32))
        body {:run {:run_id run
                    :status "completed"}
              :output {:content output
                       :basis []}}]
    (with-redefs [parallel/env (fn [_] "key")
                  http/get (fn [_ _]
                             (delay {:status 200
                                     :body (json/write-value-as-string
                                            body)}))]
      (let [client (parallel/parallel)
            item (research/finish client run)]
        (is (= output (response/text item))
            "Markdown did not match API output")))))

(deftest the-parallel-stream-handles-empty-events
  (let [rng (java.util.Random. 16017)
        bytes (.getBytes "" "UTF-8")
        body (java.io.ByteArrayInputStream. bytes)
        client (with-redefs [parallel/env (fn [_] "key")
                             http/get (fn [_ _] {:status 200
                                                 :body body})]
                 (parallel/parallel))
        result (research/stream client (str "trun_" (uuid rng)))]
    (is result "Stream did not complete")))

(deftest the-parallel-clean-removes-periods
  (let [rng (java.util.Random. 16019)
        text (token rng 6 1040 32)
        value (str text "." text ".")
        clean (parallel/clean value)]
    (is (not (str/includes? clean "."))
        "Parallel clean did not remove periods")))
