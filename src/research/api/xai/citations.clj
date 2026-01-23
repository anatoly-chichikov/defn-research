(ns research.api.xai.citations
  (:require [libpython-clj2.python :as py]
            [research.api.link :as link]))

(defprotocol Cited
  "Object that can map citations."
  (batch [item core data] "Return python items as JVM vector.")
  (index [item text point] "Return UTF-16 offset for codepoint index.")
  (mark [item data] "Return citation data.")
  (links [item items] "Return unique URLs from text list."))

(defrecord Cites [data]
  Cited
  (batch [_ core data]
    (if (nil? data)
      []
      (try
        (vec (py/as-jvm (py/call-attr core "list" data)))
        (catch Exception _
          (try
            (let [data (py/as-jvm data)]
              (if (sequential? data) (vec data) []))
            (catch Exception __ []))))))
  (index [_ text point]
    (let [text (str text)
          size (count text)
          limit (.codePointCount text 0 size)
          point (int (or point 0))
          point (if (neg? point) 0 point)
          point (if (> point limit) limit point)]
      (.offsetByCodePoints text 0 point)))
  (mark [_ data]
    (let [site (py/get-attr data "web_citation")
          view (py/as-jvm site)
          page (if view (or (py/as-jvm (py/get-attr site "url")) "") "")
          link (if (seq page) nil (py/get-attr data "x_citation"))
          node (if link (py/as-jvm link) nil)
          post (if node (or (py/as-jvm (py/get-attr link "url")) "") "")
          url (if (seq page) page post)
          end (or (py/as-jvm (py/get-attr data "end_index")) 0)
          id (or (py/as-jvm (py/get-attr data "id")) "")]
      {:end end
       :id id
       :url url}))
  (links [_ items]
    (let [policy (:link data)
          init {:seen #{}
                :list []}
          data (reduce
                (fn [data item]
                  (let [urls (link/links policy (str item))]
                    (reduce
                     (fn [data url]
                       (let [seen (:seen data)
                             list (:list data)
                             flag (contains? seen url)]
                         (if flag
                           data
                           {:seen (conj seen url)
                            :list (conj list url)})))
                     data
                     urls)))
                init
                (or items []))
          list (:list data)]
      list)))

(defn make
  "Return citation helper."
  ([] (make {}))
  ([data]
   (let [policy (or (:link data) (link/make))]
     (->Cites {:link policy}))))
