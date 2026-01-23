(ns research.api.xai.cache
  (:refer-clojure :exclude [load])
  (:require [research.storage.file :as file])
  (:import (java.nio.file Files)
           (java.nio.file.attribute FileAttribute)))

(defprotocol Stored
  "Object that can store XAI cache."
  (path [item id] "Return cache file path.")
  (load [item id] "Load cached payload.")
  (save [item id data] "Save cached payload."))

(defrecord Cache [root]
  Stored
  (path [_ id]
    (let [base (.resolve root "tmp_cache")
          dir (.resolve base "xai")
          node (.resolve dir (str id ".json"))]
      node))
  (load [item id]
    (let [path (path item id)
          node (file/file path)]
      (file/read node)))
  (save [item id data]
    (let [path (path item id)
          dir (.getParent path)
          _ (Files/createDirectories dir (make-array FileAttribute 0))
          node (file/file path)
          _ (file/write node data)]
      path)))

(defn make
  "Return cache store."
  [root]
  (->Cache root))
