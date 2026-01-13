(ns research.storage.repository-test
  (:require [clojure.test :refer [deftest is]]
            [research.domain.result :as result]
            [research.domain.session :as session]
            [research.domain.task :as task]
            [research.storage.repository :as repo])
  (:import (java.nio.file Files)
           (java.nio.file.attribute FileAttribute)))

(defn token
  "Return deterministic token string."
  [dice size base span]
  (let [build (StringBuilder.)]
    (dotimes [_ size]
      (let [pick (.nextInt dice span)
            code (+ base pick)]
        (.append build (char code))))
    (.toString build)))

(defn uuid
  "Return deterministic UUID string."
  [dice]
  (str (java.util.UUID. (.nextLong dice) (.nextLong dice))))

(deftest the-repository-returns-empty-for-empty-folder
  (let [dice (java.util.Random. 24001)
        root (Files/createTempDirectory "repo"
                                        (make-array FileAttribute 0))
        mark (token dice 4 97 26)
        name (str mark "-" (uuid dice))
        path (.resolve root name)
        _ (Files/createDirectories path (make-array FileAttribute 0))
        item (repo/repo root)]
    (is (= 0 (count (repo/load item)))
        "Load did not return empty list for empty folder")))

(deftest the-repository-saves-and-loads-session
  (let [dice (java.util.Random. 24003)
        root (Files/createTempDirectory "repo"
                                        (make-array FileAttribute 0))
        item (repo/repo root)
        topic (token dice 6 1040 32)
        ident (uuid dice)
        day (inc (.nextInt dice 8))
        hour (inc (.nextInt dice 8))
        time (str "2026-01-0" day "T0" hour ":00:00")
        entry (session/session {:id ident
                                :topic topic
                                :tasks []
                                :created time})]
    (repo/save item [entry])
    (is (= topic (session/topic (first (repo/load item))))
        "Loaded session topic did not match saved")))

(deftest the-repository-append-adds-session
  (let [dice (java.util.Random. 24005)
        root (Files/createTempDirectory "repo"
                                        (make-array FileAttribute 0))
        item (repo/repo root)
        topic (token dice 6 1040 32)
        label (token dice 6 1040 32)
        day (inc (.nextInt dice 8))
        hour (inc (.nextInt dice 8))
        time (str "2026-01-0" day "T0" hour ":00:00")
        alpha (session/session {:id (uuid dice)
                                :topic topic
                                :tasks []
                                :created time})
        beta (session/session {:id (uuid dice)
                               :topic label
                               :tasks []
                               :created time})]
    (repo/append item alpha)
    (repo/append item beta)
    (is (= 2 (count (repo/load item)))
        "Repository did not contain two sessions after append")))

(deftest the-repository-find-returns-matching-session
  (let [dice (java.util.Random. 24007)
        root (Files/createTempDirectory "repo"
                                        (make-array FileAttribute 0))
        item (repo/repo root)
        topic (token dice 6 1040 32)
        ident (uuid dice)
        day (inc (.nextInt dice 8))
        hour (inc (.nextInt dice 8))
        time (str "2026-01-0" day "T0" hour ":00:00")
        entry (session/session {:id ident
                                :topic topic
                                :tasks []
                                :created time})]
    (repo/append item entry)
    (let [hit (repo/find item ident)]
      (is (= topic (session/topic (.get hit)))
          "Found session topic did not match"))))

(deftest the-repository-find-returns-empty-for-missing
  (let [dice (java.util.Random. 24009)
        root (Files/createTempDirectory "repo"
                                        (make-array FileAttribute 0))
        item (repo/repo root)
        code (token dice 6 1040 32)
        hit (repo/find item code)]
    (is (not (.isPresent hit)) "Find returned value for missing ID")))

(deftest the-repository-update-modifies-session
  (let [dice (java.util.Random. 24011)
        root (Files/createTempDirectory "repo"
                                        (make-array FileAttribute 0))
        item (repo/repo root)
        topic (token dice 6 1040 32)
        ident (uuid dice)
        day (inc (.nextInt dice 8))
        hour (inc (.nextInt dice 8))
        time (str "2026-01-0" day "T0" hour ":00:00")
        entry (session/session {:id ident
                                :topic topic
                                :tasks []
                                :created time})]
    (repo/append item entry)
    (let [query (token dice 6 1040 32)
          status (token dice 6 1040 32)
          language (token dice 5 1040 32)
          service (token dice 4 1040 32)
          summary (token dice 6 1040 32)
          value (result/->Result summary [])
          task (task/task {:id (uuid dice)
                           :query query
                           :status status
                           :language language
                           :service service
                           :result (result/data value)
                           :created time})
          revision (session/extend entry task)]
      (repo/update item revision)
      (let [hit (repo/find item ident)]
        (is (= 1 (count (session/tasks (.get hit))))
            "Updated session did not contain task")))))

(deftest ^{:doc "Migrates legacy session folders"}
  the-repository-migrates-legacy-folders
  (let [dice (java.util.Random. 24013)
        root (Files/createTempDirectory "repo"
                                        (make-array FileAttribute 0))
        date (str "2026-01-0" (inc (.nextInt dice 8)))
        slug (token dice 6 97 26)
        code (subs (uuid dice) 0 8)
        name (str date "_" slug "_" code)
        path (.resolve root name)
        _ (Files/createDirectories path (make-array FileAttribute 0))
        tag (token dice 4 97 26)
        text (str "данные-" (uuid dice))
        input (.resolve path (str "input-" tag ".md"))
        response (.resolve path (str "response-" tag ".json"))
        _ (spit (.toFile input) text :encoding "UTF-8")
        _ (spit (.toFile response) "{}" :encoding "UTF-8")
        repo (repo/repo root)
        _ (repo/load repo)
        file (.resolve path "session.edn")
        flag (.exists (.toFile file))]
    (is flag "Migration did not create session edn")))

(deftest ^{:doc "Builds tasks from response files"}
  the-repository-builds-tasks-from-responses
  (let [dice (java.util.Random. 24015)
        root (Files/createTempDirectory "repo"
                                        (make-array FileAttribute 0))
        date (str "2026-01-0" (inc (.nextInt dice 8)))
        slug (token dice 6 97 26)
        code (subs (uuid dice) 0 8)
        name (str date "_" slug "_" code)
        path (.resolve root name)
        _ (Files/createDirectories path (make-array FileAttribute 0))
        alpha (token dice 4 97 26)
        beta (token dice 4 97 26)
        text (str "데이터-" (uuid dice))
        input (.resolve path (str "input-" alpha ".md"))
        note (.resolve path (str "input-" beta ".md"))
        left (.resolve path (str "response-" alpha ".json"))
        right (.resolve path (str "response-" beta ".json"))
        _ (spit (.toFile input) text :encoding "UTF-8")
        _ (spit (.toFile note) text :encoding "UTF-8")
        _ (spit (.toFile left) "{}" :encoding "UTF-8")
        _ (spit (.toFile right) "{}" :encoding "UTF-8")
        repo (repo/repo root)
        list (repo/load repo)
        item (first list)
        size (count (session/tasks item))]
    (is (= 2 size) "Migration did not build tasks from responses")))

(deftest ^{:doc "Strips query from session edn"}
  the-repository-strips-query-from-session-edn
  (let [dice (java.util.Random. 24017)
        root (Files/createTempDirectory "repo"
                                        (make-array FileAttribute 0))
        date (str "2026-01-0" (inc (.nextInt dice 8)))
        slug (token dice 6 97 26)
        code (subs (uuid dice) 0 8)
        name (str date "_" slug "_" code)
        path (.resolve root name)
        _ (Files/createDirectories path (make-array FileAttribute 0))
        tag (token dice 4 97 26)
        note (token dice 6 1040 32)
        input (.resolve path (str "input-" tag ".md"))
        _ (spit (.toFile input) note :encoding "UTF-8")
        time (str date "T00:00:00")
        task {:id (uuid dice)
              :query (token dice 6 1040 32)
              :status (token dice 6 945 24)
              :language (token dice 5 1040 32)
              :service tag
              :created time}
        hold {:run_id (uuid dice)
              :query (token dice 6 1040 32)
              :processor (token dice 6 945 24)
              :language (token dice 6 1040 32)
              :provider tag}
        data {:id (uuid dice)
              :topic (token dice 6 1040 32)
              :tasks [task]
              :pending hold
              :created time}
        file (.resolve path "session.edn")
        _ (spit (.toFile file) (pr-str data) :encoding "UTF-8")
        repo (repo/repo root)
        _ (repo/load repo)
        body (slurp (.toFile file) :encoding "UTF-8")
        flag (not (re-find #":query" body))]
    (is flag "Session edn still included query")))
