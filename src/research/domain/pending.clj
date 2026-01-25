(ns research.domain.pending
  (:require [clojure.string :as str]))

(defprotocol Pendinged
  "Object with pending run details."
  (id [item] "Return run identifier.")
  (query [item] "Return research query.")
  (processor [item] "Return processor name.")
  (language [item] "Return research language.")
  (provider [item] "Return provider name.")
  (data [item] "Return map representation."))

(defrecord PendingRun [id brief data]
  Pendinged
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
  (processor [_] (:processor data))
  (language [_] (:language data))
  (provider [_] (:provider data))
  (data [_] {:run_id id
             :processor (:processor data)
             :language (:language data)
             :provider (:provider data)
             :brief brief}))

(defn pending
  "Create pending run from map."
  [item]
  (let [entry (:brief item)
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
               :items items}]
    (->PendingRun
     (:run_id item)
     brief
     {:processor (:processor item)
      :language (:language item)
      :provider (or (:provider item) "parallel")})))
