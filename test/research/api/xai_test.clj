(ns research.api.xai-test
  (:require [clojure.java.shell :as shell]
            [clojure.string :as str]
            [clojure.test :refer [deftest is]]
            [jsonista.core :as json]
            [libpython-clj2.python :as py]
            [research.api.research :as research]
            [research.api.response :as response]
            [research.api.xai :as xai]
            [research.test.ids :as gen])
  (:import (java.nio.file Files)
           (java.nio.file.attribute FileAttribute)))

(deftest ^{:doc "Xai start stores query in cache."}
  the-xai-start-stores-query
  (let [rng (gen/ids 17001)
        root (Files/createTempDirectory (gen/ascii rng 8)
                                        (make-array FileAttribute 0))
        query (gen/cyrillic rng 6)
        model (gen/latin rng 6)
        mode (gen/greek rng 5)
        unit (reify xai/Bound
               (run [_ _ _] {}))
        item (xai/xai {:root root
                       :model model
                       :mode mode
                       :turns 2
                       :window 3
                       :tokens 4
                       :follow false
                       :section false
                       :domains [(gen/armenian rng 4)]
                       :unit unit})
        path (.resolve root "tmp_cache")
        path (.resolve path "xai")]
    (research/start item query "365")
    (let [file (with-open [stream (Files/newDirectoryStream path)]
                 (first (iterator-seq (.iterator stream))))
          data (json/read-value
                (.toFile file)
                (json/object-mapper {:decode-key-fn keyword}))]
      (is (= query (:query data)) "Query was not stored"))))

(deftest ^{:doc "Xai start uses processor for window."}
  the-xai-start-uses-window
  (let [rng (gen/ids 17002)
        root (Files/createTempDirectory (gen/ascii rng 8)
                                        (make-array FileAttribute 0))
        query (gen/cyrillic rng 5)
        unit (reify xai/Bound
               (run [_ _ _] {}))
        item (xai/xai {:root root
                       :model (gen/latin rng 6)
                       :mode (gen/greek rng 4)
                       :turns 2
                       :window 365
                       :tokens 4
                       :follow false
                       :section false
                       :domains [(gen/armenian rng 4)]
                       :unit unit})
        path (.resolve root "tmp_cache")
        path (.resolve path "xai")]
    (research/start item query "90")
    (let [file (with-open [stream (Files/newDirectoryStream path)]
                 (first (iterator-seq (.iterator stream))))
          data (json/read-value
                (.toFile file)
                (json/object-mapper {:decode-key-fn keyword}))
          pack (:config data)
          days (:window pack)]
      (is (= 90 days) "Window did not match processor"))))

(deftest ^{:doc "Xai start rejects invalid window."}
  the-xai-start-rejects-window
  (let [rng (gen/ids 17004)
        root (Files/createTempDirectory (gen/ascii rng 8)
                                        (make-array FileAttribute 0))
        query (gen/cyrillic rng 5)
        bad (gen/hiragana rng 4)
        unit (reify xai/Bound
               (run [_ _ _] {}))
        item (xai/xai {:root root
                       :model (gen/latin rng 6)
                       :mode (gen/greek rng 4)
                       :turns 2
                       :window 365
                       :tokens 4
                       :follow false
                       :section false
                       :domains [(gen/armenian rng 4)]
                       :unit unit})
        raised (atom false)]
    (try
      (research/start item query bad)
      (catch Exception _ (reset! raised true)))
    (is @raised "Invalid window did not raise")))

(deftest ^{:doc "Xai finish returns markdown output."}
  the-xai-finish-returns-markdown
  (let [rng (gen/ids 17003)
        root (Files/createTempDirectory (gen/ascii rng 8)
                                        (make-array FileAttribute 0))
        query (gen/cyrillic rng 7)
        model (gen/latin rng 7)
        text (gen/hebrew rng 8)
        code (gen/ascii rng 10)
        unit (reify xai/Bound
               (run [_ _ _]
                 {:run {:run_id code
                        :status "completed"}
                  :output {:content text
                           :basis []}}))
        item (xai/xai {:root root
                       :model model
                       :mode (gen/greek rng 4)
                       :turns 5
                       :window 6
                       :tokens 7
                       :follow true
                       :section true
                       :domains [(gen/arabic rng 4)]
                       :unit unit})
        run (research/start item query "365")
        result (research/finish item run)]
    (is (= text (response/text result)) "Markdown did not match output")))

(deftest ^{:doc "Xai note stringifies tools."}
  the-xai-note-stringifies-tools
  (let [rng (gen/ids 17005)
        model (gen/latin rng 6)
        tool (gen/cyrillic rng 5)
        text (gen/greek rng 7)
        turn (inc (.nextInt rng 4))
        token (+ 1 (.nextInt rng 1000))
        items [(gen/armenian rng 4) (gen/hebrew rng 4)]
        node (reify Object
               (toString [_] tool)
               (hashCode [_] (throw (RuntimeException. tool))))
        note (#'xai/note model turn token items [node] text)]
    (is (= [tool] (:tools note)) "Tools were not stringified")))

(deftest ^{:doc "Xai batch handles python containers."}
  the-xai-batch-handles-containers
  (let [rng (gen/ids 17006)
        data (gen/cyrillic rng 5)
        probe (str "import os,sysconfig\n"
                   "libdir = sysconfig.get_config_var('LIBDIR') or ''\n"
                   "lib = sysconfig.get_config_var('LDLIBRARY') or ''\n"
                   "print(os.path.join(libdir, lib))\n")
        info (shell/sh "python3" "-c" probe)
        path (str/trim (:out info))
        code (str "box = ['" data "']\n")]
    (py/initialize! {:python-executable "python3"
                     :library-path path})
    (py/with-gil-stack-rc-context
      (py/run-simple-string code)
      (let [main (py/import-module "__main__")
            box (py/get-attr main "box")
            core (py/import-module "builtins")
            items (#'xai/batch core box)]
        (is (= [data] (mapv str items)) "Items did not handle container")))))

(deftest ^{:doc "Xai index maps unicode codepoints."}
  the-xai-index-maps-codepoints
  (let [rng (gen/ids 17007)
        head (gen/latin rng 1)
        tail (gen/cyrillic rng 1)
        face (String. (Character/toChars 128512))
        text (str head face tail)
        spot (#'xai/index text 2)]
    (is (= 3 spot) "Index did not map codepoints")))
