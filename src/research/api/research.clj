(ns research.api.research)

(defprotocol Researchable
  "Object that can execute research."
  (start [item query processor] "Start research and return run id.")
  (stream [item id] "Stream progress updates.")
  (finish [item id] "Finish research and return response."))

(defprotocol Grounded
  "Object that can build citation basis."
  (basis [item sources] "Return basis entries."))
