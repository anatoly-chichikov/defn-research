(ns research.domain.session-test
  (:require [clojure.test :refer [deftest is]]
            [research.domain.pending :as pending]
            [research.domain.result :as result]
            [research.domain.session :as session]
            [research.domain.task :as task]))

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

(deftest the-session-generates-unique-id
  (let [dice (java.util.Random. 12001)
        day (inc (.nextInt dice 8))
        hour (inc (.nextInt dice 8))
        time (str "2026-01-0" day "T0" hour ":00:00")
        topic (token dice 6 1040 32)
        item (session/session {:topic topic
                               :tasks []
                               :created time})]
    (is (= 36 (count (session/id item)))
        "Session identifier length is incorrect")))

(deftest the-session-returns-provided-topic
  (let [dice (java.util.Random. 12003)
        day (inc (.nextInt dice 8))
        hour (inc (.nextInt dice 8))
        time (str "2026-01-0" day "T0" hour ":00:00")
        topic (token dice 7 12354 32)
        item (session/session {:id (uuid dice)
                               :topic topic
                               :tasks []
                               :created time})]
    (is (= topic (session/topic item))
        "Session topic did not match provided value")))

(deftest the-session-extend-adds-task
  (let [dice (java.util.Random. 12005)
        day (inc (.nextInt dice 8))
        hour (inc (.nextInt dice 8))
        time (str "2026-01-0" day "T0" hour ":00:00")
        topic (token dice 6 945 24)
        item (session/session {:id (uuid dice)
                               :topic topic
                               :tasks []
                               :created time})
        query (token dice 6 1040 32)
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
        output (session/extend item task)]
    (is (= 1 (count (session/tasks output)))
        "Extended session did not contain one task")))

(deftest the-session-extend-preserves-id
  (let [dice (java.util.Random. 12007)
        day (inc (.nextInt dice 8))
        hour (inc (.nextInt dice 8))
        time (str "2026-01-0" day "T0" hour ":00:00")
        topic (token dice 6 1040 32)
        ident (uuid dice)
        item (session/session {:id ident
                               :topic topic
                               :tasks []
                               :created time})
        query (token dice 6 1040 32)
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
        output (session/extend item task)]
    (is (= ident (session/id output))
        "Extended session ID did not match original")))

(deftest the-session-serializes-topic
  (let [dice (java.util.Random. 12009)
        day (inc (.nextInt dice 8))
        hour (inc (.nextInt dice 8))
        time (str "2026-01-0" day "T0" hour ":00:00")
        topic (token dice 6 12354 32)
        item (session/session {:id (uuid dice)
                               :topic topic
                               :tasks []
                               :created time})
        data (session/data item)]
    (is (= topic (:topic data))
        "Serialized topic did not match original")))

(deftest the-session-deserializes-correctly
  (let [dice (java.util.Random. 12011)
        day (inc (.nextInt dice 8))
        hour (inc (.nextInt dice 8))
        time (str "2026-01-0" day "T0" hour ":00:00")
        topic (token dice 6 12354 32)
        data {:id (uuid dice)
              :topic topic
              :tasks []
              :created time}
        item (session/session data)]
    (is (= topic (session/topic item))
        "Deserialized topic did not match")))

(deftest the-session-pending-returns-empty
  (let [dice (java.util.Random. 12013)
        day (inc (.nextInt dice 8))
        hour (inc (.nextInt dice 8))
        time (str "2026-01-0" day "T0" hour ":00:00")
        topic (token dice 6 1040 32)
        item (session/session {:id (uuid dice)
                               :topic topic
                               :tasks []
                               :created time})]
    (is (not (.isPresent (session/pending item)))
        "Pending run was present for new session")))

(deftest the-session-start-sets-pending
  (let [dice (java.util.Random. 12015)
        day (inc (.nextInt dice 8))
        hour (inc (.nextInt dice 8))
        time (str "2026-01-0" day "T0" hour ":00:00")
        run (token dice 6 1040 32)
        query (token dice 6 12354 32)
        processor (token dice 6 945 24)
        language (token dice 6 1040 32)
        provider (token dice 6 1040 32)
        item (session/session {:id (uuid dice)
                               :topic (token dice 5 1040 32)
                               :tasks []
                               :created time})
        hold (pending/pending {:run_id run
                               :query query
                               :processor processor
                               :language language
                               :provider provider})
        output (session/start item hold)]
    (is (= run (pending/id (.get (session/pending output))))
        "Pending run identifier did not match")))

(deftest the-session-clear-removes-pending
  (let [dice (java.util.Random. 12017)
        day (inc (.nextInt dice 8))
        hour (inc (.nextInt dice 8))
        time (str "2026-01-0" day "T0" hour ":00:00")
        run (token dice 6 1040 32)
        query (token dice 6 12354 32)
        processor (token dice 6 945 24)
        language (token dice 6 1040 32)
        provider (token dice 6 1040 32)
        item (session/session {:id (uuid dice)
                               :topic (token dice 5 1040 32)
                               :tasks []
                               :created time
                               :pending {:run_id run
                                         :query query
                                         :processor processor
                                         :language language
                                         :provider provider}})
        output (session/reset item)]
    (is (not (.isPresent (session/pending output)))
        "Pending run was not cleared")))

(deftest the-session-serializes-pending
  (let [dice (java.util.Random. 12019)
        day (inc (.nextInt dice 8))
        hour (inc (.nextInt dice 8))
        time (str "2026-01-0" day "T0" hour ":00:00")
        run (token dice 6 1040 32)
        query (token dice 6 12354 32)
        processor (token dice 6 945 24)
        language (token dice 6 1040 32)
        provider (token dice 6 1040 32)
        hold (pending/pending {:run_id run
                               :query query
                               :processor processor
                               :language language
                               :provider provider})
        item (session/start (session/session {:id (uuid dice)
                                              :topic (token dice 5 1040 32)
                                              :tasks []
                                              :created time})
                            hold)
        data (session/data item)]
    (is (= run (get-in data [:pending :run_id]))
        "Serialized pending run_id did not match")))

(deftest the-session-deserializes-pending
  (let [dice (java.util.Random. 12021)
        day (inc (.nextInt dice 8))
        hour (inc (.nextInt dice 8))
        time (str "2026-01-0" day "T0" hour ":00:00")
        run (token dice 6 1040 32)
        query (token dice 6 12354 32)
        processor (token dice 6 945 24)
        language (token dice 6 1040 32)
        provider (token dice 6 1040 32)
        data {:id (uuid dice)
              :topic (token dice 5 1040 32)
              :tasks []
              :created time
              :pending {:run_id run
                        :query query
                        :processor processor
                        :language language
                        :provider provider}}
        item (session/session data)]
    (is (= run (pending/id (.get (session/pending item))))
        "Deserialized pending run did not match")))
