(ns research.api.xai
  (:require [clojure.java.io :as io]
            [research.api.research :as research]
            [research.api.response :as response]
            [research.api.xai.brief :as brief]
            [research.api.xai.cache :as cache]
            [research.api.xai.citations :as cite]
            [research.api.xai.py-client :as py-client])
  (:import (java.util UUID)))

(defn window
  "Return fixed window days."
  [_]
  365)

(defrecord Xai [root data unit]
  research/Researchable
  (start [_ query _]
    (let [code (str (UUID/randomUUID))
          store (:store data)
          days (window data)
          pack (assoc data :window days)
          _ (cache/save store code {:query query
                                    :config pack})]
      code))
  (stream [_ _] true)
  (finish [_ id]
    (let [store (:store data)
          data (cache/load store id)
          text (:query data)
          pack (:config data)
          raw (py-client/run unit text pack)
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
        window 365
        tokens (or (:tokens item) 3500)
        flag (if (contains? item :follow) (:follow item) true)
        section (if (contains? item :section) (:section item) false)
        domains (or (:domains item)
                    ["reddit.com"
                     "youtube.com"
                     "tiktok.com"
                     "instagram.com"
                     "linkedin.com"])
        data {:model model
              :mode mode
              :turns turns
              :window window
              :tokens tokens
              :follow flag
              :section section
              :domains domains}
        exec (or (:exec item) (System/getenv "XAI_PYTHON") "")
        path (py-client/binary root exec)
        _ (when-not (:unit item) (py-client/boot path))
        brief (or (:brief item) (brief/make))
        kit (or (:cites item) (cite/make))
        store (or (:store item) (cache/make root))
        unit (or (:unit item)
                 (py-client/->Client root {:brief brief
                                           :cites kit}))
        data (assoc data :store store)]
    (->Xai root data unit)))
