(ns research.pdf.document.data
  (:require [clojure.string :as str]
            [jsonista.core :as json]
            [research.api.research :as research]
            [research.api.response :as response]
            [research.api.valyu :as valyu]
            [research.domain.result :as result]
            [research.domain.session :as sess]
            [research.domain.task :as task]
            [research.pdf.document.env :as env]
            [research.storage.organizer :as organizer])
  (:import (java.nio.file Files LinkOption)))

(defn provider
  "Return provider slug from task service."
  [item]
  (env/provider item))

(defn raw
  "Load raw response from output folder."
  [item task]
  (let [root (:root item)
        org (organizer/organizer root)
        name (organizer/name
              org
              (sess/created (:session item))
              (sess/topic (:session item))
              (sess/id (:session item)))
        base (.resolve root name)
        path (if task
               (let [tag (organizer/slug (provider task))
                     tag (if (str/blank? tag) "provider" tag)]
                 (.resolve base (str "response-" tag ".json")))
               (.resolve root "missing.json"))]
    (if (and task (Files/exists path (make-array LinkOption 0)))
      (json/read-value
       (.toFile path)
       (json/object-mapper {:decode-key-fn keyword}))
      {})))

(defn images
  "Append images block to markdown."
  [item text raw task]
  (let [items (or (:images raw) [])
        root (:root item)
        org (organizer/organizer root)
        name (organizer/name
              org
              (sess/created (:session item))
              (sess/topic (:session item))
              (sess/id (:session item)))
        base (.resolve root name)
        tag (organizer/slug (provider task))
        tag (if (str/blank? tag) "provider" tag)
        folder (.resolve base (str "images-" tag))
        lines (reduce
               (fn [list item]
                 (let [link (or (:image_url item) "")
                       title (or (:title item) "Chart")
                       code (or (:image_id item) "")
                       path (if (str/blank? link)
                              ""
                              (try (or (.getPath (java.net.URI. link)) "")
                                   (catch Exception _ "")))
                       part (second (re-find #"(\\.[^./]+)$" path))
                       part (if (str/blank? part) ".png" part)
                       file (if (str/blank? code)
                              ""
                              (.resolve folder (str code part)))
                       link (if (and (not (str/blank? code))
                                     (not (str/blank? (str file)))
                                     (Files/exists file
                                                   (make-array LinkOption 0)))
                              (.toString (.toUri file))
                              link)
                       add (and (not (str/blank? link))
                                (not (str/includes? text link)))]
                   (if add (conj list (str "![" title "](" link ")")) list)))
               []
               items)
        block (if (seq lines)
                (str "## Images\n\n" (str/join "\n" lines))
                "")
        rows (str/split text #"\n")
        size (count rows)
        idx (loop [i 0 hit -1]
              (if (< i size)
                (let [line (str/trim (nth rows i))
                      label (str/lower-case (str/replace line #"^#+\s*" ""))
                      mark (and (str/starts-with? line "#")
                                (#{"source"
                                   "sources"
                                   "reference"
                                   "references"} label))]
                  (recur (inc i) (if mark i hit)))
                hit))
        value (if (and (seq block) (>= idx 0))
                (let [head (str/join "\n" (take idx rows))
                      tail (str/join "\n" (drop idx rows))]
                  (str (str/trimr head)
                       "\n\n"
                       block
                       "\n\n"
                       (str/triml tail)))
                (if (seq block) (str (str/trimr text) "\n\n" block) text))]
    value))

(defn responsemap
  "Build response from raw map and task."
  [item raw task]
  (let [name (provider task)]
    (if (= name "valyu")
      (let [output (:output raw)
            text (if (map? output)
                   (or (:markdown output) (:content output) "")
                   (or output ""))
            text (images item text raw task)
            sources (or (:sources raw) [])
            val (valyu/valyu {:mode "basis"})
            base (research/basis val sources)
            state (or (:status raw) "completed")
            status (if (map? state) (or (:value state) state) state)
            code (or (:deepresearch_id raw) (:id raw) "")]
        (response/response {:id code
                            :status status
                            :output text
                            :basis base
                            :raw raw}))
      (let [output (:output raw)
            text (if (map? output) (or (:content output) "") "")
            basis (if (map? output) (or (:basis output) []) [])
            run (or (:run raw) {})
            code (if (map? run) (or (:run_id run) "") "")
            status (if (map? run) (or (:status run) "completed") "completed")]
        (response/response {:id code
                            :status status
                            :output text
                            :basis basis
                            :raw raw})))))

(defn resultmap
  "Return text and sources for task."
  [item task]
  (let [raw (raw item task)]
    (if (seq raw)
      (let [resp (responsemap item raw task)]
        [(response/text resp) (response/sources resp)])
      (let [value (task/result task)]
        [(result/summary value) (result/sources value)]))))
