(ns research.storage.repository
  (:refer-clojure :exclude [find load update])
  (:require [clojure.edn :as edn]
            [clojure.pprint :as pprint]
            [clojure.string :as str]
            [research.domain.session :as session]
            [research.storage.organizer :as organizer])
  (:import (java.nio.file Files LinkOption)
           (java.nio.file.attribute FileAttribute)
           (java.util Optional)))

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
    (let [opts (make-array LinkOption 0)
          list (if (Files/exists root opts)
                 (with-open [stream (Files/newDirectoryStream root)]
                   (reduce
                    (fn [list path]
                      (let [file (.resolve path "session.edn")
                            dir (Files/isDirectory path opts)
                            hit (Files/exists file opts)
                            name (if dir (str (.getFileName path)) "")
                            rule (re-pattern
                                  (str "(\\d{4}-\\d{2}-\\d{2})"
                                       "_(.+)_([A-Za-z0-9]{8})"))
                            mark (if dir (re-matches rule name) nil)]
                        (cond
                          (and dir hit)
                          (let [text (slurp (.toFile file) :encoding "UTF-8")
                                data (edn/read-string text)
                                tasks (:tasks data)
                                hold (:pending data)
                                flag (or (some #(contains? % :query) tasks)
                                         (and hold (contains? hold :query)))
                                items (mapv
                                       (fn [item]
                                         (let [service (or (:service item)
                                                           "provider")
                                               size (count service)
                                               cut (- size 3)
                                               name (if (str/ends-with?
                                                         service
                                                         ".ai")
                                                      (subs service 0 cut)
                                                      service)
                                               tag (organizer/slug name)
                                               tag (if (str/blank? tag)
                                                     "provider"
                                                     tag)
                                               input (.resolve
                                                      path
                                                      (str "input-" tag ".md"))
                                               brief (.resolve
                                                      path
                                                      (str "brief-" tag ".md"))
                                               text (cond
                                                      (Files/exists input opts)
                                                      (slurp
                                                       (.toFile input)
                                                       :encoding "UTF-8")
                                                      (Files/exists brief opts)
                                                      (slurp
                                                       (.toFile brief)
                                                       :encoding "UTF-8")
                                                      :else "")]
                                           (assoc item :query text)))
                                       tasks)
                                provider (if hold
                                           (or (:provider hold) "provider")
                                           "provider")
                                tag (organizer/slug provider)
                                tag (if (str/blank? tag) "provider" tag)
                                input (.resolve path (str "input-" tag ".md"))
                                brief (.resolve path (str "brief-" tag ".md"))
                                text (cond
                                       (Files/exists input opts)
                                       (slurp (.toFile input) :encoding "UTF-8")
                                       (Files/exists brief opts)
                                       (slurp (.toFile brief) :encoding "UTF-8")
                                       :else "")
                                hold (if hold (assoc hold :query text) hold)
                                data (assoc data :tasks items :pending hold)
                                item (session/session data)
                                note (if flag
                                       (with-out-str
                                         (pprint/pprint (session/data item)))
                                       "")
                                _ (when flag
                                    (spit (.toFile file)
                                          note
                                          :encoding "UTF-8"))]
                            (conj list item))
                          mark
                          (let [date (nth mark 1)
                                slug (nth mark 2)
                                code (nth mark 3)
                                time (str date "T00:00:00")
                                id (str code "-migrated")
                                names
                                (with-open [files
                                            (Files/newDirectoryStream path)]
                                  (let [list (reduce
                                              (fn [list file]
                                                (let [name
                                                      (str
                                                       (.getFileName file))
                                                      head
                                                      (str/starts-with?
                                                       name
                                                       "response-")
                                                      tail
                                                      (str/ends-with?
                                                       name
                                                       ".json")
                                                      mark
                                                      (and head tail)]
                                                  (if mark
                                                    (conj list name)
                                                    list)))
                                              []
                                              files)]
                                    (vec (sort list))))
                                tasks (map-indexed
                                       (fn [index name]
                                         (let [size (count name)
                                               tag (subs name 9 (- size 5))
                                               input (.resolve
                                                      path
                                                      (str "input-"
                                                           tag
                                                           ".md"))
                                               brief (.resolve
                                                      path
                                                      (str "brief-"
                                                           tag
                                                           ".md"))
                                               text (cond
                                                      (Files/exists input opts)
                                                      (slurp
                                                       (.toFile input)
                                                       :encoding "UTF-8")
                                                      (Files/exists brief opts)
                                                      (slurp
                                                       (.toFile brief)
                                                       :encoding "UTF-8")
                                                      :else "")
                                               item (str code "-" tag "-")
                                               item (str item index)]
                                           {:id item
                                            :query text
                                            :status "completed"
                                            :service (str tag ".ai")
                                            :created time}))
                                       names)
                                data {:id id
                                      :topic slug
                                      :tasks tasks
                                      :created time}
                                item (session/session data)
                                text (with-out-str
                                       (pprint/pprint (session/data item)))]
                            (spit (.toFile file) text :encoding "UTF-8")
                            (conj list item))
                          :else list)))
                    []
                    stream))
                 [])]
      (vec (sort-by session/created list))))
  Savable
  (save [_ items]
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
