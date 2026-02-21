(ns research.storage.repository
  (:refer-clojure :exclude [find load update])
  (:require [research.domain.session :as session]
            [research.storage.repository.read :as read]
            [research.storage.repository.write :as write])
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

(defrecord Repository [root]
  Loadable
  (load [_]
    (read/items root))
  Savable
  (save [_ items]
    (write/store root items))
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
  "Create repository from output path."
  [item]
  (->Repository item))
