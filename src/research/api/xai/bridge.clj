(ns research.api.xai.bridge
  (:require [clojure.string :as str]
            [libpython-clj2.python :as py]
            [research.api.link :as link]
            [research.api.xai.bridge.collect :as collect]
            [research.api.xai.bridge.fetch :as fetch]
            [research.api.xai.brief :as brief]
            [research.api.xai.citations :as cite])
  (:import (java.nio.file Files LinkOption)
           (java.util UUID)))

(defprotocol Bound
  "Object that can run XAI python runner."
  (run [item text pack] "Run XAI and return response map."))

(defn note
  "Return log record for XAI request."
  [model turns tokens include tools text]
  (fetch/note model turns tokens include tools text))

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

(defrecord Bridge [root data]
  Bound
  (run [_ text pack]
    (let [model (or (:model pack) "grok-4-1-fast")
          mode (or (:mode pack) "social_multi")
          turns (int (or (:turns pack) 2))
          window (int (or (:window pack) 365))
          tokens (int (or (:tokens pack) 3500))
          flag (if (contains? pack :follow) (:follow pack) true)
          domains (or (:domains pack)
                      ["reddit.com"
                       "youtube.com"
                       "tiktok.com"
                       "instagram.com"
                       "t.me"])
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
                    (= mode "social_multi")
                    (py/call-attr-kw
                     tool
                     "web_search"
                     []
                     {:allowed_domains domains})
                    (= mode "social")
                    (py/call-attr-kw
                     tool
                     "web_search"
                     []
                     {:allowed_domains domains})
                    :else (py/call-attr tool "web_search"))
              tools (if (= mode "social_multi") [x web] [web])
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
                            :links links}))
                  parts (:parts data)
                  marks (:marks data)
                  links (:links data)
                  policy (or (:link (:data kit)) (link/make))
                  base (reduce
                        (fn [data item]
                          (let [url (:url item)
                                seen (:seen data)
                                list (:list data)
                                name (or (:title item)
                                         (link/domain policy url))]
                            (if (and (seq url) (not (contains? seen url)))
                              {:seen (conj seen url)
                               :list (conj list {:title name
                                                 :url url
                                                 :excerpts []
                                                 :confidence ""})}
                              data)))
                        {:seen #{}
                         :list []}
                        marks)
                  list (:list base)
                  urls (cite/links kit links)
                  body (str/join "\n\n" parts)
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
               :output out})
            (finally
              (py/call-attr client "close"))))))))
