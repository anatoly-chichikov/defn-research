(ns research.api.xai
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [jsonista.core :as json]
            [libpython-clj2.python :as py]
            [research.api.research :as research]
            [research.api.response :as response]
            [research.storage.file :as file])
  (:import (java.nio.file Files LinkOption)
           (java.nio.file.attribute FileAttribute)
           (java.util UUID)))

(defprotocol Bound
  "Object that can run XAI python runner."
  (run [item text pack] "Run XAI and return response map."))

(defn- path
  "Return cache file path."
  [root id]
  (let [base (.resolve root "tmp_cache")
        dir (.resolve base "xai")]
    (.resolve dir (str id ".json"))))

(defn- window
  "Return window days for processor."
  [data processor]
  (let [text (str/trim (or processor ""))]
    (cond
      (str/blank? text) (:window data)
      (= text "30") 30
      (= text "90") 90
      (= text "365") 365
      :else (throw (ex-info
                    "Xai processor must be 30 90 or 365"
                    {:processor processor})))))

(defn- note
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

(defn- batch
  "Return python items as JVM vector."
  [core item]
  (if (nil? item)
    []
    (try
      (vec (py/as-jvm (py/call-attr core "list" item)))
      (catch Exception _
        (try
          (let [data (py/as-jvm item)]
            (if (sequential? data) (vec data) []))
          (catch Exception __ []))))))

(defn- index
  "Return UTF-16 offset for codepoint index."
  [text point]
  (let [size (count text)
        limit (.codePointCount text 0 size)
        point (int (or point 0))
        point (if (neg? point) 0 point)
        point (if (> point limit) limit point)]
    (.offsetByCodePoints text 0 point)))

(defn- mark
  "Return citation data."
  [item]
  (let [site (py/get-attr item "web_citation")
        view (py/as-jvm site)
        page (if view (or (py/as-jvm (py/get-attr site "url")) "") "")
        link (if (seq page) nil (py/get-attr item "x_citation"))
        node (if link (py/as-jvm link) nil)
        post (if node (or (py/as-jvm (py/get-attr link "url")) "") "")
        url (if (seq page) page post)
        end (or (py/as-jvm (py/get-attr item "end_index")) 0)
        id (or (py/as-jvm (py/get-attr item "id")) "")]
    {:end end
     :id id
     :url url}))

(defn- binary
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

(defn- boot
  "Initialize python runtime."
  [path]
  (let [conf (if (str/blank? path) {} {:python-executable path})]
    (py/initialize! conf)
    (py/with-gil-stack-rc-context (py/import-module "xai_sdk"))
    :ok))

(defrecord Bridge [root info]
  Bound
  (run [_ text pack]
    (let [model (or (:model pack) "grok-4-1-fast")
          mode (or (:mode pack) "social_multi")
          turns (int (or (:turns pack) 2))
          window (int (or (:window pack) 365))
          tokens (int (or (:tokens pack) 3500))
          follow (if (contains? pack :follow) (:follow pack) true)
          domains (or (:domains pack)
                      ["reddit.com"
                       "youtube.com"
                       "tiktok.com"
                       "instagram.com"
                       "t.me"])
          include ["inline_citations"
                   "web_search_call_output"
                   "x_search_call_output"]
          lines (str/split-lines text)
          spot (first (keep-indexed
                       (fn [idx line]
                         (when (= "Research:" (str/trim line)) idx))
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
               head)
          regex #"https?://[^\s\)\]]+"]
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
            (let [state (and follow (seq items))
                  data (if state
                         (loop [parts [] cites [] links [] items items]
                           (if (seq items)
                             (let [item (first items)
                                   note (if (and (seq top)
                                                 (not
                                                  (str/includes?
                                                   (str/lower-case item)
                                                   (str/lower-case top))))
                                          (str top " - " item)
                                          item)
                                   pos (last (keep-indexed
                                              (fn [idx line]
                                                (when (not
                                                       (str/blank?
                                                        (str/trim line)))
                                                  idx))
                                              head))
                                   base (if (some? pos)
                                          (assoc head pos note)
                                          (conj head note))
                                   prompt (str/trim (str/join "\n" base))
                                   _ (println
                                      (json/write-value-as-string
                                       (note
                                        model
                                        turns
                                        tokens
                                        include
                                        tools
                                        prompt)))
                                   message (py/call-attr chat "user" prompt)
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
                                             :include include})
                                   reply (py/call-attr request "sample")
                                   raw (or (py/as-jvm
                                            (py/get-attr reply "content"))
                                           "")
                                   marks (mapv
                                          mark
                                          (batch
                                           core
                                           (py/get-attr
                                            reply
                                            "inline_citations")))
                                   pairs (filter
                                          (fn [item] (seq (:url item)))
                                          marks)
                                   pairs (sort-by :end > pairs)
                                   body (if (str/includes? raw "[[")
                                          raw
                                          (reduce
                                           (fn [text item]
                                             (let [end (:end item)
                                                   spot (index text end)
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
                                   rows (str/split-lines body)
                                   rows (map
                                         (fn [row]
                                           (let [trim (str/triml row)]
                                             (if (str/starts-with? trim "#")
                                               (str "##"
                                                    (str/replace
                                                     trim
                                                     #"^#+"
                                                     ""))
                                               row)))
                                         rows)
                                   body (str/trim (str/join "\n" rows))
                                   meta (py/get-attr reply "debug_output")
                                   refs (mapv
                                         str
                                         (batch
                                          core
                                          (py/get-attr reply "citations")))
                                   resp (try
                                          (mapv
                                           str
                                           (batch
                                            core
                                            (py/get-attr meta "responses")))
                                          (catch Exception _ []))
                                   chunk (try
                                           (mapv
                                            str
                                            (batch
                                             core
                                             (py/get-attr meta "chunks")))
                                           (catch Exception _ []))
                                   links (concat links refs resp chunk)
                                   parts (conj
                                          parts
                                          (str "# " prompt "\n\n" body))
                                   cites (into cites marks)]
                               (recur parts cites links (rest items)))
                             {:parts parts
                              :cites cites
                              :links links}))
                         (let [_ (println
                                  (json/write-value-as-string
                                   (note
                                    model
                                    turns
                                    tokens
                                    include
                                    tools
                                    text)))
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
                                         :include include})
                               reply (py/call-attr request "sample")
                               raw (or (py/as-jvm
                                        (py/get-attr reply "content"))
                                       "")
                               marks (mapv
                                      mark
                                      (batch
                                       core
                                       (py/get-attr
                                        reply
                                        "inline_citations")))
                               pairs (filter
                                      (fn [item] (seq (:url item)))
                                      marks)
                               pairs (sort-by :end > pairs)
                               body (if (str/includes? raw "[[")
                                      raw
                                      (reduce
                                       (fn [text item]
                                         (let [end (:end item)
                                               spot (index text end)
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
                                     (batch
                                      core
                                      (py/get-attr reply "citations")))
                               resp (try
                                      (mapv
                                       str
                                       (batch
                                        core
                                        (py/get-attr meta "responses")))
                                      (catch Exception _ []))
                               chunk (try
                                       (mapv
                                        str
                                        (batch
                                         core
                                         (py/get-attr meta "chunks")))
                                       (catch Exception _ []))
                               links (concat refs resp chunk)]
                           {:parts [body]
                            :cites marks
                            :links links}))
                  parts (:parts data)
                  cites (:cites data)
                  links (:links data)
                  base (reduce
                        (fn [data item]
                          (let [url (:url item)
                                seen (:seen data)
                                list (:list data)
                                link (str/replace
                                      (str url)
                                      #"^https?://"
                                      "")
                                host (first (str/split link #"/"))
                                name (str/replace (str host) #"^www\." "")]
                            (if (and (seq url) (not (contains? seen url)))
                              {:seen (conj seen url)
                               :list (conj list {:title name
                                                 :url url
                                                 :excerpts []
                                                 :confidence ""})}
                              data)))
                        {:seen #{}
                         :list []}
                        cites)
                  list (:list base)
                  raw (reduce
                       (fn [data item]
                         (let [items (re-seq regex (str item))]
                           (reduce
                            (fn [data link]
                              (let [seen (:seen data)
                                    list (:list data)]
                                (if (contains? seen link)
                                  data
                                  {:seen (conj seen link)
                                   :list (conj list link)})))
                            data
                            items)))
                       {:seen #{}
                        :list []}
                       links)
                  raw (:list raw)
                  body (str/join "\n\n" parts)
                  out {:content body
                       :basis [{:field "content"
                                :citations list
                                :reasoning ""}]
                       :metadata {:raw_links raw}}
                  run {:run_id (str (UUID/randomUUID))
                       :status "completed"
                       :processor "xai"
                       :created_at (.toString (java.time.Instant/now))}]
              {:run run
               :output out})
            (finally
              (py/call-attr client "close"))))))))

(defrecord Xai [root data unit]
  research/Researchable
  (start [_ query processor]
    (let [code (str (UUID/randomUUID))
          path (path root code)
          dir (.getParent path)
          _ (Files/createDirectories dir (make-array FileAttribute 0))
          days (window data processor)
          pack (assoc data :window days)
          file (file/file path)
          _ (file/write file {:query query
                              :config pack})]
      code))
  (stream [_ _] true)
  (finish [_ id]
    (let [path (path root id)
          file (file/file path)
          data (file/read file)
          text (:query data)
          pack (:config data)
          raw (run unit text pack)
          out (or (:output raw) {})
          body (or (:content out) "")
          basis (or (:basis out) [])
          state (or (get-in raw [:run :status]) "completed")
          code (or (get-in raw [:run :run_id]) id)]
      (response/response {:id code
                          :status state
                          :output body
                          :basis basis
                          :raw raw}))))

(defn xai
  "Create XAI client from env or map."
  [item]
  (let [root (or (:root item) (.toPath (io/file ".")))
        mode (or (:mode item) "social_multi")
        model (or (:model item) "grok-4-1-fast")
        turns (or (:turns item) 2)
        window (or (:window item) 365)
        tokens (or (:tokens item) 3500)
        follow (if (contains? item :follow) (:follow item) true)
        section (if (contains? item :section) (:section item) false)
        domains (or (:domains item)
                    ["reddit.com"
                     "youtube.com"
                     "tiktok.com"
                     "instagram.com"
                     "t.me"])
        data {:model model
              :mode mode
              :turns turns
              :window window
              :tokens tokens
              :follow follow
              :section section
              :domains domains}
        exec (or (:exec item) (System/getenv "XAI_PYTHON") "")
        path (binary root exec)
        unit (or (:unit item) (->Bridge root {:path path}))
        _ (when-not (:unit item) (boot path))]
    (->Xai root data unit)))
