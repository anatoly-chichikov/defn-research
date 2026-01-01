(defproject research "0.1.0-SNAPSHOT"
  :description "Deep research tool"
  :license {:name "Apache-2.0"}
  :dependencies [[org.clojure/clojure "1.11.3"]
                 [markdown-clj/markdown-clj "1.12.4"]
                 [http-kit/http-kit "2.8.1"]
                 [metosin/jsonista "0.3.13"]
                 [org.jsoup/jsoup "1.18.3"]
                 [com.twelvemonkeys.imageio/imageio-jpeg "3.12.0"]
                 [com.google.genai/google-genai "1.32.0"]
                 [org.clojure/tools.cli "1.2.245"]
                 [org.slf4j/slf4j-nop "2.0.16"]]
  :main research.main
  :resource-paths ["resources"]
  :profiles {:dev {:dependencies [[clj-kondo/clj-kondo "2025.10.23"]
                                  [cljfmt/cljfmt "0.9.2"]]}
             :test {:dependencies [[lambdaisland/kaocha "1.91.1392"]
                                   [nubank/matcher-combinators "3.9.2"]]}}
  :aliases {"test" ["with-profile" "+test" "run" "-m" "kaocha.runner"]
            "lint" ["with-profile" "+dev" "run" "-m" "clj-kondo.main" "--lint" "src" "test"]
            "fmt" ["with-profile" "+dev" "run" "-m" "cljfmt.main" "fix" "src" "test"]
            "fmt:check" ["with-profile" "+dev" "run" "-m" "cljfmt.main" "check" "src" "test"]})
