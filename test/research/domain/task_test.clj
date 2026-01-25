(ns research.domain.task-test
  (:require [clojure.test :refer [deftest is]]
            [research.domain.result :as result]
            [research.domain.task :as task]
            [research.test.ids :as gen]))

(deftest the-task-generates-unique-id
  (let [rng (gen/ids 11001)
        day (inc (.nextInt rng 8))
        hour (inc (.nextInt rng 8))
        time (str "2026-01-0" day "T0" hour ":00:00")
        query (gen/cyrillic rng 6)
        status (gen/cyrillic rng 6)
        language (gen/cyrillic rng 5)
        service (gen/cyrillic rng 4)
        summary (gen/cyrillic rng 6)
        value (result/->ResearchReport summary [])
        item (task/task {:query query
                         :status status
                         :language language
                         :service service
                         :result (result/data value)
                         :created time})]
    (is (= 36 (count (task/id item)))
        "Task identifier length is incorrect")))

(deftest the-task-returns-provided-query
  (let [rng (gen/ids 11003)
        day (inc (.nextInt rng 8))
        hour (inc (.nextInt rng 8))
        time (str "2026-01-0" day "T0" hour ":00:00")
        query (gen/hiragana rng 7)
        status (gen/cyrillic rng 6)
        language (gen/cyrillic rng 5)
        service (gen/cyrillic rng 4)
        summary (gen/cyrillic rng 6)
        value (result/->ResearchReport summary [])
        item (task/task {:id (gen/uuid rng)
                         :query query
                         :status status
                         :language language
                         :service service
                         :result (result/data value)
                         :created time})]
    (is (= query (task/query item))
        "Task query did not match provided value")))

(deftest the-task-returns-provided-status
  (let [rng (gen/ids 11005)
        day (inc (.nextInt rng 8))
        hour (inc (.nextInt rng 8))
        time (str "2026-01-0" day "T0" hour ":00:00")
        status (gen/greek rng 6)
        query (gen/cyrillic rng 6)
        language (gen/cyrillic rng 5)
        service (gen/cyrillic rng 4)
        summary (gen/cyrillic rng 6)
        value (result/->ResearchReport summary [])
        item (task/task {:id (gen/uuid rng)
                         :query query
                         :status status
                         :language language
                         :service service
                         :result (result/data value)
                         :created time})]
    (is (= status (task/status item))
        "Task status did not match provided value")))

(deftest the-task-complete-returns-new-task
  (let [rng (gen/ids 11007)
        day (inc (.nextInt rng 8))
        hour (inc (.nextInt rng 8))
        time (str "2026-01-0" day "T0" hour ":00:00")
        query (gen/cyrillic rng 6)
        status (gen/cyrillic rng 6)
        language (gen/cyrillic rng 5)
        service (gen/cyrillic rng 4)
        summary (gen/cyrillic rng 5)
        value (result/->ResearchReport summary [])
        item (task/task {:id (gen/uuid rng)
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
  (let [rng (gen/ids 11009)
        day (inc (.nextInt rng 8))
        hour (inc (.nextInt rng 8))
        time (str "2026-01-0" day "T0" hour ":00:00")
        query (gen/cyrillic rng 6)
        status (gen/cyrillic rng 6)
        language (gen/cyrillic rng 5)
        service (gen/cyrillic rng 4)
        summary (gen/cyrillic rng 6)
        value (result/->ResearchReport summary [])
        item (task/task {:id (gen/uuid rng)
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
  (let [rng (gen/ids 11011)
        day (inc (.nextInt rng 8))
        hour (inc (.nextInt rng 8))
        time (str "2026-01-0" day "T0" hour ":00:00")
        query (gen/cyrillic rng 6)
        status (gen/cyrillic rng 6)
        language (gen/cyrillic rng 5)
        service (gen/cyrillic rng 4)
        summary (gen/cyrillic rng 6)
        value (result/->ResearchReport summary [])
        item (task/task {:id (gen/uuid rng)
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
  (let [rng (gen/ids 11013)
        day (inc (.nextInt rng 8))
        hour (inc (.nextInt rng 8))
        time (str "2026-01-0" day "T0" hour ":00:00")
        query (gen/hiragana rng 6)
        status (gen/cyrillic rng 6)
        processor (gen/greek rng 6)
        language (gen/cyrillic rng 5)
        service (gen/cyrillic rng 4)
        summary (gen/cyrillic rng 6)
        value (result/->ResearchReport summary [])
        item (task/task {:id (gen/uuid rng)
                         :query query
                         :status status
                         :language language
                         :service service
                         :processor processor
                         :result (result/data value)
                         :created time})
        data (task/data item)]
    (is (and (contains? data :brief)
             (contains? (:brief data) :text)
             (contains? (:brief data) :topic)
             (contains? (:brief data) :items)
             (= processor (:processor data))
             (not (contains? data :query))
             (not (contains? data :result)))
        (str
         "Serialized task did not include brief or still included "
         "query or result"))))

(deftest the-task-deserializes-correctly
  (let [rng (gen/ids 11015)
        day (inc (.nextInt rng 8))
        hour (inc (.nextInt rng 8))
        time (str "2026-01-0" day "T0" hour ":00:00")
        query (gen/cyrillic rng 7)
        status (gen/greek rng 6)
        language (gen/cyrillic rng 5)
        service (gen/cyrillic rng 4)
        summary (gen/cyrillic rng 6)
        value (result/->ResearchReport summary [])
        data {:id (gen/uuid rng)
              :query query
              :status status
              :language language
              :service service
              :result (result/data value)
              :created time}
        item (task/task data)]
    (is (= query (task/query item))
        "Deserialized query did not match")))
