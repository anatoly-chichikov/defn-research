(ns research.storage.organizer
  (:refer-clojure :exclude [name])
  (:require [clojure.string :as str]
            [jsonista.core :as json])
  (:import (java.nio.file Files LinkOption)
           (java.nio.file.attribute FileAttribute)
           (java.time.format DateTimeFormatter)
           (java.util Optional)))

(defprotocol Organized
  "Object that organizes output files."
  (name [item time text id] "Return folder name.")
  (folder [item name provider] "Return output folder path.")
  (response [item name provider data] "Save response JSON.")
  (cover [item name provider] "Return cover path.")
  (report [item name provider] "Return report path.")
  (html [item name provider] "Return html path.")
  (brief [item name provider text] "Save brief text.")
  (existing [item name provider] "Return existing cover path."))

(defn parse
  "Format LocalDateTime into date string."
  [time]
  (.format time DateTimeFormatter/ISO_LOCAL_DATE))

(defn translit
  "Transliterate Cyrillic into Latin."
  [text]
  (let [table {"а" "a"
               "б" "b"
               "в" "v"
               "г" "g"
               "д" "d"
               "е" "e"
               "ё" "yo"
               "ж" "zh"
               "з" "z"
               "и" "i"
               "й" "y"
               "к" "k"
               "л" "l"
               "м" "m"
               "н" "n"
               "о" "o"
               "п" "p"
               "р" "r"
               "с" "s"
               "т" "t"
               "у" "u"
               "ф" "f"
               "х" "h"
               "ц" "ts"
               "ч" "ch"
               "ш" "sh"
               "щ" "sch"
               "ъ" ""
               "ы" "y"
               "ь" ""
               "э" "e"
               "ю" "yu"
               "я" "ya"}]
    (apply str
           (map
            (fn [item]
              (let [low (str/lower-case (str item))
                    hit (get table low)]
                (if hit
                  (if (= (str item) low) hit (str/upper-case hit))
                  (str item))))
            text))))

(defn slug
  "Convert text into safe slug."
  [text]
  (let [text (translit text)
        text (str/lower-case text)
        text (str/replace text #"[^a-z0-9\s-]" "")
        text (str/replace text #"[\s]+" "-")
        text (subs text 0 (min 40 (count text)))]
    (if (str/blank? text) "untitled" text)))

(defrecord Organizer [root]
  Organized
  (name [_ time text id]
    (str (parse time) "_" (slug text) "_" (subs id 0 8)))
  (folder [_ name provider]
    (let [path (.resolve root name)
          _ (Files/createDirectories path (make-array FileAttribute 0))]
      path))
  (response [item name provider data]
    (let [tag (slug provider)
          tag (if (str/blank? tag) "provider" tag)
          path (.resolve (folder item name provider)
                         (str "response-" tag ".json"))]
      (json/write-value (.toFile path) data)
      path))
  (cover [item name provider]
    (let [tag (slug provider)
          tag (if (str/blank? tag) "provider" tag)]
      (.resolve (folder item name provider) (str "cover-" tag ".jpg"))))
  (report [item name provider]
    (let [cut (str/last-index-of name "_")
          stem (if cut (subs name 0 cut) name)
          tag (slug provider)
          tag (if (str/blank? tag) "provider" tag)]
      (.resolve (folder item name provider) (str stem "-" tag ".pdf"))))
  (html [item name provider]
    (let [cut (str/last-index-of name "_")
          stem (if cut (subs name 0 cut) name)
          tag (slug provider)
          tag (if (str/blank? tag) "provider" tag)]
      (.resolve (folder item name provider) (str stem "-" tag ".html"))))
  (brief [item name provider text]
    (let [tag (slug provider)
          tag (if (str/blank? tag) "provider" tag)
          path (.resolve (folder item name provider) (str "brief-" tag ".md"))
          _ (spit (.toFile path) text :encoding "UTF-8")]
      path))
  (existing [item name provider]
    (let [tag (slug provider)
          tag (if (str/blank? tag) "provider" tag)
          base (.resolve root name)
          path (.resolve base (str "cover-" tag ".jpg"))]
      (if (Files/exists path (make-array LinkOption 0))
        (Optional/of path)
        (Optional/empty)))))

(defn organizer
  "Create organizer from root path."
  [root]
  (->Organizer root))
