(ns research.main.print
  (:refer-clojure :exclude [list])
  (:require [clojure.string :as str]
            [research.domain.session :as session]
            [research.domain.task :as task]
            [research.pdf.document :as document]
            [research.pdf.document.data :as docdata]
            [research.pdf.palette :as palette]
            [research.storage.organizer :as organizer]
            [research.storage.repository :as repo]))

(defn enumerate
  "List sessions."
  [data]
  (let [repo (repo/repo data)
        list (repo/load repo)]
    (if (seq list)
      (doseq [item list]
        (let [count (count (session/tasks item))
              head (str "[" (subs (session/id item) 0 8) "] "
                        (session/topic item))
              form java.time.format.DateTimeFormatter/ISO_LOCAL_DATE_TIME
              stamp (.format (session/created item) form)]
          (println head)
          (println (str "  Created: " stamp))
          (println (str "  Tasks: " count))
          (println "")))
      (println "No research sessions found"))))

(defn display
  "Show session details."
  [data id]
  (let [repo (repo/repo data)
        list (repo/load repo)
        pick (first (filter #(str/starts-with? (session/id %) id) list))]
    (if pick
      (do (println (str "Topic: " (session/topic pick)))
          (println (str "ID: " (session/id pick)))
          (let [form java.time.format.DateTimeFormatter/ISO_LOCAL_DATE_TIME
                stamp (.format (session/created pick) form)]
            (println (str "Created: " stamp)))
          (println (str "\nTasks (" (count (session/tasks pick)) "):"))
          (doseq [task (session/tasks pick)]
            (println (str "\n  [" (task/status task) "] " (task/query task)))
            (let [info {:root data
                        :session pick}
                  [text sources] (docdata/resultmap info task)
                  part (subs text 0 (min 100 (count text)))]
              (when (seq text)
                (println (str "  Summary: " part " [truncated]")))
              (when (seq sources)
                (println (str "  Sources: " (count sources)))))))
      (println (str "Session not found: " id)))))

(defn render
  "Generate report for session."
  [data out id html]
  (let [repo (repo/repo data)
        list (repo/load repo)
        pick (first (filter #(str/starts-with? (session/id %) id) list))]
    (if pick
      (let [provider (if (seq (session/tasks pick))
                       (let [last (last (session/tasks pick))
                             name (task/provider last)]
                         (if (str/ends-with? name ".ai")
                           (first (str/split name #"\."))
                           name))
                       "parallel")
            org (organizer/organizer out)
            name (organizer/name
                  org
                  (session/created pick)
                  (session/topic pick)
                  (session/id pick))
            cover (organizer/existing org name provider)
            doc (document/document pick (palette/palette) cover out)
            path (if html
                   (organizer/html org name provider)
                   (organizer/report org name provider))]
        (if html (document/page doc path) (document/save doc path))
        (let [note (if html "HTML saved: " "PDF saved: ")]
          (println (str note (.toString path)))))
      (println (str "Session not found: " id)))))
