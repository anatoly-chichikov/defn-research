(ns research.domain.task-test
  (:require [clojure.test :refer [deftest is]]
            [research.domain.result :as result]
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

(deftest the-task-generates-unique-id
  (let [dice (java.util.Random. 11001)
        day (inc (.nextInt dice 8))
        hour (inc (.nextInt dice 8))
        time (str "2026-01-0" day "T0" hour ":00:00")
        query (token dice 6 1040 32)
        status (token dice 6 1040 32)
        language (token dice 5 1040 32)
        service (token dice 4 1040 32)
        summary (token dice 6 1040 32)
        value (result/->Result summary [])
        item (task/task {:query query
                         :status status
                         :language language
                         :service service
                         :result (result/data value)
                         :created time})]
    (is (= 36 (count (task/id item)))
        "Task identifier length is incorrect")))

(deftest the-task-returns-provided-query
  (let [dice (java.util.Random. 11003)
        day (inc (.nextInt dice 8))
        hour (inc (.nextInt dice 8))
        time (str "2026-01-0" day "T0" hour ":00:00")
        query (token dice 7 12354 32)
        status (token dice 6 1040 32)
        language (token dice 5 1040 32)
        service (token dice 4 1040 32)
        summary (token dice 6 1040 32)
        value (result/->Result summary [])
        item (task/task {:id (uuid dice)
                         :query query
                         :status status
                         :language language
                         :service service
                         :result (result/data value)
                         :created time})]
    (is (= query (task/query item))
        "Task query did not match provided value")))

(deftest the-task-returns-provided-status
  (let [dice (java.util.Random. 11005)
        day (inc (.nextInt dice 8))
        hour (inc (.nextInt dice 8))
        time (str "2026-01-0" day "T0" hour ":00:00")
        status (token dice 6 945 24)
        query (token dice 6 1040 32)
        language (token dice 5 1040 32)
        service (token dice 4 1040 32)
        summary (token dice 6 1040 32)
        value (result/->Result summary [])
        item (task/task {:id (uuid dice)
                         :query query
                         :status status
                         :language language
                         :service service
                         :result (result/data value)
                         :created time})]
    (is (= status (task/status item))
        "Task status did not match provided value")))

(deftest the-task-complete-returns-new-task
  (let [dice (java.util.Random. 11007)
        day (inc (.nextInt dice 8))
        hour (inc (.nextInt dice 8))
        time (str "2026-01-0" day "T0" hour ":00:00")
        query (token dice 6 1040 32)
        status (token dice 6 1040 32)
        language (token dice 5 1040 32)
        service (token dice 4 1040 32)
        summary (token dice 5 1040 32)
        value (result/->Result summary [])
        item (task/task {:id (uuid dice)
                         :query query
                         :status status
                         :language language
                         :service service
                         :result (result/data value)
                         :created time})
        output (task/finish item value)]
    (is (= "completed" (task/status output))
        "Completed task status was not completed")))

(deftest the-task-complete-preserves-id
  (let [dice (java.util.Random. 11009)
        day (inc (.nextInt dice 8))
        hour (inc (.nextInt dice 8))
        time (str "2026-01-0" day "T0" hour ":00:00")
        query (token dice 6 1040 32)
        status (token dice 6 1040 32)
        language (token dice 5 1040 32)
        service (token dice 4 1040 32)
        summary (token dice 6 1024 32)
        value (result/->Result summary [])
        item (task/task {:id (uuid dice)
                         :query query
                         :status status
                         :language language
                         :service service
                         :result (result/data value)
                         :created time})
        output (task/finish item value)]
    (is (= (task/id item) (task/id output))
        "Completed task ID did not match original")))

(deftest the-task-complete-adds-timestamp
  (let [dice (java.util.Random. 11011)
        day (inc (.nextInt dice 8))
        hour (inc (.nextInt dice 8))
        time (str "2026-01-0" day "T0" hour ":00:00")
        query (token dice 6 1040 32)
        status (token dice 6 1040 32)
        language (token dice 5 1040 32)
        service (token dice 4 1040 32)
        summary (token dice 6 1040 32)
        value (result/->Result summary [])
        item (task/task {:id (uuid dice)
                         :query query
                         :status status
                         :language language
                         :service service
                         :result (result/data value)
                         :created time})
        output (task/finish item value)]
    (is (.isPresent (task/completed output))
        "Completed task timestamp was missing")))

(deftest the-task-omits-query-serialization
  (let [dice (java.util.Random. 11013)
        day (inc (.nextInt dice 8))
        hour (inc (.nextInt dice 8))
        time (str "2026-01-0" day "T0" hour ":00:00")
        query (token dice 6 12354 32)
        status (token dice 6 1040 32)
        language (token dice 5 1040 32)
        service (token dice 4 1040 32)
        summary (token dice 6 1040 32)
        value (result/->Result summary [])
        item (task/task {:id (uuid dice)
                         :query query
                         :status status
                         :language language
                         :service service
                         :result (result/data value)
                         :created time})
        data (task/data item)]
    (is (not (contains? data :query))
        "Serialized task still included query")))

(deftest the-task-deserializes-correctly
  (let [dice (java.util.Random. 11015)
        day (inc (.nextInt dice 8))
        hour (inc (.nextInt dice 8))
        time (str "2026-01-0" day "T0" hour ":00:00")
        query (token dice 7 1040 32)
        status (token dice 6 945 24)
        language (token dice 5 1040 32)
        service (token dice 4 1040 32)
        summary (token dice 6 1040 32)
        value (result/->Result summary [])
        data {:id (uuid dice)
              :query query
              :status status
              :language language
              :service service
              :result (result/data value)
              :created time}
        item (task/task data)]
    (is (= query (task/query item))
        "Deserialized query did not match")))
