(ns research.domain.pending)

(defprotocol Pendinged
  "Object with pending run details."
  (id [item] "Return run identifier.")
  (query [item] "Return research query.")
  (processor [item] "Return processor name.")
  (language [item] "Return research language.")
  (provider [item] "Return provider name.")
  (data [item] "Return map representation."))

(defrecord PendingRun [id query data]
  Pendinged
  (id [_] id)
  (query [_] query)
  (processor [_] (:processor data))
  (language [_] (:language data))
  (provider [_] (:provider data))
  (data [_] {:run_id id
             :processor (:processor data)
             :language (:language data)
             :provider (:provider data)}))

(defn pending
  "Create pending run from map."
  [item]
  (->PendingRun
   (:run_id item)
   (or (:query item) "")
   {:processor (:processor item)
    :language (:language item)
    :provider (or (:provider item) "parallel")}))
