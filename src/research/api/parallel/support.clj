(ns research.api.parallel.support
  (:require [clojure.string :as str]
            [jsonista.core :as json]
            [research.api.progress :as progress]))

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
  (progress/clean (progress/make) text))

(defn emit
  "Emit progress event to stdout."
  [log data]
  (let [kind (get data :type "")
        kind (or kind "")]
    (cond
      (= kind "task_run.state")
      (let [state (get-in data [:run :status] "unknown")
            line (str "[STATUS] " state)]
        (progress/emit log line))
      (= kind "task_run.progress_stats")
      (let [meter (or (get data :progress_meter 0) 0)
            ratio (int (/ meter 1))
            stats (get data :source_stats {})
            count (or (get stats :num_sources_read 0) 0)
            line (str "[PROGRESS] " ratio "% | Sources: " count)]
        (progress/emit log line))
      (str/starts-with? kind "task_run.progress_msg")
      (let [text (or (get data :message "") "")
            text (if (> (count text) 120)
                   (str (subs text 0 120) " [cut]")
                   text)
            label (str/upper-case (last (str/split kind #"\.")))
            line (str "[" label "] " text)]
        (progress/emit log line))
      (= kind "error")
      (let [text (or (get data :message "Unknown error") "")
            line (str "[ERROR] " text)]
        (progress/emit log line))
      :else true)))

(defn parse
  "Parse SSE data payload into map."
  [text]
  (try
    (json/read-value text (json/object-mapper {:decode-key-fn keyword}))
    (catch Exception _ {})))

(defn sse
  "Stream SSE events from reader."
  [reader log]
  (loop [lines (line-seq reader) name "" data []]
    (if (seq lines)
      (let [line (first lines)
            rest (rest lines)]
        (cond
          (str/blank? line)
          (let [payload (str/join "\n" data)
                body (if (str/blank? payload) {} (parse payload))
                _ (if (seq body) (emit log body) true)]
            (recur rest "" []))
          (str/starts-with? line "event:")
          (let [name (str/trim (subs line 6))]
            (recur rest name data))
          (str/starts-with? line "data:")
          (let [data (conj data (str/trim (subs line 5)))]
            (recur rest name data))
          :else (recur rest name data)))
      true)))
