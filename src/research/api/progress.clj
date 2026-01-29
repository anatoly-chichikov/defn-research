(ns research.api.progress
  (:require [clojure.string :as str]))

(defprotocol Progressed
  "Object that can emit progress output."
  (emit [item text] "Emit progress text.")
  (clean [item text] "Return cleaned progress text."))

(defrecord Progress [data]
  Progressed
  (emit [item text]
    (let [text (clean item text)]
      (println text)
      true))
  (clean [_ text]
    (let [text (str text)
          pat (:dot data)
          note (str/replace text pat "")]
      note)))

(defn make
  "Return default progress emitter."
  []
  (->Progress {:dot #"\."}))
