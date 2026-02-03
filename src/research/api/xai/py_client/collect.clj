(ns research.api.xai.py-client.collect
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.walk :as walk]
            [research.api.xai.py-client.fetch :as fetch]))

(defn template
  "Return prompt template map."
  []
  (let [res (io/resource "prompts/xai-section.edn")
        text (if res (slurp res) "")
        data (if (str/blank? text) nil (edn/read-string text))]
    (if data
      data
      (throw (ex-info
              "Prompt template is missing"
              {:path "prompts/xai-section.edn"})))))

(defn fill
  "Return prompt map with replacements applied."
  [data slots]
  (walk/postwalk
   (fn [item]
     (if (and (string? item) (contains? slots item))
       (get slots item)
       item))
   data))

(defn collect
  "Collect response data for multi prompt."
  [chat part core kit model turns tokens tags tools head items top]
  (let [topic (str/trim (str (or top "")))
        line (some (fn [item]
                     (let [text (str/trim (str item))
                           lower (str/lower-case text)]
                       (when (or (str/starts-with?
                                  lower
                                  "язык ответа:")
                                 (str/starts-with?
                                  lower
                                  "response language:"))
                         text)))
                   head)
        text (str/trim (str (or line "")))
        lower (str/lower-case text)
        text (cond
               (str/starts-with? lower "язык ответа:")
               (subs text (count "Язык ответа:"))
               (str/starts-with? lower "response language:")
               (subs text (count "Response language:"))
               :else text)
        text (str/trim text)
        text (if (str/ends-with? text ".")
               (subs text 0 (dec (count text)))
               text)
        language (if (str/blank? text) "unspecified" text)
        temp (template)
        raw items
        list (loop [list [] items raw]
               (if (seq items)
                 (let [item (first items)
                       text (str/trim (str (or (:text item) "")))
                       depth (or (:depth item) 1)
                       depth (max 1 (min depth 3))
                       list (if (str/blank? text)
                              list
                              (conj list {:depth depth
                                          :text text}))]
                   (recur list (rest items)))
                 list))
        size (count list)
        items (loop [idx 0 path [] items []]
                (if (< idx size)
                  (let [item (nth list idx)
                        depth (:depth item)
                        depth (if (> depth (inc (count path)))
                                (inc (count path))
                                depth)
                        base (subvec (vec path) 0 (dec depth))
                        path (conj base (:text item))
                        next (if (< (inc idx) size) (nth list (inc idx)) nil)
                        step (if next (:depth next) 0)
                        leaf (not (> step depth))
                        name (last path)
                        head (vec (butlast path))
                        text (if leaf
                               (if (seq head)
                                 (str "Context: "
                                      (str/join " / " head)
                                      "\nFocus: "
                                      name)
                                 name)
                               "")
                        items (if leaf
                                (conj items {:name name
                                             :text text})
                                items)]
                    (recur (inc idx) path items))
                  items))]
    (loop [parts [] marks [] links [] prompts [] items items]
      (if (seq items)
        (let [item (first items)
              name (str/trim (str (or (:name item) "")))
              text (str/trim (str (or (:text item) name)))
              slots {"<<response_language>>" language
                     "<<topic>>" topic
                     "<<section_title>>" name
                     "<<section_details>>" text}
              data (fill temp slots)
              prompt (pr-str data)
              data (fetch/fetch
                    chat
                    part
                    core
                    kit
                    model
                    turns
                    tokens
                    tags
                    tools
                    prompt)
              body (:body data)
              cells (:cells data)
              refs (:links data)
              rows (str/split-lines body)
              rows (loop [rows rows lines [] seen false title "" flag false]
                     (if (seq rows)
                       (let [row (first rows)
                             trim (str/triml row)
                             blank (str/blank? row)
                             mark (str/starts-with? trim "#")
                             lead (if mark (re-find #"^#+" trim) "")
                             size (count (or lead ""))
                             text (if mark (str/triml (subs trim size)) "")
                             text (str/replace text #"^\d+[\.\)]\s+" "")
                             text (str/trim text)
                             start (and mark (not seen))
                             title (if start text title)
                             match (and seen
                                        (= (str/lower-case text)
                                           (str/lower-case title)))
                             mask (and flag blank)
                             line (if (and mark (not match))
                                    (str (if seen "### " "## ") text)
                                    "")
                             lines (cond
                                     mask lines
                                     (and mark (not match)) (conj lines line)
                                     (and mark match) lines
                                     :else (conj lines row))
                             flag (cond
                                    mask false
                                    (and mark match) true
                                    :else false)
                             seen (or seen mark)]
                         (recur (rest rows) lines seen title flag))
                       lines))
              body (str/trim (str/join "\n" rows))
              links (concat links refs)
              parts (conj parts body)
              marks (into marks cells)
              prompts (conj prompts prompt)]
          (recur parts marks links prompts (rest items)))
        {:parts parts
         :marks marks
         :links links
         :prompts prompts}))))
