(ns research.api.xai.py-client.fetch
  (:require [clojure.string :as str]
            [libpython-clj2.python :as py]
            [research.api.xai.citations :as cite]))

(defn note
  "Return log record for XAI request."
  [model turns tokens include tools text]
  (let [list (mapv str tools)
        size (count text)
        lines (count (str/split-lines text))]
    {:kind "xai_request"
     :model model
     :max_turns turns
     :max_tokens tokens
     :tools list
     :include include
     :size size
     :lines lines}))

(defn- line
  "Return log line for prompt."
  [text]
  (str "xai prompt " text))

(defn fetch
  "Fetch response for prompt."
  [chat part core kit model turns tokens tags tools text]
  (let [_ (println (line text))
        message (py/call-attr chat "user" text)
        request (py/call-attr-kw
                 part
                 "create"
                 []
                 {:model model
                  :messages [message]
                  :tools tools
                  :tool_choice "auto"
                  :max_turns turns
                  :max_tokens tokens
                  :include tags})
        reply (py/call-attr request "sample")
        raw (or (py/as-jvm
                 (py/get-attr reply "content"))
                "")
        cells (mapv
               (fn [item] (cite/mark kit item))
               (cite/batch
                kit
                core
                (py/get-attr
                 reply
                 "inline_citations")))
        pairs (filter
               (fn [item] (seq (:url item)))
               cells)
        pairs (sort-by :end > pairs)
        body (if (str/includes? raw "[[")
               raw
               (reduce
                (fn [text item]
                  (let [end (:end item)
                        spot (cite/index kit text end)
                        mark (str
                              "[["
                              (:id item)
                              "]]("
                              (:url item)
                              ")")]
                    (if (<= spot (count text))
                      (str (subs text 0 spot)
                           mark
                           (subs text spot))
                      text)))
                raw
                pairs))
        meta (py/get-attr reply "debug_output")
        refs (mapv
              str
              (cite/batch
               kit
               core
               (py/get-attr reply "citations")))
        resp (try
               (mapv
                str
                (cite/batch
                 kit
                 core
                 (py/get-attr meta "responses")))
               (catch Exception _ []))
        chunk (try
                (mapv
                 str
                 (cite/batch
                  kit
                  core
                  (py/get-attr meta "chunks")))
                (catch Exception _ []))
        links (concat refs resp chunk)]
    {:body body
     :cells cells
     :links links}))
