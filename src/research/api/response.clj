(ns research.api.response
  (:require [clojure.string :as str]
            [research.domain.result :as result]))

(defprotocol Responded
  "Object representing API response."
  (id [item] "Return run identifier.")
  (cost [item] "Return total cost.")
  (raw [item] "Return raw response map.")
  (completed [item] "Return true when completed.")
  (failed [item] "Return true when failed.")
  (text [item] "Return output markdown.")
  (sources [item] "Return source list."))

(defn purge
  "Remove utm fragments from text."
  [text]
  (str/replace text #"(\?utm_[^\s\)\]]+|&utm_[^\s\)\]]+)" ""))

(defn clean
  "Remove utm params from URL."
  [text]
  (try
    (let [part (java.net.URI. text)
          query (.getQuery part)
          value (if (and query (not (str/blank? query)))
                  (let [pairs (map #(str/split % #"=") (str/split query #"&"))
                        items (filter
                               (fn [item]
                                 (not
                                  (str/starts-with?
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
      value)
    (catch Exception _ text)))

(defn strip
  "Strip tracking URLs from output text."
  [text]
  (let [pattern #"https?://[^\s\)\]]+"
        items (or (re-seq pattern text) [])
        value (reduce
               (fn [note link]
                 (str/replace note link (clean link)))
               text
               items)]
    (purge value)))

(defn domain
  "Extract domain from URL string."
  [text]
  (try (let [part (java.net.URI. text)
             host (.getHost part)]
         (if host (str/replace host "www." "") ""))
       (catch Exception _ "")))

(defrecord Response [id status text data]
  Responded
  (id [_] id)
  (cost [_] (:cost data))
  (raw [_] (:raw data))
  (completed [_] (= status "completed"))
  (failed [_] (= status "failed"))
  (text [_] text)
  (sources [_] (let [seen (atom #{})]
                 (reduce
                  (fn [list field]
                    (let [items (get field :citations [])
                          level (or (get field :confidence "") "")]
                      (reduce
                       (fn [list cite]
                         (let [link (or (:url cite) "")
                               link (if (str/blank? link) "" (clean link))
                               add (and (not (str/blank? link))
                                        (not (contains? @seen link)))]
                           (if add
                             (let [ex (get cite :excerpts [])
                                   note (if (seq ex) (first ex) "")
                                   head (or (:title cite) (domain link))
                                   item (result/->Source
                                         head
                                         link
                                         note
                                         level)]
                               (swap! seen conj link)
                               (conj list item))
                             list)))
                       list
                       items)))
                  []
                  (or (:basis data) [])))))

(defn response
  "Create response from map."
  [item]
  (let [text (strip (or (:output item) ""))
        base {:basis (or (:basis item) [])
              :cost (or (:cost item) 0.0)
              :raw (or (:raw item) {})}]
    (->Response (:id item) (:status item) text base)))
