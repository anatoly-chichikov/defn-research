(ns research.docker.dockerfile-test
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is]]
            [research.test.ids :as gen])
  (:import (java.nio.file Files Paths)))

(deftest the-dockerfile-uses-lein-entrypoint
  (let [rng (gen/ids 26001)
        text (gen/cyrillic rng 3)
        dockerfile (slurp "Dockerfile" :encoding "UTF-8")
        options ["ENTRYPOINT [\"lein\", \"run\"]"
                 "ENTRYPOINT [\"lein\", \"run\"]"]
        snippet (nth options
                     (mod (reduce + (map int text)) (count options)))]
    (is (str/includes? dockerfile snippet)
        "Docker entrypoint did not use lein run")))

(deftest the-compose-file-does-not-exist
  (let [rng (gen/ids 26003)
        text (gen/cyrillic rng 4)
        name (if (zero? (mod (reduce + (map int text)) 2))
               "docker-compose.yml"
               "docker-compose.yml")
        path (Paths/get name (make-array String 0))]
    (is (not (Files/exists path (make-array java.nio.file.LinkOption 0)))
        "Docker compose file was unexpectedly present")))
