(ns research.pdf.style-test
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is]]
            [research.pdf.palette :as palette]
            [research.pdf.style :as style]))

(defn token
  "Return deterministic token string."
  [rng size base span]
  (let [build (StringBuilder.)]
    (dotimes [_ size]
      (let [pick (.nextInt rng span)
            code (+ base pick)]
        (.append build (char code))))
    (.toString build)))

(defn pal
  "Return palette that yields value."
  [value]
  (reify palette/Colored
    (bg [_] value)
    (text [_] value)
    (heading [_] value)
    (link [_] value)
    (muted [_] value)
    (quote [_] value)
    (accent [_] value)
    (codebg [_] value)
    (codeinline [_] value)
    (border [_] value)))

(deftest the-style-includes-image-constraints
  (let [rng (java.util.Random. 20001)
        value (token rng 6 1040 32)
        css (style/css (style/style (pal value)))]
    (is (str/includes? css ".synthesis img")
        "Image constraints were not included in stylesheet")))

(deftest the-style-h1-omits-underline
  (let [rng (java.util.Random. 20003)
        value (token rng 6 1040 32)
        css (style/css (style/style (pal value)))
        snippet (str/join
                 "\n"
                 ["h1 {"
                  "  font-size: 2.05rem;"
                  "  font-weight: 700;"
                  "  color: var(--heading);"
                  "  line-height: 1.15;"
                  "  margin: 0 0 0.7em;"
                  "  border-bottom: 1px solid var(--border);"])]
    (is (not (str/includes? css snippet)) "H1 underline was present")))

(deftest the-style-h2-omits-underline
  (let [rng (java.util.Random. 20005)
        value (token rng 6 1040 32)
        css (style/css (style/style (pal value)))
        snippet (str/join
                 "\n"
                 ["h2 {"
                  "  font-size: 1.55rem;"
                  "  font-weight: 600;"
                  "  color: var(--heading);"
                  "  line-height: 1.2;"
                  "  margin: 2.2em 0 0.6em;"
                  "  position: relative;"
                  "  border-bottom: 1px solid var(--border);"])]
    (is (not (str/includes? css snippet)) "H2 underline was present")))

(deftest the-style-h2-draws-accent-bar
  (let [rng (java.util.Random. 20007)
        value (token rng 6 1040 32)
        css (style/css (style/style (pal value)))
        snippet (str/join
                 "\n"
                 ["h2::before {"
                  "  content: \"\";"
                  "  position: absolute;"
                  "  left: -1.4rem;"
                  "  top: 0;"
                  "  bottom: 0;"
                  "  width: 4px;"
                  "  background: linear-gradient(to bottom, var(--accent) 0%,"
                  " var(--accent) 82%, transparent 100%);"
                  "  border-radius: 0 0 4px 0;"
                  "}"])]
    (is (str/includes? css snippet) "Heading accent bar was missing")))

(deftest the-style-synthesis-omits-left-border
  (let [rng (java.util.Random. 20009)
        value (token rng 6 1040 32)
        css (style/css (style/style (pal value)))]
    (is (str/includes? css "border-left: none;")
        "Synthesis left border was present")))

(deftest the-style-hr-hides-divider-line
  (let [rng (java.util.Random. 20011)
        value (token rng 6 1040 32)
        css (style/css (style/style (pal value)))]
    (is (str/includes? css "border-top: 0;")
        "Horizontal rule line was visible")))

(deftest the-style-brief-query-uses-quote-colors
  (let [rng (java.util.Random. 20013)
        value (token rng 6 1040 32)
        css (style/css (style/style (pal value)))]
    (is (str/includes? css "background: var(--quote-bg);")
        "Brief query background was not quote color")))

(deftest the-style-brief-query-uses-link-tone
  (let [rng (java.util.Random. 20015)
        value (token rng 6 1040 32)
        css (style/css (style/style (pal value)))]
    (is (str/includes? css "border-left: 3px solid var(--link);")
        "Brief query border did not use link color")))

(deftest the-style-blockquote-uses-link-tone
  (let [rng (java.util.Random. 20017)
        value (token rng 6 1040 32)
        css (style/css (style/style (pal value)))]
    (is (str/includes? css "border-left: 3px solid var(--link);")
        "Blockquote border did not use link color")))

(deftest the-style-serif-font-includes-japanese-fallback
  (let [rng (java.util.Random. 20019)
        value (token rng 6 1040 32)
        css (style/css (style/style (pal value)))]
    (is (str/includes? css "Noto Serif JP")
        "Japanese fallback font was missing")))

(deftest the-style-includes-emoji-fallbacks
  "Confirm emoji-capable fonts are listed in the serif stack."
  (let [rng (java.util.Random. 20021)
        value (token rng 6 1040 32)
        css (style/css (style/style (pal value)))]
    (is (str/includes? css ".emoji")
        "Emoji class was missing from stylesheet")))
