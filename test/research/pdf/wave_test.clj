(ns research.pdf.wave-test
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is]]
            [research.pdf.palette :as palette]
            [research.pdf.wave :as wave]))

(deftest the-wave-render-returns-svg
  (let [pal (palette/palette)
        item (wave/wave pal)
        html (wave/render item)]
    (is (str/includes? html "<svg")
        "Rendered wave did not contain svg tag")))

(deftest the-wave-render-contains-path
  (let [pal (palette/palette)
        item (wave/wave pal)
        html (wave/render item)]
    (is (str/includes? html "<path")
        "Rendered wave did not contain path tag")))

(deftest the-wave-render-contains-palette-color
  (let [pal (palette/palette)
        item (wave/wave pal)
        html (wave/render item)]
    (is (str/includes? html (palette/link pal))
        "Rendered wave did not contain palette link color")))

(deftest the-wave-render-contains-gradient
  (let [pal (palette/palette)
        item (wave/wave pal)
        html (wave/render item)]
    (is (str/includes? html "linearGradient")
        "Rendered wave did not contain gradient")))

(deftest the-footer-render-returns-svg
  (let [pal (palette/palette)
        item (wave/footer pal)
        html (wave/render item)]
    (is (str/includes? html "<svg")
        "Rendered footer did not contain svg tag")))

(deftest the-footer-render-contains-path
  (let [pal (palette/palette)
        item (wave/footer pal)
        html (wave/render item)]
    (is (str/includes? html "<path")
        "Rendered footer did not contain path tag")))
