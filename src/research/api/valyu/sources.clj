(ns research.api.valyu.sources
  (:require [clojure.string :as str]
            [research.api.link :as link]))

(defprotocol Sourced
  "Object that can score sources."
  (level [item info] "Return confidence level from source.")
  (trusted [item text] "Return true when domain is trusted."))

(defrecord Sources [data]
  Sourced
  (trusted [_ text]
    (let [list (:trust data)
          tail (str text)]
      (cond
        (some #(or (= tail %) (str/ends-with? tail (str "." %))) list) true
        (or (str/ends-with? tail ".gov") (str/includes? tail ".gov.")) true
        (or (str/ends-with? tail ".edu") (str/includes? tail ".edu.")) true
        (or (str/ends-with? tail ".ac") (str/includes? tail ".ac.")) true
        (or (str/ends-with? tail ".mil") (str/includes? tail ".mil.")) true
        (or (str/ends-with? tail ".int") (str/includes? tail ".int.")) true
        :else false)))
  (level [item info]
    (let [kind (or (:source info) "")
          form (or (:source_type info) (:category info) "")
          count (or (:citation_count info) 0)
          names (or (:authors info) [])
          code (or (:doi info) "")
          date (or (:publication_date info) "")
          score (:relevance_score info)
          url (or (:url info) "")
          policy (or (:link data) (link/make))
          host (if (str/blank? url) "" (link/domain policy url))
          detail (or (pos? count)
                     (seq names)
                     (not (str/blank? code))
                     (not (str/blank? date)))
          note (atom "Unknown")
          _ (when (or (contains? (:science data) kind) (= form "paper"))
              (reset! note "Medium"))
          _ (when (and (or (contains? (:science data) kind) (= form "paper"))
                       (>= count 10))
              (reset! note "High"))
          _ (when (and (= form "paper") (seq names) (not (str/blank? code)))
              (reset! note "High"))
          _ (when (and (= form "paper") (str/blank? code) (not= @note "High"))
              (reset! note "Medium"))
          _ (when (and (= kind "web")
                       (seq names)
                       (not (str/blank? date))
                       (not= @note "High"))
              (reset! note "Medium"))
          _ (when (and (contains? (:finance data) kind) (not= @note "High"))
              (reset! note "Medium"))
          _ (when (and (trusted item host) (= @note "Unknown"))
              (reset! note "Medium"))
          _ (when (and (some? score) (< score 0.5) detail (= @note "Unknown"))
              (reset! note "Low"))]
      @note)))

(defn make
  "Return source scorer."
  [data]
  (->Sources data))
