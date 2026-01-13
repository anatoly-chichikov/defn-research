(ns research.domain.task
  (:refer-clojure :exclude [format])
  (:require [research.domain.result :as result])
  (:import (java.time LocalDateTime)
           (java.time.format DateTimeFormatter)
           (java.util Optional UUID)))

(defprotocol Tasked
  "Object representing a research task."
  (id [item] "Return task identifier.")
  (query [item] "Return research query.")
  (status [item] "Return task status.")
  (result [item] "Return task result object.")
  (language [item] "Return task language.")
  (service [item] "Return task service.")
  (created [item] "Return creation time.")
  (completed [item] "Return completion time.")
  (finish [item value] "Return task marked as completed.")
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

(defrecord Task [id query data result]
  Tasked
  (id [_] id)
  (query [_] query)
  (status [_] (:status data))
  (result [_] result)
  (language [_] (:language data))
  (service [_] (:service data))
  (created [_] (:created data))
  (completed [_] (:completed data))
  (finish [_ value]
    (->Task
     id
     query
     (assoc data :status "completed" :completed (Optional/of (now)))
     value))
  (data [_] (let [base {:id id
                        :status (:status data)
                        :language (:language data)
                        :service (:service data)
                        :created (format (:created data))}
                  done (:completed data)
                  ready (if (.isPresent done)
                          (assoc base :completed (format (.get done)))
                          base)
                  pack (if (result/present result)
                         (assoc ready :result (result/data result))
                         ready)]
              pack)))

(defn task
  "Create task from map."
  [item]
  (let [text (or (:language item) "русский")
        name (or (:service item) "parallel.ai")
        time (parse (:created item))
        done (if (:completed item)
               (Optional/of (parse (:completed item)))
               (Optional/empty))
        query (or (:query item) "")
        data {:status (:status item)
              :language text
              :service name
              :created time
              :completed done}
        raw (:result item)
        value (result/result raw)
        code (or (:id item) (str (UUID/randomUUID)))]
    (->Task code query data value)))
