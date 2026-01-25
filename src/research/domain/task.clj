(ns research.domain.task
  (:refer-clojure :exclude [format])
  (:require [clojure.string :as str]
            [research.domain.result :as result])
  (:import (java.time LocalDateTime)
           (java.time.format DateTimeFormatter)
           (java.util Optional UUID)))

(defprotocol Tasked
  "Object representing a research task."
  (id [item] "Return task identifier.")
  (query [item] "Return research query.")
  (status [item] "Return task status.")
  (report [item] "Return task result object.")
  (language [item] "Return task language.")
  (provider [item] "Return task provider.")
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

(defrecord ResearchRun [id brief data result]
  Tasked
  (id [_] id)
  (query [_]
    (let [text (str (or (:text brief) ""))
          topic (str (or (:topic brief) ""))
          items (or (:items brief) [])
          items (vec (remove str/blank? items))
          rows (map-indexed (fn [idx item] (str (inc idx) ". " item)) items)
          tail (str/join "\n" rows)
          note (cond
                 (not (str/blank? text)) text
                 (and (not (str/blank? topic)) (seq rows))
                 (str topic "\n\nResearch:\n" tail)
                 (seq rows) (str "Research:\n" tail)
                 :else topic)]
      note))
  (status [_] (:status data))
  (report [_] result)
  (language [_] (:language data))
  (provider [_] (:service data))
  (created [_] (:created data))
  (completed [_] (:completed data))
  (finish [_ value]
    (->ResearchRun
     id
     brief
     (assoc data :status "completed" :completed (Optional/of (now)))
     value))
  (data [_] (let [base {:id id
                        :status (:status data)
                        :language (:language data)
                        :service (:service data)
                        :processor (:processor data)
                        :brief brief
                        :created (format (:created data))}
                  done (:completed data)
                  ready (if (.isPresent done)
                          (assoc base :completed (format (.get done)))
                          base)
                  proc (:processor data)
                  ready (if (str/blank? (str proc))
                          (dissoc ready :processor)
                          ready)]
              ready)))

(defn task
  "Create task from map."
  [item]
  (let [text (or (:language item) "русский")
        name (or (:service item) "parallel.ai")
        time (parse (:created item))
        done (if (:completed item)
               (Optional/of (parse (:completed item)))
               (Optional/empty))
        entry (:brief item)
        query (or (:text entry) (:query item) "")
        rows (str/split-lines (str query))
        label "Research:"
        spot (first (keep-indexed
                     (fn [idx line]
                       (when (= label (str/trim line)) idx))
                     rows))
        edge (first (keep-indexed
                     (fn [idx line]
                       (let [line (str/trim line)
                             hit (re-find
                                  #"^(?:\d+\s*[\.)]|\d+|[*+-])\s+"
                                  line)]
                         (when hit idx)))
                     rows))
        cut (if (some? spot) spot edge)
        head (vec (if (some? cut) (take cut rows) rows))
        tail (if (some? cut)
               (drop (if (some? spot) (inc spot) cut) rows)
               [])
        list (loop [list [] chunk tail]
               (if (seq chunk)
                 (let [line (str/trim (first chunk))]
                   (if (str/blank? line)
                     (recur list (rest chunk))
                     (let [num (re-find #"^\d+\s*[\.)]\s+(.+)$" line)
                           raw (re-find #"^\d+\s+(.+)$" line)
                           bul (re-find #"^[*+-]\s+(.+)$" line)
                           part (cond
                                  num (second num)
                                  raw (second raw)
                                  bul (second bul)
                                  :else nil)
                           part (if (and part (not (str/blank? part)))
                                  (str/trim part)
                                  nil)
                           list (if part
                                  (conj list part)
                                  (if (seq list)
                                    (conj (vec (butlast list))
                                          (str (last list) " " line))
                                    (conj list line)))]
                       (recur list (rest chunk)))))
                 list))
        list (vec (remove str/blank? (map str/trim list)))
        top (reduce
             (fn [text line]
               (if (str/blank? (str/trim line)) text (str/trim line)))
             ""
             head)
        topic (or (:topic entry) top "")
        items (vec (or (:items entry) list []))
        brief {:text query
               :topic topic
               :items items}
        data {:status (:status item)
              :language text
              :service name
              :processor (:processor item)
              :created time
              :completed done}
        raw (:result item)
        value (result/result raw)
        code (or (:id item) (str (UUID/randomUUID)))]
    (->ResearchRun code brief data value)))
