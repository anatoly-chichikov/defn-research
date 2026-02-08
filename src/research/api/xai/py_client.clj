(ns research.api.xai.py-client
  (:require [clojure.string :as str]
            [libpython-clj2.python :as py]
            [research.api.xai.brief :as brief]
            [research.api.xai.citations :as cite]
            [research.api.xai.py-client.collect :as collect]
            [research.api.xai.py-client.fetch :as fetch])
  (:import (java.nio.file Files LinkOption)
           (java.util UUID)))

(defprotocol Bound
  "Object that can run XAI python runner."
  (run [item text pack] "Run XAI and return response map."))

(defn note
  "Return log record for XAI request."
  [model turns tokens include tools text]
  (fetch/note model turns tokens include tools text))

(defn- order
  "Return renumbered citations and ordered URLs."
  [text marks]
  (let [rule (re-pattern "\\[\\[(\\d+)\\]\\]\\((https?://[^)]+)\\)")
        hits (or (re-seq rule (str text)) [])
        data (reduce
              (fn [data item]
                (let [url (nth item 2)
                      seen (:seen data)
                      list (:list data)
                      map (:map data)]
                  (if (contains? seen url)
                    data
                    {:seen (conj seen url)
                     :list (conj list url)
                     :map (assoc map url (inc (count list)))})))
              {:seen #{}
               :list []
               :map {}}
              hits)
        map (:map data)
        text (if (seq map)
               (str/replace
                (str text)
                rule
                (fn [items]
                  (let [url (nth items 2)
                        num (get map url (second items))]
                    (str "[[" num "]](" url ")"))))
               (str text))
        name (reduce
              (fn [map item]
                (let [url (:url item)
                      text (str (or (:title item) ""))]
                  (if (or (str/blank? url)
                          (contains? map url)
                          (str/blank? text))
                    map
                    (assoc map url text))))
              {}
              marks)]
    {:text text
     :list (:list data)
     :name name}))

(defn binary
  "Return python executable path."
  [root exec]
  (let [file (.resolve root ".venv")
        file (.resolve file "bin")
        file (.resolve file "python")
        opts (make-array LinkOption 0)
        exec (or exec "")
        path (if (str/blank? exec)
               (if (Files/exists file opts)
                 (.toString file)
                 "python3")
               exec)]
    path))

(defn boot
  "Initialize python runtime."
  [path]
  (let [conf (if (str/blank? path) {} {:python-executable path})]
    (py/initialize! conf)
    (py/with-gil-stack-rc-context (py/import-module "xai_sdk"))
    :ok))

(defrecord Client [root data]
  Bound
  (run [_ text pack]
    (let [model (or (:model pack) "grok-4-1-fast")
          mode (or (:mode pack) "social_multi")
          turns (int (or (:turns pack) 2))
          window (int (or (:window pack) 365))
          tokens (int (or (:tokens pack) 3500))
          flag (if (contains? pack :follow) (:follow pack) true)
          domains (:domains pack)
          tags ["inline_citations"
                "web_search_call_output"
                "x_search_call_output"]
          brief (or (:brief data) (brief/make))
          kit (or (:cites data) (cite/make))
          info (brief/parts brief text)
          head (:head info)
          items (:items info)
          top (:top info)]
      (py/with-gil-stack-rc-context
        (let [sdk (py/import-module "xai_sdk")
              chat (py/import-module "xai_sdk.chat")
              tool (py/import-module "xai_sdk.tools")
              time (py/import-module "datetime")
              core (py/import-module "builtins")
              date (py/get-attr time "datetime")
              now (py/call-attr date "utcnow")
              span (py/call-attr-kw time "timedelta" [] {:days window})
              origin (py/call-attr now "__sub__" span)
              x (py/call-attr-kw tool "x_search" [] {:from_date origin})
              web (cond
                    (or (= mode "social_multi") (= mode "social"))
                    (py/call-attr-kw
                     tool
                     "web_search"
                     []
                     {:allowed_domains domains})
                    :else (py/call-attr tool "web_search"))
              tools (if (= mode "social") [web] [x web])
              client (py/call-attr sdk "Client")
              part (py/get-attr client "chat")]
          (try
            (let [state (and flag (seq items))
                  data (if state
                         (collect/collect
                          chat
                          part
                          core
                          kit
                          model
                          turns
                          tokens
                          tags
                          tools
                          head
                          items
                          top)
                         (let [data (fetch/fetch
                                     chat
                                     part
                                     core
                                     kit
                                     model
                                     turns
                                     tokens
                                     tags
                                     tools
                                     text)
                               body (:body data)
                               cells (:cells data)
                               links (:links data)]
                           {:parts [body]
                            :marks cells
                            :links links
                            :prompts [text]}))
                  parts (:parts data)
                  marks (:marks data)
                  links (:links data)
                  prompts (:prompts data)
                  body (str/join "\n\n" parts)
                  info (order body marks)
                  body (:text info)
                  urls (:list info)
                  name (:name info)
                  label (fn [url text]
                          (let [text (str (or text ""))]
                            (if (str/blank? text) url text)))
                  base (reduce
                        (fn [data item]
                          (let [url (:url item)
                                seen (:seen data)
                                list (:list data)
                                text (label url (:title item))]
                            (if (and (seq url) (not (contains? seen url)))
                              {:seen (conj seen url)
                               :list (conj list {:title (if (str/blank? text)
                                                          url
                                                          text)
                                                 :url url
                                                 :excerpts []})}
                              data)))
                        {:seen #{}
                         :list []}
                        marks)
                  list (if (seq urls)
                         (mapv
                          (fn [url]
                            (let [text (label url (get name url))]
                              {:title text
                               :url url
                               :excerpts []}))
                          urls)
                         (:list base))
                  urls (if (seq urls) urls (cite/links kit links))
                  out {:content body
                       :basis [{:field "content"
                                :citations list
                                :reasoning ""}]
                       :metadata {:raw_links urls}}
                  run {:run_id (str (UUID/randomUUID))
                       :status "completed"
                       :processor "xai"
                       :created_at (.toString (java.time.Instant/now))}]
              {:run run
               :output out
               :prompts prompts})
            (finally
              (py/call-attr client "close"))))))))
