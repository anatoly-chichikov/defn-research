(ns research.api.parallel
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [jsonista.core :as json]
            [org.httpkit.client :as http]
            [research.api.research :as research]
            [research.api.response :as response]))

(defn now
  "Return current time millis."
  []
  (System/currentTimeMillis))

(defn env
  "Return environment value by key."
  [key]
  (System/getenv key))

(defn clean
  "Remove periods from log text."
  [text]
  (str/replace text #"\." ""))

(defn emit
  "Emit progress event to stdout."
  [_ data]
  (let [kind (get data :type "")
        kind (or kind "")]
    (cond
      (= kind "task_run.state")
      (let [state (get-in data [:run :status] "unknown")
            line (clean (str "[STATUS] " state))]
        (println line))
      (= kind "task_run.progress_stats")
      (let [meter (or (get data :progress_meter 0) 0)
            ratio (int (/ meter 1))
            stats (get data :source_stats {})
            count (or (get stats :num_sources_read 0) 0)
            line (clean (str "[PROGRESS] " ratio "% | Sources: " count))]
        (println line))
      (str/starts-with? kind "task_run.progress_msg")
      (let [text (or (get data :message "") "")
            text (if (> (count text) 120) (str (subs text 0 120) " [cut]") text)
            label (str/upper-case (last (str/split kind #"\.")))
            line (clean (str "[" label "] " text))]
        (println line))
      (= kind "error")
      (let [text (or (get data :message "Unknown error") "")
            line (clean (str "[ERROR] " text))]
        (println line))
      :else true)))

(defn parse
  "Parse SSE data payload into map."
  [text]
  (try
    (json/read-value text (json/object-mapper {:decode-key-fn keyword}))
    (catch Exception _ {})))

(defn sse
  "Stream SSE events from reader."
  [reader]
  (loop [lines (line-seq reader) name "" data []]
    (if (seq lines)
      (let [line (first lines)
            rest (rest lines)]
        (cond
          (str/blank? line)
          (let [payload (str/join "\n" data)
                body (if (str/blank? payload) {} (parse payload))
                _ (if (seq body) (emit name body) true)]
            (recur rest "" []))
          (str/starts-with? line "event:")
          (let [name (str/trim (subs line 6))]
            (recur rest name data))
          (str/starts-with? line "data:")
          (let [data (conj data (str/trim (subs line 5)))]
            (recur rest name data))
          :else (recur rest name data)))
      true)))

(defrecord Parallel [key base time]
  research/Researchable
  (start [_ query processor]
    (let [url (str base "/v1/tasks/runs?beta=true")
          body {:input query
                :processor processor
                :enable_events true
                :task_spec {:output_schema {:type "text"}}}
          head {"x-api-key" key
                "Content-Type" "application/json"
                "parallel-beta" (str "search-extract-2025-10-10,"
                                     "events-sse-2025-07-24")}
          response @(http/post url {:headers head
                                    :body (json/write-value-as-string body)
                                    :timeout 60000
                                    :as :text})
          status (:status response)
          data (if (< status 300)
                 (json/read-value
                  (:body response)
                  (json/object-mapper {:decode-key-fn keyword}))
                 (throw (ex-info "Parallel create failed" {:status status})))
          run (or (:run_id data) (get-in data [:run :run_id]) "")]
      run))
  (stream [_ id]
    (let [url (str base "/v1beta/tasks/runs/" id "/events")
          head {"x-api-key" key
                "Accept" "text/event-stream"
                "parallel-beta" (str "search-extract-2025-10-10,"
                                     "events-sse-2025-07-24")}
          response @(http/get url {:headers head
                                   :as :stream
                                   :timeout 60000})
          body (:body response)]
      (if body (with-open [reader (io/reader body)] (sse reader)) true)))
  (finish [_ id]
    (let [url (str base "/v1/tasks/runs/" id "/result")
          head {"x-api-key" key
                "Content-Type" "application/json"}
          response @(http/get url {:headers head
                                   :query-params {:api_timeout 7200}
                                   :timeout 7200000
                                   :as :text})
          status (:status response)
          raw (if (= status 200)
                (json/read-value
                 (:body response)
                 (json/object-mapper {:decode-key-fn keyword}))
                (throw (ex-info "Parallel result failed" {:status status})))
          output (:output raw)
          text (if (map? output) (or (:content output) "") "")
          basis (if (map? output) (or (:basis output) []) [])
          run (or (:run raw) {})
          code (if (map? run) (or (:run_id run) id) id)
          state (if (map? run) (or (:status run) "completed") "completed")]
      (response/response {:id code
                          :status state
                          :output text
                          :basis basis
                          :raw raw}))))

(defn parallel
  "Create Parallel client from env."
  []
  (let [key (or (env "PARALLEL_API_KEY") "")
        base (or (env "PARALLEL_BASE_URL") "https://api.parallel.ai")]
    (if (str/blank? key)
      (throw (ex-info "PARALLEL_API_KEY is required" {}))
      (->Parallel key base (now)))))
