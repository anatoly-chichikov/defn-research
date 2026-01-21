(ns research.main
  (:refer-clojure :exclude [list])
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.tools.cli :as cli]
            [org.httpkit.client :as http]
            [research.api.parallel :as parallel]
            [research.api.research :as research]
            [research.api.response :as response]
            [research.api.valyu :as valyu]
            [research.api.xai :as xai]
            [research.domain.pending :as pending]
            [research.domain.result :as result]
            [research.domain.session :as session]
            [research.domain.task :as task]
            [research.image.generator :as image]
            [research.pdf.document :as document]
            [research.pdf.palette :as palette]
            [research.storage.organizer :as organizer]
            [research.storage.repository :as repo])
  (:import (java.nio.file Files LinkOption)
           (java.nio.file.attribute FileAttribute)
           (java.util Optional UUID)))

(defprotocol Applied
  "Object that runs CLI operations."
  (list [item] "List sessions.")
  (show [item id] "Show session details.")
  (generate [item id html] "Generate report for session.")
  (create [item topic] "Create session.")
  (run [item topic query processor language provider]
    "Create session and run research.")
  (research [item id query processor language provider]
    "Run research for session."))

(defn env
  "Return environment value by key."
  [key]
  (System/getenv key))

(defn store
  "Store Valyu images for output folder."
  [name provider data root]
  (let [items (or (:images data) [])]
    (when (= provider "valyu")
      (let [org (organizer/organizer root)
            tag (organizer/slug provider)
            tag (if (str/blank? tag) "provider" tag)
            folder (organizer/folder org name provider)
            folder (.resolve folder (str "images-" tag))]
        (doseq [item items]
          (let [link (or (:image_url item) "")
                code (or (:image_id item) "")
                path (if (or (str/blank? link) (str/blank? code))
                       ""
                       (try (or (.getPath (java.net.URI. link)) "")
                            (catch Exception _ "")))
                part (second (re-find #"(\\.[^./]+)$" path))
                part (if (str/blank? part) ".png" part)
                file (if (str/blank? code)
                       ""
                       (.resolve folder "images"))
                file (if (str/blank? (str file))
                       ""
                       (.resolve file (str code part)))]
            (when (and (not (str/blank? link)) (not (str/blank? (str file))))
              (Files/createDirectories
               (.getParent file)
               (make-array FileAttribute 0))
              (when-not (Files/exists file (make-array LinkOption 0))
                (let [response @(http/get link {:timeout 60000
                                                :as :byte-array})]
                  (when (< (:status response) 300)
                    (Files/write
                     file
                     (:body response)
                     (make-array java.nio.file.OpenOption 0))))))))))))

(defrecord App [root data out]
  Applied
  (list [_]
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
  (show [_ id]
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
              (let [value (task/result task)]
                (when (result/present value)
                  (let [head (subs (result/summary value)
                                   0
                                   (min 100 (count (result/summary value))))]
                    (println (str "  Summary: " head " [truncated]")))
                  (let [count (count (result/sources value))]
                    (println (str "  Sources: " count)))))))
        (println (str "Session not found: " id)))))
  (generate [_ id html]
    (let [repo (repo/repo data)
          list (repo/load repo)
          pick (first (filter #(str/starts-with? (session/id %) id) list))]
      (if pick
        (let [provider (if (seq (session/tasks pick))
                         (let [last (last (session/tasks pick))
                               name (task/service last)]
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
  (create [_ topic]
    (let [repo (repo/repo data)
          id (str (UUID/randomUUID))
          map {:id id
               :topic topic
               :tasks []
               :created (task/format (task/now))}
          value (session/session map)]
      (repo/append repo value)
      (println (str "Created session: " (subs id 0 8)))
      (println (str "Topic: " topic))
      (subs id 0 8)))
  (run [app topic query processor language provider]
    (let [id (create app topic)
          mode (cond
                 (= processor "lite")
                 (throw (ex-info
                         "Run failed because processor lite is not supported"
                         {:processor processor}))
                 (and (= provider "valyu")
                      (not (or (= processor "fast")
                               (= processor "standard")
                               (= processor "heavy"))))
                 (throw (ex-info
                         (str "Run failed because processor is not supported"
                              " for valyu")
                         {:processor processor}))
                 (or (= processor "fast")
                     (= processor "standard")
                     (= processor "heavy"))
                 processor
                 :else "standard")
          pairs (if (= provider "all")
                  [["parallel" processor]
                   ["valyu" mode]]
                  [[provider
                    (if (= provider "valyu") mode processor)]])]
      (doseq [pair pairs]
        (let [name (first pair)
              proc (second pair)]
          (research app id query proc language name)))))
  (research [_ id query processor language provider]
    (let [repo (repo/repo data)
          list (repo/load repo)
          pick (first (filter #(str/starts-with? (session/id %) id) list))]
      (if (not pick)
        (println (str "Session not found: " id))
        (let [pending (session/pending pick)]
          (println (str "Session: " (session/topic pick)))
          (if (.isPresent pending)
            (let [pend (.get pending)
                  run (pending/id pend)
                  query (pending/query pend)
                  processor (pending/processor pend)
                  language (pending/language pend)
                  provider (pending/provider pend)
                  exec (cond
                         (= provider "valyu")
                         (valyu/valyu {:key (env "VALYU_API_KEY")})
                         (= provider "xai")
                         (xai/xai {:root root})
                         :else (parallel/parallel))
                  org (organizer/organizer out)
                  name (organizer/name
                        org
                        (session/created pick)
                        (session/topic pick)
                        (session/id pick))
                  _ (organizer/input org name provider query)]
              (println (str "Resuming run: " (subs run 0 (min 16 (count run)))))
              (println (str "Query: " query))
              (println (str "Processor: " processor))
              (println "Streaming progress")
              (research/stream exec run)
              (println "Fetching result")
              (let [resp (research/finish exec run)
                    updated (session/reset pick)
                    _ (repo/update repo updated)
                    org (organizer/organizer out)
                    name (organizer/name
                          org
                          (session/created pick)
                          (session/topic pick)
                          (session/id pick))
                    _ (organizer/response org name provider (response/raw resp))
                    _ (store name provider (response/raw resp) out)
                    summary (response/text resp)
                    sources (response/sources resp)
                    pack {:summary summary
                          :sources (mapv result/data sources)}
                    task (task/task {:id (str (UUID/randomUUID))
                                     :query query
                                     :status "completed"
                                     :language language
                                     :service (str provider ".ai")
                                     :created (task/format (task/now))
                                     :result pack})
                    final (session/extend updated task)
                    _ (repo/update repo final)
                    cover (organizer/cover org name provider)
                    coveropt (Optional/of cover)
                    key (or (env "GEMINI_API_KEY") "")]
                (let [file (organizer/folder org name provider)
                      count (count (response/sources resp))]
                  (println (str "Response saved: " (.toString file)))
                  (println (str "Results saved: " count " sources")))
                (if (str/blank? key)
                  (println "Gemini API key not set skipping image generation")
                  (do (println "Generating cover image")
                      (let [gen (image/generator)]
                        (image/generate gen (session/topic final) cover)
                        (println (str "Cover generated: " (.toString cover))))))
                (let [doc (document/document
                           final
                           (palette/palette)
                           coveropt
                           out)
                      path (organizer/report org name provider)]
                  (document/save doc path)
                  (println (str "PDF generated: " (.toString path))))))
            (let [allow #{"parallel" "valyu" "xai"}
                  _ (when-not (contains? allow provider)
                      (throw (ex-info
                              "Provider must be parallel valyu or xai"
                              {})))
                  check (or (= processor "fast")
                            (= processor "standard")
                            (= processor "heavy"))
                  _ (when (and (= provider "valyu") (not check))
                      (throw
                       (ex-info
                        "Processor must be fast standard or heavy for valyu"
                        {})))
                  exec (cond
                         (= provider "valyu")
                         (valyu/valyu {:key (env "VALYU_API_KEY")})
                         (= provider "xai")
                         (xai/xai {:root root})
                         :else (parallel/parallel))
                  org (organizer/organizer out)
                  name (organizer/name
                        org
                        (session/created pick)
                        (session/topic pick)
                        (session/id pick))
                  _ (organizer/input org name provider query)
                  run (research/start exec query processor)
                  pend (pending/pending {:run_id run
                                         :query query
                                         :processor processor
                                         :language language
                                         :provider provider})
                  state (session/start pick pend)
                  _ (repo/update repo state)]
              (println (str "Query: " query))
              (println (str "Processor: " processor))
              (println (str "Language: " language))
              (println (str "Research started: " run))
              (println "Streaming progress")
              (research/stream exec run)
              (println "Fetching result")
              (let [resp (research/finish exec run)
                    updated (session/reset pick)
                    _ (repo/update repo updated)
                    org (organizer/organizer out)
                    name (organizer/name
                          org
                          (session/created pick)
                          (session/topic pick)
                          (session/id pick))
                    _ (organizer/response org name provider (response/raw resp))
                    _ (store name provider (response/raw resp) out)
                    summary (response/text resp)
                    sources (response/sources resp)
                    pack {:summary summary
                          :sources (mapv result/data sources)}
                    task (task/task {:id (str (UUID/randomUUID))
                                     :query query
                                     :status "completed"
                                     :language language
                                     :service (str provider ".ai")
                                     :created (task/format (task/now))
                                     :result pack})
                    final (session/extend updated task)
                    _ (repo/update repo final)
                    cover (organizer/cover org name provider)
                    coveropt (Optional/of cover)
                    key (or (env "GEMINI_API_KEY") "")]
                (let [file (organizer/folder org name provider)
                      count (count (response/sources resp))]
                  (println (str "Response saved: " (.toString file)))
                  (println (str "Results saved: " count " sources")))
                (if (str/blank? key)
                  (println "Gemini API key not set skipping image generation")
                  (do (println "Generating cover image")
                      (let [gen (image/generator)]
                        (image/generate gen (session/topic final) cover)
                        (println (str "Cover generated: " (.toString cover))))))
                (let [doc (document/document
                           final
                           (palette/palette)
                           coveropt
                           out)
                      path (organizer/report org name provider)]
                  (document/save doc path)
                  (println (str "PDF generated: " (.toString path))))))))))))

(defn app
  "Create application instance."
  [root]
  (let [out (.resolve root "output")
        data out]
    (->App root data out)))

(defn parse
  "Parse CLI arguments."
  [args]
  (let [opts [[nil "--processor PROCESSOR" "Processor" :default "pro"]
              [nil "--language LANGUAGE" "Language" :default "русский"]
              [nil "--provider PROVIDER" "Provider" :default "parallel"]
              [nil "--html" "HTML" :default false]]
        parse (cli/parse-opts args opts)
        list (:arguments parse)
        opts (:options parse)
        cmd (first list)
        tail (rest list)]
    {:cmd cmd
     :tail tail
     :opts opts}))

(defn -main
  "Entry point."
  [& args]
  (let [root (.toPath (io/file "."))
        app (app root)
        data (parse args)
        cmd (:cmd data)
        tail (:tail data)
        opts (:opts data)]
    (case cmd
      "list" (list app)
      "show" (show app (first tail))
      "generate" (generate app (first tail) (:html opts))
      "create" (create app (str/join " " tail))
      "run" (run app
                 (first tail)
                 (second tail)
                 (:processor opts)
                 (:language opts)
                 (:provider opts))
      "research" (research app
                           (first tail)
                           (second tail)
                           (:processor opts)
                           (:language opts)
                           (:provider opts))
      (println "Unknown command"))))
