(ns research.api.xai.brief
  (:require [clojure.string :as str]))

(defprotocol Briefed
  "Object that can parse research brief."
  (parts [item text] "Return brief parts."))

(defrecord Brief [mark]
  Briefed
  (parts [_ text]
    (let [lines (str/split-lines (str text))
          spot (first (keep-indexed
                       (fn [idx line]
                         (when (= mark (str/trim line)) idx))
                       lines))
          head (vec (if (some? spot) (take spot lines) lines))
          tail (if (some? spot) (drop (inc spot) lines) [])
          items (loop [list [] chunk tail]
                  (if (seq chunk)
                    (let [line (str/trim (first chunk))]
                      (if (str/blank? line)
                        (recur list (rest chunk))
                        (let [lead (first line)
                              mark (and lead (Character/isDigit ^char lead))
                              part (cond
                                     (and mark (str/includes? line "."))
                                     (str/trim
                                      (second (str/split line #"\." 2)))
                                     (and mark (str/includes? line ")"))
                                     (str/trim
                                      (second (str/split line #"\)" 2)))
                                     mark line
                                     :else nil)
                              list (if part
                                     (conj list part)
                                     (if (seq list)
                                       (conj (vec (butlast list))
                                             (str (last list) " " line))
                                       (conj list line)))]
                          (recur list (rest chunk)))))
                    list))
          top (reduce
               (fn [text line]
                 (if (str/blank? (str/trim line)) text (str/trim line)))
               ""
               head)]
      {:head head
       :items items
       :top top})))

(defn make
  "Return brief parser."
  []
  (->Brief "Research:"))
