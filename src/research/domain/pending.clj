(ns research.domain.pending)

(defprotocol Pendinged
  "Object with pending run details."
  (id [item] "Return run identifier.")
  (query [item] "Return research query.")
  (processor [item] "Return processor name.")
  (language [item] "Return research language.")
  (provider [item] "Return provider name.")
  (data [item] "Return map representation."))

(defrecord Pending [id query data]
  Pendinged
  (id [_] id)
  (query [_] query)
  (processor [_] (:processor data))
  (language [_] (:language data))
  (provider [_] (:provider data))
  (data [_] {:run_id id
             :query query
             :processor (:processor data)
             :language (:language data)
             :provider (:provider data)}))

(defn pending
  "Create pending run from map."
  [item]
  (->Pending
   (:run_id item)
   (:query item)
   {:processor (:processor item)
    :language (:language item)
    :provider (or (:provider item) "parallel")}))
