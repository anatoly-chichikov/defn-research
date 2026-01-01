(ns research.pdf.style
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [research.pdf.palette :as palette]))

(defprotocol Styled
  "Object with CSS style."
  (css [item] "Return CSS string."))

(defn fill
  "Replace palette tokens in CSS."
  [text item]
  (-> text
      (str/replace "__BG__" (palette/bg item))
      (str/replace "__TEXT__" (palette/text item))
      (str/replace "__HEADING__" (palette/heading item))
      (str/replace "__LINK__" (palette/link item))
      (str/replace "__MUTED__" (palette/muted item))
      (str/replace "__QUOTE__" (palette/quote item))
      (str/replace "__ACCENT__" (palette/accent item))
      (str/replace "__CODEBG__" (palette/codebg item))
      (str/replace "__CODEINLINE__" (palette/codeinline item))
      (str/replace "__BORDER__" (palette/border item))))

(defrecord Style [palette]
  Styled
  (css [_]
    (let [path (io/resource "style.css")
          text (if path
                 (slurp path)
                 (throw (ex-info "Style resource missing" {})))]
      (fill text palette))))

(defn style
  "Create style from palette."
  [item]
  (->Style item))
