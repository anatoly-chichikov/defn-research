(ns research.domain.session-test
  (:require [clojure.test :refer [deftest is]]
            [research.domain.pending :as pending]
            [research.domain.session :as session]
            [research.domain.task :as task]))

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

(deftest the-session-generates-unique-id
  (let [rng (java.util.Random. 12001)
        topic (token rng 6 1040 32)
        item (session/session {:topic topic
                               :tasks []
                               :created (session/format (session/now))})]
    (is (= 36 (count (session/id item)))
        "Session identifier length is incorrect")))

(deftest the-session-returns-provided-topic
  (let [rng (java.util.Random. 12003)
        topic (token rng 7 12354 32)
        item (session/session {:topic topic
                               :tasks []
                               :created (session/format (session/now))})]
    (is (= topic (session/topic item))
        "Session topic did not match provided value")))

(deftest the-session-extend-adds-task
  (let [rng (java.util.Random. 12005)
        topic (token rng 6 945 24)
        item (session/session {:topic topic
                               :tasks []
                               :created (session/format (session/now))})
        task (task/task {:query "q"
                         :status "pending"
                         :result nil
                         :created (task/format (task/now))})
        next (session/extend item task)]
    (is (= 1 (count (session/tasks next)))
        "Extended session did not contain one task")))

(deftest the-session-extend-preserves-id
  (let [rng (java.util.Random. 12007)
        topic (token rng 6 1040 32)
        item (session/session {:topic topic
                               :tasks []
                               :created (session/format (session/now))})
        task (task/task {:query "q"
                         :status "pending"
                         :result nil
                         :created (task/format (task/now))})
        next (session/extend item task)]
    (is (= (session/id item) (session/id next))
        "Extended session ID did not match original")))

(deftest the-session-serializes-topic
  (let [rng (java.util.Random. 12009)
        topic (token rng 6 12354 32)
        item (session/session {:topic topic
                               :tasks []
                               :created (session/format (session/now))})
        data (session/data item)]
    (is (= topic (:topic data))
        "Serialized topic did not match original")))

(deftest the-session-deserializes-correctly
  (let [rng (java.util.Random. 12011)
        topic (token rng 6 12354 32)
        data {:id (uuid rng)
              :topic topic
              :tasks []
              :created "2025-12-06T10:00:00"}
        item (session/session data)]
    (is (= topic (session/topic item))
        "Deserialized topic did not match")))

(deftest the-session-pending-returns-empty
  (let [rng (java.util.Random. 12013)
        topic (token rng 6 1040 32)
        item (session/session {:topic topic
                               :tasks []
                               :created (session/format (session/now))})]
    (is (not (.isPresent (session/pending item)))
        "Pending run was present for new session")))

(deftest the-session-start-sets-pending
  (let [rng (java.util.Random. 12015)
        run (token rng 6 1040 32)
        item (session/session {:topic "t"
                               :tasks []
                               :created (session/format (session/now))})
        pend (pending/pending {:run_id run
                               :query "q"
                               :processor "pro"
                               :language "english"
                               :provider "parallel"})
        next (session/start item pend)]
    (is (= run (pending/id (.get (session/pending next))))
        "Pending run identifier did not match")))

(deftest the-session-clear-removes-pending
  (let [rng (java.util.Random. 12017)
        run (token rng 6 1040 32)
        item (session/session {:topic "t"
                               :tasks []
                               :created (session/format (session/now))
                               :pending {:run_id run
                                         :query "q"
                                         :processor "pro"
                                         :language "english"
                                         :provider "parallel"}})
        next (session/reset item)]
    (is (not (.isPresent (session/pending next)))
        "Pending run was not cleared")))

(deftest the-session-serializes-pending
  (let [rng (java.util.Random. 12019)
        run (token rng 6 1040 32)
        pend (pending/pending {:run_id run
                               :query "q"
                               :processor "pro"
                               :language "en"
                               :provider "parallel"})
        item (session/start (session/session {:topic "t"
                                              :tasks []
                                              :created (session/format
                                                        (session/now))})
                            pend)
        data (session/data item)]
    (is (= run (get-in data [:pending :run_id]))
        "Serialized pending run_id did not match")))

(deftest the-session-deserializes-pending
  (let [rng (java.util.Random. 12021)
        run (token rng 6 1040 32)
        data {:id (uuid rng)
              :topic "test"
              :tasks []
              :created "2025-12-06T10:00:00"
              :pending {:run_id run
                        :query "test query"
                        :processor "ultra"
                        :language "русский"}}
        item (session/session data)]
    (is (= run (pending/id (.get (session/pending item))))
        "Deserialized pending run did not match")))
