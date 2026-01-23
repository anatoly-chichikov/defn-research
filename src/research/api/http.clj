(ns research.api.http
  (:refer-clojure :exclude [get])
  (:require [org.httpkit.client :as http]))

(defprotocol Requested
  "Object that can perform HTTP requests."
  (get [item url data] "Return HTTP GET response.")
  (post [item url data] "Return HTTP POST response."))

(defrecord Http [data]
  Requested
  (get [_ url data]
    (http/get url (or data {})))
  (post [_ url data]
    (http/post url (or data {}))))

(defn make
  "Return default HTTP client."
  []
  (->Http {:kind "httpkit"}))
