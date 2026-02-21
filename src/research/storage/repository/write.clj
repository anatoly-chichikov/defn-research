(ns research.storage.repository.write
  (:require [clojure.pprint :as pprint]
            [research.domain.session :as session]
            [research.storage.organizer :as organizer])
  (:import (java.nio.file Files)
           (java.nio.file.attribute FileAttribute)))

(defn store
  "Persist session list into output folder."
  [root items]
  (let [org (organizer/organizer root)]
    (doseq [item items]
      (let [name (organizer/name
                  org
                  (session/created item)
                  (session/topic item)
                  (session/id item))
            base (.resolve root name)
            _ (Files/createDirectories base (make-array FileAttribute 0))
            path (.resolve base "session.edn")
            text (with-out-str (pprint/pprint (session/data item)))]
        (spit (.toFile path) text :encoding "UTF-8")))))
