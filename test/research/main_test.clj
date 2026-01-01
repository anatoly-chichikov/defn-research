(ns research.main-test
  (:require [clojure.test :refer [deftest is]]
            [jsonista.core :as json]
            [research.api.parallel :as parallel]
            [research.api.research :as research]
            [research.api.response :as response]
            [research.domain.session :as session]
            [research.image.generator :as image]
            [research.main :as main]
            [research.pdf.document :as document]
            [research.storage.file :as file]
            [research.storage.organizer :as organizer]
            [research.storage.repository :as repo])
  (:import (java.nio.file Files)
           (java.nio.file.attribute FileAttribute)))

(defn token
  "Return deterministic token string."
  [rng size base span]
  (let [build (StringBuilder.)]
    (dotimes [_ size]
      (let [pick (.nextInt rng span)
            code (+ base pick)]
        (.append build (char code))))
    (.toString build)))

(defn uuid
  "Return deterministic UUID string."
  [rng]
  (str (java.util.UUID. (.nextLong rng) (.nextLong rng))))

(deftest the-cli-parses-command
  (let [rng (java.util.Random. 25001)
        text (token rng 6 1040 32)
        data (main/parse ["create" text])]
    (is (= "create" (:cmd data)) "CLI parse is incorrect")))

(deftest the-cli-parses-options
  (let [rng (java.util.Random. 25002)
        topic (token rng 6 1040 32)
        query (token rng 7 1040 32)
        processor (token rng 5 1040 32)
        language (token rng 4 1040 32)
        provider (token rng 6 1040 32)
        data (main/parse ["run"
                          topic
                          query
                          "--processor"
                          processor
                          "--language"
                          language
                          "--provider"
                          provider])
        value (:opts data)
        goal {:processor processor
              :language language
              :provider provider
              :html false}]
    (is (= goal value) "Options were not parsed")))

(deftest the-application-run-forwards-parameters
  (let [rng (java.util.Random. 25003)
        topic (token rng 6 1040 32)
        query (token rng 7 880 32)
        code (token rng 5 1328 32)
        processor (token rng 4 1040 32)
        language (token rng 4 880 32)
        provider (token rng 4 12354 32)
        store (atom nil)
        item (reify main/Applied
               (list [_] nil)
               (show [_ _] nil)
               (generate [_ _ _] nil)
               (create [_ value] (reset! store [value]) code)
               (run [self value query processor language provider]
                 (let [code (main/create self value)]
                   (main/research self code query processor language provider)))
               (research [_ code query processor language provider]
                 (reset! store [(first @store)
                                code
                                query
                                processor
                                language
                                provider])))]
    (main/run item topic query processor language provider)
    (is (= [topic code query processor language provider] @store)
        "Run did not pass data")))

(deftest the-application-skips-cover-when-key-missing
  (let [rng (java.util.Random. 25005)
        topic (token rng 6 1040 32)
        query (token rng 7 880 32)
        processor (token rng 5 1328 32)
        language (token rng 4 12354 32)
        provider "parallel"
        run (token rng 8 1536 32)
        text (token rng 12 1040 32)
        stamp (session/format (session/now))
        ident (uuid rng)
        entry {:run_id run
               :query query
               :processor processor
               :language language
               :provider provider}
        sess (session/session {:id ident
                               :topic topic
                               :tasks []
                               :created stamp
                               :pending entry})
        root (Files/createTempDirectory "app"
                                        (make-array FileAttribute 0))
        data (.resolve root "data")
        out (.resolve root "output")
        _ (Files/createDirectories data (make-array FileAttribute 0))
        _ (Files/createDirectories out (make-array FileAttribute 0))
        store (repo/repo (file/file (.resolve data "research.json")))
        _ (repo/save store [sess])
        reply (response/response {:id run
                                  :status "completed"
                                  :output text
                                  :basis []
                                  :raw {}})
        fake (reify research/Researchable
               (start [_ _ _] run)
               (stream [_ _] true)
               (finish [_ _] reply))
        app (main/app root)
        token (subs ident 0 8)]
    (with-redefs [parallel/parallel (fn [] fake)
                  main/env (fn [_] "")
                  document/emit (fn [_ _] nil)
                  image/generate (fn [_ _ _] nil)]
      (main/research app token query processor language provider))
    (let [org (organizer/organizer out)
          name (organizer/name
                org
                (session/created sess)
                (session/topic sess)
                (session/id sess))
          cover (organizer/cover org name provider)]
      (is (not (Files/exists cover (make-array java.nio.file.LinkOption 0)))
          "Cover image was generated despite missing key"))))

(deftest the-application-writes-raw-response
  (let [rng (java.util.Random. 25007)
        topic (token rng 6 1040 32)
        query (token rng 7 880 32)
        processor (token rng 5 1328 32)
        language (token rng 4 12354 32)
        provider (token rng 5 1040 32)
        run (token rng 8 1536 32)
        text (token rng 12 1040 32)
        stamp (session/format (session/now))
        ident (uuid rng)
        entry {:run_id run
               :query query
               :processor processor
               :language language
               :provider provider}
        sess (session/session {:id ident
                               :topic topic
                               :tasks []
                               :created stamp
                               :pending entry})
        root (Files/createTempDirectory "app"
                                        (make-array FileAttribute 0))
        data (.resolve root "data")
        out (.resolve root "output")
        _ (Files/createDirectories data (make-array FileAttribute 0))
        _ (Files/createDirectories out (make-array FileAttribute 0))
        store (repo/repo (file/file (.resolve data "research.json")))
        _ (repo/save store [sess])
        key (keyword (token rng 6 1040 32))
        value (token rng 6 880 32)
        nest (keyword (token rng 5 1328 32))
        inner (keyword (token rng 4 12354 32))
        raw {key value
             nest {inner (.nextInt rng 1000)}}
        reply (response/response {:id run
                                  :status "completed"
                                  :output text
                                  :basis []
                                  :raw raw})
        fake (reify research/Researchable
               (start [_ _ _] run)
               (stream [_ _] true)
               (finish [_ _] reply))
        app (main/app root)
        token (subs ident 0 8)]
    (with-redefs [parallel/parallel (fn [] fake)
                  main/env (fn [_] "")
                  document/emit (fn [_ _] nil)
                  image/generate (fn [_ _ _] nil)]
      (main/research app token query processor language provider))
    (let [org (organizer/organizer out)
          name (organizer/name
                org
                (session/created sess)
                (session/topic sess)
                (session/id sess))
          folder (organizer/folder org name provider)
          path (.resolve folder "response.json")
          data (json/read-value
                (.toFile path)
                (json/object-mapper {:decode-key-fn keyword}))]
      (is (= raw data) "Raw response did not match stored response"))))
