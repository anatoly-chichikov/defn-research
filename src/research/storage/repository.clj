(ns research.storage.repository
  (:refer-clojure :exclude [find load update])
  (:require [research.domain.session :as session]
            [research.storage.file :as file])
  (:import (java.util Optional)))

(defprotocol Loadable
  "Object that can load sessions."
  (load [item] "Return session list."))

(defprotocol Savable
  "Object that can save sessions."
  (save [item list] "Persist session list."))

(defprotocol Mutable
  "Object that can update sessions."
  (append [item value] "Append session.")
  (find [item value] "Find session by id.")
  (update [item value] "Update session by id."))

(defrecord Repository [file]
  Loadable
  (load [_]
    (let [data (try (file/read file) (catch Exception _ {:sessions []}))
          items (mapv session/session (:sessions data))]
      items))
  Savable
  (save [_ items] (file/write file {:version "1.0.0"
                                    :sessions (mapv session/data items)}))
  Mutable
  (append [item value]
    (let [items (load item)
          store (conj items value)]
      (save item store)))
  (find [item value]
    (let [items (load item)
          pick (first (filter #(= (session/id %) value) items))]
      (if pick (Optional/of pick) (Optional/empty))))
  (update [item value]
    (let [items (load item)
          store (mapv (fn [item]
                        (if (= (session/id item) (session/id value))
                          value
                          item))
                      items)]
      (save item store))))

(defn repo
  "Create repository from JsonFile."
  [item]
  (->Repository item))
