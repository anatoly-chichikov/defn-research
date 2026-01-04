(ns research.api.valyu
  (:require [clojure.string :as str]
            [jsonista.core :as json]
            [org.httpkit.client :as http]
            [research.api.research :as research]
            [research.api.response :as response]))

(declare valyu-status valyu-emit)

(defn clean
  "Remove periods from log text."
  [text]
  (str/replace text #"\." ""))

(defn message
  "Return newest message and updated seen map."
  [value seen token]
  (let [items (or (:messages value) [])
        size (get seen token 0)]
    (if (<= (count items) size)
      ["" seen]
      (let [item (last items)
            text (or (:message item) (:content item) (:text item) "")
            text (cond
                   (vector? text) (str/join " " (map str text))
                   (map? text) (str text)
                   :else (str text))
            next (assoc seen token (count items))]
        [text next]))))

(defn domain
  "Extract domain from URL string."
  [text]
  (try (let [part (java.net.URI. text)
             host (.getHost part)]
         (if host (str/replace host "www." "") ""))
       (catch Exception _ "")))

(defn trusted
  "Return true when domain is trusted."
  [item text]
  (let [list (:trust item)
        tail (str text)]
    (cond
      (some #(or (= tail %) (str/ends-with? tail (str "." %))) list) true
      (or (str/ends-with? tail ".gov") (str/includes? tail ".gov.")) true
      (or (str/ends-with? tail ".edu") (str/includes? tail ".edu.")) true
      (or (str/ends-with? tail ".ac") (str/includes? tail ".ac.")) true
      (or (str/ends-with? tail ".mil") (str/includes? tail ".mil.")) true
      (or (str/ends-with? tail ".int") (str/includes? tail ".int.")) true
      :else false)))

(defn level
  "Return confidence level from source."
  [item data]
  (let [kind (or (:source data) "")
        form (or (:source_type data) (:category data) "")
        count (or (:citation_count data) 0)
        names (or (:authors data) [])
        code (or (:doi data) "")
        date (or (:publication_date data) "")
        score (:relevance_score data)
        link (or (:url data) "")
        host (if (str/blank? link) "" (domain link))
        detail (or (pos? count)
                   (seq names)
                   (not (str/blank? code))
                   (not (str/blank? date)))
        note (atom "Unknown")
        _ (when (or (contains? (:science item) kind) (= form "paper"))
            (reset! note "Medium"))
        _ (when (and (or (contains? (:science item) kind) (= form "paper"))
                     (>= count 10))
            (reset! note "High"))
        _ (when (and (= form "paper") (seq names) (not (str/blank? code)))
            (reset! note "High"))
        _ (when (and (= form "paper") (str/blank? code) (not= @note "High"))
            (reset! note "Medium"))
        _ (when (and (= kind "web")
                     (seq names)
                     (not (str/blank? date))
                     (not= @note "High"))
            (reset! note "Medium"))
        _ (when (and (contains? (:finance item) kind) (not= @note "High"))
            (reset! note "Medium"))
        _ (when (and (trusted item host) (= @note "Unknown"))
            (reset! note "Medium"))
        _ (when (and (some? score) (< score 0.5) detail (= @note "Unknown"))
            (reset! note "Low"))]
    @note))

(defrecord Valyu [key base data]
  research/Researchable
  (start [_ query processor]
    (let [url (str base "/deepresearch/tasks")
          body {:input query
                :model processor
                :output_formats ["markdown" "pdf"]}
          head {"Content-Type" "application/json"
                "x-api-key" key}
          response @(http/post url {:headers head
                                    :body (json/write-value-as-string body)
                                    :timeout 60000
                                    :as :text})
          status (:status response)
          data (if (< status 300)
                 (json/read-value
                  (:body response)
                  (json/object-mapper {:decode-key-fn keyword}))
                 (throw (ex-info "Valyu create failed" {:status status})))
          run (or (:deepresearch_id data) (:id data) "")]
      run))
  (stream [item id]
    (let [data (loop [start (System/currentTimeMillis)]
                 (let [data (valyu-status item id)
                       state (or (:status data) "")
                       value (if (map? state) (or (:value state) state) state)
                       done (or (= value "completed")
                                (= value "failed")
                                (= value "cancelled")
                                (= value "canceled"))
                       _ (valyu-emit data)]
                   (if done
                     data
                     (if (> (- (System/currentTimeMillis) start) 7200000)
                       (throw (ex-info "Valyu task timed out" {:id id}))
                       (do (Thread/sleep 60000) (recur start))))))]
      data))
  (finish [item id]
    (let [data (valyu-status item id)
          output (:output data)
          text (if (map? output)
                 (or (:markdown output) (:content output) "")
                 (or output ""))
          sources (or (:sources data) [])
          base (research/basis item sources)
          state (or (:status data) "completed")
          status (if (map? state) (or (:value state) state) state)
          code (or (:deepresearch_id data) (:id data) id)]
      (response/response {:id code
                          :status status
                          :output text
                          :basis base
                          :raw data})))
  research/Grounded
  (basis [item sources]
    (reduce
     (fn [list data]
       (let [link (or (:url data) "")
             text (or (:content data) (:snippet data) (:description data) "")
             title (or (:title data) (if (str/blank? link) "" (domain link)))
             level (level (:data item) data)]
         (if (str/blank? link)
           list
           (conj list {:citations [{:title title
                                    :url link
                                    :excerpts [text]}]
                       :confidence level}))))
     []
     sources)))

(defn valyu-status
  "Return status payload from Valyu API."
  [item id]
  (let [url (str (:base item) "/deepresearch/tasks/" id "/status")
        head {"Content-Type" "application/json"
              "x-api-key" (:key item)}
        response @(http/get url {:headers head
                                 :timeout 60000
                                 :as :text})
        status (:status response)
        data (if (< status 300)
               (json/read-value
                (:body response)
                (json/object-mapper {:decode-key-fn keyword}))
               (throw (ex-info "Valyu status failed" {:status status})))]
    data))

(defn valyu-emit
  "Emit progress info for Valyu."
  [data]
  (let [status (or (:status data) "")
        progress (or (:progress data) {})
        current (get progress :current_step nil)
        total (get progress :total_steps nil)
        message (or (:message data) "")
        items (cond-> []
                (not (str/blank? (str status)))
                (conj (str status))
                (and (some? current) (some? total))
                (conj (str current "/" total))
                (not (str/blank? message))
                (conj message))
        line (if (seq items) (str/join " | " items) (str data))]
    (println (clean (str "[PROGRESS] " line)))))

(defn valyu
  "Create Valyu client from env or map."
  [item]
  (let [key (or (:key item) (System/getenv "VALYU_API_KEY") "")
        base (or (:base item)
                 (System/getenv "VALYU_BASE_URL")
                 "https://api.valyu.ai")
        base (if (and (str/includes? base "api.valyu.ai")
                      (not (str/ends-with? base "/v1")))
               (str (str/replace base #"/+$" "") "/v1")
               base)
        mode (or (:mode item) "")
        data {:science #{"valyu/valyu-arxiv"
                         "valyu/valyu-pubmed"
                         "valyu/valyu-clinical-trials"}
              :finance #{"valyu/valyu-stocks" "valyu/sec-filings" "valyu/sec"}
              :trust #{"acm.org"
                       "apnews.com"
                       "arxiv.org"
                       "bbc.co.uk"
                       "bloomberg.com"
                       "britannica.com"
                       "cell.com"
                       "doi.org"
                       "economist.com"
                       "elsevier.com"
                       "europa.eu"
                       "ft.com"
                       "ieee.org"
                       "ietf.org"
                       "imf.org"
                       "jstor.org"
                       "nature.com"
                       "nytimes.com"
                       "oecd.org"
                       "openalex.org"
                       "ourworldindata.org"
                       "reuters.com"
                       "science.org"
                       "sciencedirect.com"
                       "springer.com"
                       "tandfonline.com"
                       "theguardian.com"
                       "un.org"
                       "worldbank.org"
                       "who.int"
                       "wikipedia.org"
                       "wiley.com"
                       "w3.org"
                       "wsj.com"}}]
    (if (and (str/blank? key) (not= mode "basis"))
      (throw (ex-info "VALYU_API_KEY is required" {}))
      (->Valyu key base {:science (:science data)
                         :finance (:finance data)
                         :trust (:trust data)}))))
