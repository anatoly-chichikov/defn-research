(ns research.pdf.document.sources
  (:require [clojure.string :as str]
            [research.domain.result :as result]
            [research.domain.session :as sess]
            [research.pdf.document.data :as data]
            [research.pdf.document.text :as text]))

(defn catalog
  "Collect sources from session tasks."
  [item]
  (let [list (sess/tasks (:session item))]
    (reduce
     (fn [list task]
       (let [[_ sources] (data/resultmap item task)
             name (data/provider task)]
         (reduce
          (fn [list item]
            (let [link (text/trim (result/url item))]
              (if (str/blank? link)
                list
                (conj list {:source item
                            :provider name}))))
          list
          sources)))
     []
     list)))

(defn section
  "Render sources section."
  [list]
  (if (seq list)
    (let [rows (reduce
                (fn [text item]
                  (let [source (:source item)
                        name (:provider item)
                        link (text/trim (result/url source))
                        title (text/label source name)
                        note (if (= name "valyu")
                               (text/excerpt (result/excerpt source))
                               "")
                        link (text/escape link)
                        title (text/escape title)
                        note (text/escape note)
                        row (str "<li class=\"ref-item\">"
                                 "<a class=\"ref-link\" href=\""
                                 link
                                 "\" target=\"_blank\">"
                                 title
                                 "</a>"
                                 (if (str/blank? note)
                                   ""
                                   (str "<div class=\"source-excerpt\">"
                                        note
                                        "</div>"))
                                 "</li>")]
                    (str text row)))
                ""
                list)]
      (str "<section class=\"references\">"
           "<h2>Sources</h2><ol class=\"ref-list\">"
           rows
           "</ol></section>"))
    ""))

(defn emojify
  "Wrap emoji characters in spans."
  [text]
  (let [pattern (re-pattern
                 (str "([\\x{1F000}-\\x{1FAFF}"
                      "\\x{2600}-\\x{27BF}"
                      "\\x{2300}-\\x{23FF}]"
                      "\\x{FE0F}?)"))]
    (str/replace text pattern "<span class=\"emoji\">$1</span>")))
