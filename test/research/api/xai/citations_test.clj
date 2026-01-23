(ns research.api.xai.citations-test
  (:require [clojure.java.shell :as shell]
            [clojure.string :as str]
            [clojure.test :refer [deftest is]]
            [libpython-clj2.python :as py]
            [research.api.xai.citations :as cite]
            [research.test.ids :as gen]))

(deftest ^{:doc "Citations batch handles python containers."}
  the-citations-batch-handles-containers
  (let [rng (gen/ids 18303)
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
            item (cite/make)
            items (cite/batch item core box)]
        (is (= [data] (mapv str items)) "items did not handle container")))))

(deftest ^{:doc "Citations index maps unicode codepoints."}
  the-citations-index-maps-codepoints
  (let [rng (gen/ids 18305)
        head (gen/latin rng 1)
        tail (gen/cyrillic rng 1)
        face (String. (Character/toChars 128512))
        text (str head face tail)
        item (cite/make)
        spot (cite/index item text 2)]
    (is (= 3 spot) "index did not map codepoints")))
