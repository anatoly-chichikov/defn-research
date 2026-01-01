(ns research.pdf.palette-test
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is]]
            [research.pdf.palette :as palette]))

(defn token
  "Return deterministic token string."
  [rng size base span]
  (let [build (StringBuilder.)]
    (dotimes [_ size]
      (let [pick (.nextInt rng span)
            code (+ base pick)]
        (.append build (char code))))
    (.toString build)))

(deftest the-palette-bg-returns-hex
  (let [rng (java.util.Random. 19001)
        text (token rng 3 1040 32)
        pal (palette/palette)
        value (if (zero? (mod (count text) 2))
                (palette/bg pal)
                (palette/bg pal))]
    (is (str/starts-with? value "#") "Bg color did not start with hash")))

(deftest the-palette-bg-returns-seven-characters
  (let [rng (java.util.Random. 19003)
        text (token rng 3 1040 32)
        pal (palette/palette)
        value (if (zero? (mod (count text) 2))
                (palette/bg pal)
                (palette/bg pal))]
    (is (= 7 (count value)) "Bg color was not seven characters")))

(deftest the-palette-text-returns-hex
  (let [rng (java.util.Random. 19005)
        text (token rng 3 1040 32)
        pal (palette/palette)
        value (if (zero? (mod (count text) 2))
                (palette/text pal)
                (palette/text pal))]
    (is (str/starts-with? value "#") "Text color did not start with hash")))

(deftest the-palette-heading-returns-hex
  (let [rng (java.util.Random. 19007)
        text (token rng 3 1040 32)
        pal (palette/palette)
        value (if (zero? (mod (count text) 2))
                (palette/heading pal)
                (palette/heading pal))]
    (is (str/starts-with? value "#")
        "Heading color did not start with hash")))

(deftest the-palette-link-returns-hex
  (let [rng (java.util.Random. 19009)
        text (token rng 3 1040 32)
        pal (palette/palette)
        value (if (zero? (mod (count text) 2))
                (palette/link pal)
                (palette/link pal))]
    (is (str/starts-with? value "#") "Link color did not start with hash")))

(deftest the-palette-muted-returns-hex
  (let [rng (java.util.Random. 19011)
        text (token rng 3 1040 32)
        pal (palette/palette)
        value (if (zero? (mod (count text) 2))
                (palette/muted pal)
                (palette/muted pal))]
    (is (str/starts-with? value "#") "Muted color did not start with hash")))

(deftest the-palette-quote-returns-hex
  (let [rng (java.util.Random. 19013)
        text (token rng 3 1040 32)
        pal (palette/palette)
        value (if (zero? (mod (count text) 2))
                (palette/quote pal)
                (palette/quote pal))]
    (is (str/starts-with? value "#") "Quote color did not start with hash")))

(deftest the-palette-accent-returns-hex
  (let [rng (java.util.Random. 19015)
        text (token rng 3 1040 32)
        pal (palette/palette)
        value (if (zero? (mod (count text) 2))
                (palette/accent pal)
                (palette/accent pal))]
    (is (str/starts-with? value "#")
        "Accent color did not start with hash")))

(deftest the-palette-codebg-returns-hex
  (let [rng (java.util.Random. 19017)
        text (token rng 3 1040 32)
        pal (palette/palette)
        value (if (zero? (mod (count text) 2))
                (palette/codebg pal)
                (palette/codebg pal))]
    (is (str/starts-with? value "#") "Codebg color did not start with hash")))

(deftest the-palette-codeinline-returns-hex
  (let [rng (java.util.Random. 19019)
        text (token rng 3 1040 32)
        pal (palette/palette)
        value (if (zero? (mod (count text) 2))
                (palette/codeinline pal)
                (palette/codeinline pal))]
    (is (str/starts-with? value "#")
        "Codeinline color did not start with hash")))

(deftest the-palette-border-returns-hex
  (let [rng (java.util.Random. 19021)
        text (token rng 3 1040 32)
        pal (palette/palette)
        value (if (zero? (mod (count text) 2))
                (palette/border pal)
                (palette/border pal))]
    (is (str/starts-with? value "#") "Border color did not start with hash")))
