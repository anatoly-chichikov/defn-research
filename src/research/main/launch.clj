(ns research.main.launch
  (:require [research.main.execute :as execute]
            [research.main.seed :as seed]))

(defn launch
  "Create session and run research."
  [root data out topic query processor language provider env]
  (let [processor (if (= provider "xai") "year" processor)
        id (seed/seed data topic)
        mode (cond
               (= processor "lite")
               (throw (ex-info
                       "Run failed because processor lite is not supported"
                       {:processor processor}))
               (and (= provider "valyu")
                    (not (or (= processor "fast")
                             (= processor "standard")
                             (= processor "heavy"))))
               (throw (ex-info
                       (str "Run failed because processor is not supported"
                            " for valyu")
                       {:processor processor}))
               (or (= processor "fast")
                   (= processor "standard")
                   (= processor "heavy"))
               processor
               :else "standard")
        pairs (if (= provider "all")
                [["parallel" processor]
                 ["valyu" mode]]
                [[provider
                  (if (= provider "valyu") mode processor)]])]
    (doseq [pair pairs]
      (let [name (first pair)
            proc (second pair)]
        (execute/execute root data out id query proc language name env)))))
