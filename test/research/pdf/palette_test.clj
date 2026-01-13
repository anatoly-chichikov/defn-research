(ns research.pdf.palette-test
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is]]
            [research.pdf.palette :as palette]
            [research.test.ids :as gen]))

(deftest the-palette-bg-returns-hex
  (let [rng (gen/ids 19001)
        text (gen/cyrillic rng 3)
        pal (palette/palette)
        value (if (zero? (mod (count text) 2))
                (palette/bg pal)
                (palette/bg pal))]
    (is (str/starts-with? value "#") "Bg color did not start with hash")))

(deftest the-palette-bg-returns-seven-characters
  (let [rng (gen/ids 19003)
        text (gen/cyrillic rng 3)
        pal (palette/palette)
        value (if (zero? (mod (count text) 2))
                (palette/bg pal)
                (palette/bg pal))]
    (is (= 7 (count value)) "Bg color was not seven characters")))

(deftest the-palette-text-returns-hex
  (let [rng (gen/ids 19005)
        text (gen/cyrillic rng 3)
        pal (palette/palette)
        value (if (zero? (mod (count text) 2))
                (palette/text pal)
                (palette/text pal))]
    (is (str/starts-with? value "#") "Text color did not start with hash")))

(deftest the-palette-heading-returns-hex
  (let [rng (gen/ids 19007)
        text (gen/cyrillic rng 3)
        pal (palette/palette)
        value (if (zero? (mod (count text) 2))
                (palette/heading pal)
                (palette/heading pal))]
    (is (str/starts-with? value "#")
        "Heading color did not start with hash")))

(deftest the-palette-link-returns-hex
  (let [rng (gen/ids 19009)
        text (gen/cyrillic rng 3)
        pal (palette/palette)
        value (if (zero? (mod (count text) 2))
                (palette/link pal)
                (palette/link pal))]
    (is (str/starts-with? value "#") "Link color did not start with hash")))

(deftest the-palette-muted-returns-hex
  (let [rng (gen/ids 19011)
        text (gen/cyrillic rng 3)
        pal (palette/palette)
        value (if (zero? (mod (count text) 2))
                (palette/muted pal)
                (palette/muted pal))]
    (is (str/starts-with? value "#") "Muted color did not start with hash")))

(deftest the-palette-quote-returns-hex
  (let [rng (gen/ids 19013)
        text (gen/cyrillic rng 3)
        pal (palette/palette)
        value (if (zero? (mod (count text) 2))
                (palette/quote pal)
                (palette/quote pal))]
    (is (str/starts-with? value "#") "Quote color did not start with hash")))

(deftest the-palette-accent-returns-hex
  (let [rng (gen/ids 19015)
        text (gen/cyrillic rng 3)
        pal (palette/palette)
        value (if (zero? (mod (count text) 2))
                (palette/accent pal)
                (palette/accent pal))]
    (is (str/starts-with? value "#")
        "Accent color did not start with hash")))

(deftest the-palette-codebg-returns-hex
  (let [rng (gen/ids 19017)
        text (gen/cyrillic rng 3)
        pal (palette/palette)
        value (if (zero? (mod (count text) 2))
                (palette/codebg pal)
                (palette/codebg pal))]
    (is (str/starts-with? value "#") "Codebg color did not start with hash")))

(deftest the-palette-codeinline-returns-hex
  (let [rng (gen/ids 19019)
        text (gen/cyrillic rng 3)
        pal (palette/palette)
        value (if (zero? (mod (count text) 2))
                (palette/codeinline pal)
                (palette/codeinline pal))]
    (is (str/starts-with? value "#")
        "Codeinline color did not start with hash")))

(deftest the-palette-border-returns-hex
  (let [rng (gen/ids 19021)
        text (gen/cyrillic rng 3)
        pal (palette/palette)
        value (if (zero? (mod (count text) 2))
                (palette/border pal)
                (palette/border pal))]
    (is (str/starts-with? value "#") "Border color did not start with hash")))
