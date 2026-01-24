(ns research.domain.result
  (:require [clojure.string :as str]))

(defprotocol Sourced
  "Object with URL source."
  (title [item] "Return source title.")
  (url [item] "Return source URL.")
  (excerpt [item] "Return relevant excerpt.")
  (confidence [item] "Return confidence level when available."))

(defprotocol Summarized
  "Object with text summary."
  (summary [item] "Return text summary."))

(defprotocol Listed
  "Object with sources list."
  (sources [item] "Return sources list."))

(defprotocol Serialized
  "Object that can serialize to map."
  (data [item] "Return map representation."))

(defprotocol Presence
  "Object with presence signal."
  (presence [item] "Return true when value is present."))

(defrecord CitationSource [title url excerpt confidence]
  Sourced
  (title [_] title)
  (url [_] url)
  (excerpt [_] excerpt)
  (confidence [_] confidence)
  Serialized
  (data [_] (if (clojure.string/blank? confidence)
              {:title title
               :url url
               :excerpt excerpt}
              {:title title
               :url url
               :excerpt excerpt
               :confidence confidence})))

(defn source
  "Create source from map."
  [item]
  (->CitationSource
   (:title item)
   (:url item)
   (:excerpt item)
   (or (:confidence item) "")))

(defn purge
  "Remove sources section from summary text."
  [text]
  (let [pattern #"(^|\n)#{1,6}\s*Sources?\s*\n.*?(?=\n#{1,6}\s|\Z)"
        value (str/replace text pattern "")]
    value))

(defrecord ResearchReport [summary sources]
  Summarized
  (summary [_] (purge summary))
  Listed
  (sources [_] sources)
  Serialized
  (data [_] {:summary summary
             :sources (mapv data sources)})
  Presence
  (presence [_] true)
  Object
  (toString [_] summary))

(defrecord EmptyReport [summary sources]
  Summarized
  (summary [_] summary)
  Listed
  (sources [_] sources)
  Serialized
  (data [_] {:summary summary
             :sources sources})
  Presence
  (presence [_] false))

(defn result
  "Create result from map."
  [item]
  (if item
    (let [raw (:summary item)
          text (if (map? raw) (:content raw "") raw)
          list (mapv source (:sources item))]
      (->ResearchReport text list))
    (->EmptyReport "" [])))
