(ns research.storage.file
  (:refer-clojure :exclude [read])
  (:require [jsonista.core :as json])
  (:import (java.nio.file Files)
           (java.nio.file.attribute FileAttribute)))

(defprotocol Reader
  "Object that can read data."
  (read [item] "Return file content as map."))

(defprotocol Writer
  "Object that can write data."
  (write [item value] "Persist map data."))

(defprotocol Existing
  "Object that can check file existence."
  (exists [item] "Return true when file exists."))

(defrecord JsonFile [path]
  Reader
  (read [_]
    (let [name (.toFile path)]
      (if (.exists name)
        (json/read-value name (json/object-mapper {:decode-key-fn keyword}))
        (throw (ex-info "File not found" {:path (.toString path)})))))
  Writer
  (write [_ value]
    (let [root (.getParent path)
          file (.toFile path)
          _ (when root
              (Files/createDirectories root (make-array FileAttribute 0)))]
      (json/write-value file value)
      path))
  Existing
  (exists [_] (Files/exists path (make-array java.nio.file.LinkOption 0)))
  Object
  (toString [_] (.toString path)))

(defn file
  "Create JsonFile from path."
  [path]
  (->JsonFile path))
