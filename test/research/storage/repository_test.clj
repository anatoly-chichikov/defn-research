(ns research.storage.repository-test
  (:require [clojure.test :refer [deftest is]]
            [research.domain.session :as session]
            [research.domain.task :as task]
            [research.storage.file :as file]
            [research.storage.repository :as repo])
  (:import (java.nio.file Files)
           (java.nio.file.attribute FileAttribute)))

(defn token
  "Return deterministic token string."
  [rng size base span]
  (let [build (StringBuilder.)]
    (dotimes [_ size]
      (let [pick (.nextInt rng span)
            code (+ base pick)]
        (.append build (char code))))
    (.toString build)))

(defn uuid
  "Return deterministic UUID string."
  [rng]
  (str (java.util.UUID. (.nextLong rng) (.nextLong rng))))

(deftest the-repository-returns-empty-for-missing-file
  (let [rng (java.util.Random. 24001)
        root (Files/createTempDirectory "repo"
                                        (make-array FileAttribute 0))
        path (.resolve root (str "missing-" (uuid rng) ".json"))
        item (repo/repo (file/file path))]
    (is (= 0 (count (repo/load item)))
        "Load did not return empty list for missing file")))

(deftest the-repository-saves-and-loads-session
  (let [rng (java.util.Random. 24003)
        root (Files/createTempDirectory "repo"
                                        (make-array FileAttribute 0))
        path (.resolve root (str "repo-" (uuid rng) ".json"))
        item (repo/repo (file/file path))
        topic (str "topic-" (uuid rng))
        sess (session/session {:topic topic
                               :tasks []
                               :created (session/format (session/now))})]
    (repo/save item [sess])
    (is (= topic (session/topic (first (repo/load item))))
        "Loaded session topic did not match saved")))

(deftest the-repository-append-adds-session
  (let [rng (java.util.Random. 24005)
        root (Files/createTempDirectory "repo"
                                        (make-array FileAttribute 0))
        path (.resolve root (str "append-" (uuid rng) ".json"))
        item (repo/repo (file/file path))
        first (session/session {:topic "first"
                                :tasks []
                                :created (session/format (session/now))})
        second (session/session {:topic "second"
                                 :tasks []
                                 :created (session/format (session/now))})]
    (repo/append item first)
    (repo/append item second)
    (is (= 2 (count (repo/load item)))
        "Repository did not contain two sessions after append")))

(deftest the-repository-find-returns-matching-session
  (let [rng (java.util.Random. 24007)
        root (Files/createTempDirectory "repo"
                                        (make-array FileAttribute 0))
        path (.resolve root (str "find-" (uuid rng) ".json"))
        item (repo/repo (file/file path))
        sess (session/session {:topic "findme"
                               :tasks []
                               :created (session/format (session/now))})]
    (repo/append item sess)
    (let [found (repo/find item (session/id sess))]
      (is (= "findme" (session/topic (.get found)))
          "Found session topic did not match"))))

(deftest the-repository-find-returns-empty-for-missing
  (let [rng (java.util.Random. 24009)
        root (Files/createTempDirectory "repo"
                                        (make-array FileAttribute 0))
        path (.resolve root (str "notfound-" (uuid rng) ".json"))
        item (repo/repo (file/file path))
        found (repo/find item (uuid rng))]
    (is (not (.isPresent found)) "Find returned value for missing ID")))

(deftest the-repository-update-modifies-session
  (let [rng (java.util.Random. 24011)
        root (Files/createTempDirectory "repo"
                                        (make-array FileAttribute 0))
        path (.resolve root (str "update-" (uuid rng) ".json"))
        item (repo/repo (file/file path))
        sess (session/session {:topic "original"
                               :tasks []
                               :created (session/format (session/now))})]
    (repo/append item sess)
    (let [task (task/task {:query "q"
                           :status "pending"
                           :result nil
                           :created (task/format (task/now))})
          next (session/extend sess task)]
      (repo/update item next)
      (let [found (repo/find item (session/id sess))]
        (is (= 1 (count (session/tasks (.get found))))
            "Updated session did not contain task")))))
