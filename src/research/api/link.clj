(ns research.api.link
  (:require [clojure.string :as str]))

(defprotocol Linkable
  "Object that can normalize links."
  (clean [item text] "Return URL without tracking params.")
  (strip [item text] "Return text with tracking params removed.")
  (domain [item text] "Return domain from URL string.")
  (links [item text] "Return URLs from text."))

(defrecord Links [data]
  Linkable
  (clean [_ text]
    (try
      (let [text (str text)
            part (java.net.URI. text)
            query (.getQuery part)
            keep (if (and query (not (str/blank? query)))
                   (let [pairs (map #(str/split % #"=") (str/split query #"&"))
                         items (filter (fn [item]
                                         (not (str/starts-with?
                                               (str/lower-case (first item))
                                               "utm_")))
                                       pairs)
                         line (str/join "&" (map #(str/join "=" %) items))]
                     (if (= line query)
                       text
                       (str (.getScheme part)
                            "://"
                            (.getAuthority part)
                            (.getPath part)
                            (if (str/blank? line) "" (str "?" line))
                            (if (.getFragment part)
                              (str "#" (.getFragment part))
                              ""))))
                   text)]
        keep)
      (catch Exception _ (str text))))
  (strip [item text]
    (let [text (str text)
          pat (:link data)
          items (or (re-seq pat text) [])
          note (reduce (fn [note url]
                         (str/replace note url (clean item url)))
                       text
                       items)
          utm (:utm data)
          line (str/replace note utm "")]
      line))
  (domain [_ text]
    (try
      (let [part (java.net.URI. (str text))
            host (.getHost part)]
        (if host (str/replace host "www." "") ""))
      (catch Exception _ "")))
  (links [_ text]
    (let [text (str text)
          pat (:link data)
          items (or (re-seq pat text) [])]
      (vec items))))

(defn make
  "Return default link policy."
  []
  (->Links {:link #"https?://[^\s\)\]]+"
            :utm #"(\?utm_[^\s\)\]]+|&utm_[^\s\)\]]+)"}))
