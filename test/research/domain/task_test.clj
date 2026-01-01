(ns research.domain.task-test
  (:require [clojure.test :refer [deftest is]]
            [research.domain.result :as result]
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

(deftest the-task-generates-unique-id
  (let [rng (java.util.Random. 11001)
        query (token rng 6 1040 32)
        item (task/task {:query query
                         :status "pending"
                         :result nil
                         :created (task/format (task/now))})]
    (is (= 36 (count (task/id item)))
        "Task identifier length is incorrect")))

(deftest the-task-returns-provided-query
  (let [rng (java.util.Random. 11003)
        query (token rng 7 12354 32)
        item (task/task {:query query
                         :status "pending"
                         :result nil
                         :created (task/format (task/now))})]
    (is (= query (task/query item))
        "Task query did not match provided value")))

(deftest the-task-returns-provided-status
  (let [rng (java.util.Random. 11005)
        status (token rng 6 945 24)
        item (task/task {:query "q"
                         :status status
                         :result nil
                         :created (task/format (task/now))})]
    (is (= status (task/status item))
        "Task status did not match provided value")))

(deftest the-task-complete-returns-new-task
  (let [rng (java.util.Random. 11007)
        summary (token rng 5 1040 32)
        item (task/task {:query "q"
                         :status "pending"
                         :result nil
                         :created (task/format (task/now))})
        value (result/->Result summary [])
        done (task/finish item value)]
    (is (= "completed" (task/status done))
        "Completed task status was not completed")))

(deftest the-task-complete-preserves-id
  (let [rng (java.util.Random. 11009)
        summary (token rng 6 1024 32)
        item (task/task {:query "q"
                         :status "pending"
                         :result nil
                         :created (task/format (task/now))})
        value (result/->Result summary [])
        done (task/finish item value)]
    (is (= (task/id item) (task/id done))
        "Completed task ID did not match original")))

(deftest the-task-complete-adds-timestamp
  (let [rng (java.util.Random. 11011)
        summary (token rng 6 1040 32)
        item (task/task {:query "q"
                         :status "pending"
                         :result nil
                         :created (task/format (task/now))})
        value (result/->Result summary [])
        done (task/finish item value)]
    (is (.isPresent (task/completed done))
        "Completed task timestamp was missing")))

(deftest the-task-serializes-query
  (let [rng (java.util.Random. 11013)
        query (token rng 6 12354 32)
        item (task/task {:query query
                         :status "pending"
                         :result nil
                         :created (task/format (task/now))})
        data (task/data item)]
    (is (= query (:query data))
        "Serialized query did not match original")))

(deftest the-task-deserializes-correctly
  (let [rng (java.util.Random. 11015)
        query (token rng 7 1040 32)
        data {:id (uuid rng)
              :query query
              :status "pending"
              :created (task/format (task/now))}
        item (task/task data)]
    (is (= query (task/query item))
        "Deserialized query did not match")))
