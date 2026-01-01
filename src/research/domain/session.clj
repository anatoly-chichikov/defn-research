(ns research.domain.session
  (:refer-clojure :exclude [extend format])
  (:require [research.domain.pending :as pending]
            [research.domain.task :as task])
  (:import (java.time LocalDateTime)
           (java.time.format DateTimeFormatter)
           (java.util Optional UUID)))

(defprotocol Sessioned
  "Object representing research session."
  (id [item] "Return session identifier.")
  (topic [item] "Return session topic.")
  (tasks [item] "Return task list.")
  (created [item] "Return creation time.")
  (pending [item] "Return pending run.")
  (extend [item value] "Return new session with appended task.")
  (start [item value] "Return session with pending run.")
  (reset [item] "Return session without pending run.")
  (data [item] "Return map representation."))

(defn now
  "Return current local datetime."
  []
  (LocalDateTime/now))

(defn parse
  "Parse ISO datetime string into LocalDateTime."
  [text]
  (LocalDateTime/parse text))

(defn format
  "Format LocalDateTime into ISO string."
  [time]
  (.format time DateTimeFormatter/ISO_LOCAL_DATE_TIME))

(defrecord Session [id topic tasks data]
  Sessioned
  (id [_] id)
  (topic [_] topic)
  (tasks [_] tasks)
  (created [_] (:created data))
  (pending [_] (:pending data))
  (extend [_ value]
    (->Session
     id
     topic
     (conj tasks value)
     (assoc data :pending (Optional/empty))))
  (start [_ value]
    (->Session id topic tasks (assoc data :pending (Optional/of value))))
  (reset [_]
    (->Session id topic tasks (assoc data :pending (Optional/empty))))
  (data [_] (let [base {:id id
                        :topic topic
                        :tasks (mapv task/data tasks)
                        :created (format (:created data))}
                  hold (:pending data)
                  pack (if (.isPresent hold)
                         (assoc base :pending (pending/data (.get hold)))
                         base)]
              pack)))

(defn session
  "Create session from map."
  [item]
  (let [list (mapv task/task (:tasks item))
        time (parse (:created item))
        hold (if (:pending item)
               (Optional/of (pending/pending (:pending item)))
               (Optional/empty))
        data {:created time
              :pending hold}
        code (or (:id item) (str (UUID/randomUUID)))]
    (->Session code (:topic item) list data)))
