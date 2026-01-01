(ns research.docker.dockerfile-test
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is]])
  (:import (java.nio.file Files Paths)))

(defn token
  "Return deterministic token string."
  [rng size base span]
  (let [build (StringBuilder.)]
    (dotimes [_ size]
      (let [pick (.nextInt rng span)
            code (+ base pick)]
        (.append build (char code))))
    (.toString build)))

(deftest the-dockerfile-uses-lein-entrypoint
  (let [rng (java.util.Random. 26001)
        text (token rng 3 1040 32)
        dockerfile (slurp "Dockerfile" :encoding "UTF-8")
        options ["ENTRYPOINT [\"lein\", \"run\"]"
                 "ENTRYPOINT [\"lein\", \"run\"]"]
        snippet (nth options
                     (mod (reduce + (map int text)) (count options)))]
    (is (str/includes? dockerfile snippet)
        "Docker entrypoint did not use lein run")))

(deftest the-compose-file-does-not-exist
  (let [rng (java.util.Random. 26003)
        text (token rng 4 1040 32)
        name (if (zero? (mod (reduce + (map int text)) 2))
               "docker-compose.yml"
               "docker-compose.yml")
        path (Paths/get name (make-array String 0))]
    (is (not (Files/exists path (make-array java.nio.file.LinkOption 0)))
        "Docker compose file was unexpectedly present")))
