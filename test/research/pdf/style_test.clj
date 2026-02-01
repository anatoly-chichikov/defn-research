(ns research.pdf.style-test
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is]]
            [research.pdf.palette :as palette]
            [research.pdf.style :as style]
            [research.test.ids :as gen]))

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
  (let [rng (gen/ids 20001)
        value (gen/cyrillic rng 6)
        css (style/css (style/style (pal value)))]
    (is (str/includes? css ".synthesis img")
        "Image constraints were not included in stylesheet")))

(deftest the-style-h1-omits-underline
  (let [rng (gen/ids 20003)
        value (gen/cyrillic rng 6)
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
  (let [rng (gen/ids 20005)
        value (gen/cyrillic rng 6)
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

(deftest ^{:doc "Ensures h4 uses link color."}
  the-style-h4-uses-link-tone
  (let [rng (gen/ids 20006)
        value (gen/cyrillic rng 6)
        css (style/css (style/style (pal value)))
        snippet (str/join
                 "\n"
                 ["h4 {"
                  "  font-size: 1rem;"
                  "  font-weight: 400;"
                  "  color: var(--link);"
                  "  line-height: 1.3;"
                  "  margin: 1.2em 0 0.25em;"
                  "}"])]
    (is (str/includes? css snippet) "H4 color did not match link tone")))

(deftest the-style-h2-draws-accent-bar
  (let [rng (gen/ids 20007)
        value (gen/cyrillic rng 6)
        css (style/css (style/style (pal value)))
        snippet (str/join
                 "\n"
                 [".toc-back {"
                  "  position: absolute;"
                  "  left: -1.4rem;"
                  "  top: 0;"
                  "  bottom: 0;"
                  "  width: 4px;"
                  "  display: block;"
                  "  background: linear-gradient(to bottom, var(--accent) 0%,"
                  " var(--accent) 82%, transparent 100%);"
                  "  border-radius: 0 0 4px 0;"
                  "  text-decoration: none;"
                  "}"])]
    (is (str/includes? css snippet) "Heading accent bar was missing")))

(deftest the-style-synthesis-omits-left-border
  (let [rng (gen/ids 20009)
        value (gen/cyrillic rng 6)
        css (style/css (style/style (pal value)))]
    (is (str/includes? css "border-left: none;")
        "Synthesis left border was present")))

(deftest the-style-hr-hides-divider-line
  (let [rng (gen/ids 20011)
        value (gen/cyrillic rng 6)
        css (style/css (style/style (pal value)))]
    (is (str/includes? css "border-top: 0;")
        "Horizontal rule line was visible")))

(deftest the-style-brief-query-uses-quote-colors
  (let [rng (gen/ids 20013)
        value (gen/cyrillic rng 6)
        css (style/css (style/style (pal value)))]
    (is (str/includes? css "background: var(--quote-bg);")
        "Brief query background was not quote color")))

(deftest the-style-brief-query-uses-link-tone
  (let [rng (gen/ids 20015)
        value (gen/cyrillic rng 6)
        css (style/css (style/style (pal value)))]
    (is (str/includes? css "border-left: 3px solid var(--link);")
        "Brief query border did not use link color")))

(deftest the-style-blockquote-uses-link-tone
  (let [rng (gen/ids 20017)
        value (gen/cyrillic rng 6)
        css (style/css (style/style (pal value)))]
    (is (str/includes? css "border-left: 3px solid var(--link);")
        "Blockquote border did not use link color")))

(deftest the-style-serif-font-includes-japanese-fallback
  (let [rng (gen/ids 20019)
        value (gen/cyrillic rng 6)
        css (style/css (style/style (pal value)))]
    (is (str/includes? css "Noto Serif JP")
        "Japanese fallback font was missing")))

(deftest the-style-includes-emoji-fallbacks
  (let [rng (gen/ids 20021)
        value (gen/cyrillic rng 6)
        css (style/css (style/style (pal value)))]
    (is (str/includes? css ".emoji")
        "Emoji class was missing from stylesheet")))

(deftest ^{:doc "Ensures subtitle links match text color."}
  the-style-uses-text-tone-for-subtitle-links
  (let [rng (gen/ids 20025)
        value (gen/cyrillic rng 6)
        css (style/css (style/style (pal value)))
        rule (str/join "\n" [".subtitle a {" "  color: inherit;"
                             "  text-decoration: none;"])]
    (is (str/includes? css rule) "Subtitle link style was not applied")))

(deftest ^{:doc "Ensures footer links match text color."}
  the-style-uses-text-tone-for-footer-links
  (let [rng (gen/ids 20027)
        value (gen/cyrillic rng 6)
        css (style/css (style/style (pal value)))
        rule (str/join "\n" [".page-footer a {" "  color: inherit;"
                             "  text-decoration: none;"])]
    (is (str/includes? css rule) "Footer link style was not applied")))

(deftest ^{:doc "Ensures signature mark uses text font."}
  the-style-uses-text-font-for-signature-mark
  (let [rng (gen/ids 20029)
        value (gen/cyrillic rng 6)
        css (style/css (style/style (pal value)))
        rule (str/join
              "\n"
              [".signature-mark {"
               "  font-family: inherit;"
               "}"])]
    (is (str/includes? css rule) "Signature mark font was not inherited")))

(deftest ^{:doc "Ensure citation links are raised."}
  the-style-raises-citations
  (let [rng (gen/ids 20023)
        value (gen/cyrillic rng 6)
        css (style/css (style/style (pal value)))]
    (is (str/includes? css "vertical-align: super;")
        "Citation links were not raised")))
