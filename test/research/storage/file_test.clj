(ns research.storage.file-test
  (:require [clojure.test :refer [deftest is]]
            [research.storage.file :as file])
  (:import (java.nio.file Files Paths)
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

(deftest the-jsonfile-writes-and-reads-data
  (let [rng (java.util.Random. 22001)
        root (Files/createTempDirectory "json"
                                        (make-array FileAttribute 0))
        path (.resolve root (str "test-" (uuid rng) ".json"))
        item (file/file path)
        key (keyword (token rng 6 1040 32))]
    (file/write item {key "value"})
    (is (= "value" (get (file/read item) key))
        "Read data did not match written data")))

(deftest the-jsonfile-raises-on-missing-file
  (let [rng (java.util.Random. 22003)
        path (Paths/get (str "/nonexistent-" (uuid rng) ".json")
                        (make-array String 0))
        item (file/file path)
        raised (atom false)]
    (try (file/read item) (catch Exception _ (reset! raised true)))
    (is @raised "Reading missing file did not raise exception")))

(deftest the-jsonfile-exists-returns-false-for-missing
  (let [rng (java.util.Random. 22005)
        path (Paths/get (str "/nonexistent-" (uuid rng) ".json")
                        (make-array String 0))
        item (file/file path)]
    (is (not (file/exists item))
        "Exists returned true for missing file")))

(deftest the-jsonfile-exists-returns-true-for-existing
  (let [rng (java.util.Random. 22007)
        root (Files/createTempDirectory "json"
                                        (make-array FileAttribute 0))
        path (.resolve root (str "test-" (uuid rng) ".json"))
        item (file/file path)]
    (file/write item {:test true})
    (is (file/exists item) "Exists returned false for existing file")))

(deftest the-jsonfile-creates-parent-directories
  (let [rng (java.util.Random. 22009)
        root (Files/createTempDirectory "json"
                                        (make-array FileAttribute 0))
        nest (str "a-" (uuid rng) "/b-" (uuid rng))
        path (.resolve root (str nest "/test.json"))
        item (file/file path)]
    (file/write item {:nested true})
    (is (file/exists item) "File was not created in nested directory")))

(deftest the-jsonfile-handles-unicode
  (let [rng (java.util.Random. 22011)
        root (Files/createTempDirectory "json"
                                        (make-array FileAttribute 0))
        path (.resolve root (str "unicode-" (uuid rng) ".json"))
        item (file/file path)
        text (str "日本語テスト-" (uuid rng))]
    (file/write item {:text text})
    (is (= text (get (file/read item) :text))
        "Unicode content was corrupted")))
