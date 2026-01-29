(ns research.pdf.document.tasks
  (:require [clojure.string :as str]
            [markdown.core :as md]
            [research.domain.session :as sess]
            [research.pdf.document.citations :as cite]
            [research.pdf.document.data :as data]
            [research.pdf.document.sources :as sources]
            [research.pdf.document.text :as text]))

(defn taskhtml
  "Render task section HTML."
  [item task]
  (let [[text sources] (data/resultmap item task)
        text (text/clean text)
        text (text/underscorify text)
        text (cite/stars text)
        text (sources/emojify text)
        [text urls mark] (cite/citations text sources)
        text (text/tablecite text)
        text (text/tablelead text)
        text (text/tablepipe text)
        text (text/tablerows text)
        text (cite/strip text)
        text (text/nested text)
        text (text/normalize text)
        text (text/rule text)
        html (if (str/blank? text) "" (md/md-to-html-string text))
        html (cite/tables html)
        html (cite/codeindent html)
        html (text/paragraphs html)
        html (cite/backslash html)
        html (reduce-kv
              (fn [note token link]
                (str/replace note token link))
              html
              mark)
        body (if (str/blank? html)
               ""
               (str "<div class=\"synthesis\">" html "</div>"))]
    [(str "<section>" body "<div class=\"divider\"></div></section>") urls]))

(defn tasks
  "Render all tasks into HTML sections."
  [item]
  (let [list (sess/tasks (:session item))
        items (reduce
               (fn [note task]
                 (let [[html urls] (taskhtml item task)
                       list (concat (second note) urls)]
                   [(str (first note) html) (vec (distinct list))]))
               ["" []]
               list)]
    items))
