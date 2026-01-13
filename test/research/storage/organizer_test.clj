(ns research.storage.organizer-test
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is]]
            [jsonista.core :as json]
            [research.storage.organizer :as organizer]
            [research.test.ids :as gen])
  (:import (java.nio.file Files)
           (java.nio.file.attribute FileAttribute)
           (java.time LocalDateTime)))

(deftest the-organizer-creates-folder-for-session
  (let [rng (gen/ids 23001)
        root (Files/createTempDirectory "org"
                                        (make-array FileAttribute 0))
        ident (gen/uuid rng)
        provider (gen/cyrillic rng 6)
        item (organizer/organizer root)
        path (organizer/folder item ident provider)]
    (is (Files/exists path (make-array java.nio.file.LinkOption 0))
        "Folder was not created for session")))

(deftest the-organizer-folder-contains-identifier
  (let [rng (gen/ids 23003)
        root (Files/createTempDirectory "org"
                                        (make-array FileAttribute 0))
        ident (gen/uuid rng)
        item (organizer/organizer root)
        path (organizer/folder item ident "valyu")]
    (is (str/includes? (str path) ident)
        "Folder path did not contain session identifier")))

(deftest the-organizer-saves-response-as-json
  (let [rng (gen/ids 23005)
        root (Files/createTempDirectory "org"
                                        (make-array FileAttribute 0))
        ident (gen/uuid rng)
        item (organizer/organizer root)
        key (str "test-" (gen/uuid rng))
        path (organizer/response item ident "valyu" {key "données"})
        data (json/read-value
              (.toFile path)
              (json/object-mapper {:decode-key-fn keyword}))]
    (is (= "données" (get data (keyword key)))
        "Response JSON did not contain expected data")))

(deftest the-organizer-response-creates-folder
  (let [rng (gen/ids 23007)
        root (Files/createTempDirectory "org"
                                        (make-array FileAttribute 0))
        ident (gen/uuid rng)
        item (organizer/organizer root)
        path (organizer/response item ident "parallel" {:created true})]
    (is (Files/exists (.getParent path)
                      (make-array java.nio.file.LinkOption 0))
        "Response did not create parent folder")))

(deftest the-organizer-cover-returns-jpg-path
  (let [rng (gen/ids 23009)
        root (Files/createTempDirectory "org"
                                        (make-array FileAttribute 0))
        ident (gen/uuid rng)
        provider (gen/cyrillic rng 6)
        item (organizer/organizer root)
        path (organizer/cover item ident provider)
        tag (organizer/slug provider)
        goal (str "cover-" tag ".jpg")]
    (is (str/ends-with? (str path) goal)
        "Cover path did not include provider suffix")))

(deftest the-organizer-report-returns-pdf-path
  (let [rng (gen/ids 23011)
        root (Files/createTempDirectory "org"
                                        (make-array FileAttribute 0))
        ident (gen/uuid rng)
        provider (gen/cyrillic rng 6)
        item (organizer/organizer root)
        path (organizer/report item ident provider)
        tag (organizer/slug provider)
        goal (str "-" tag ".pdf")]
    (is (str/ends-with? (str path) goal)
        "Report path did not include provider suffix")))

(deftest the-organizer-html-returns-html-path
  (let [rng (gen/ids 23013)
        root (Files/createTempDirectory "org"
                                        (make-array FileAttribute 0))
        ident (gen/uuid rng)
        provider (gen/cyrillic rng 6)
        item (organizer/organizer root)
        path (organizer/html item ident provider)
        tag (organizer/slug provider)
        goal (str "-" tag ".html")]
    (is (str/ends-with? (str path) goal)
        "HTML path did not include provider suffix")))

(deftest the-organizer-existing-returns-empty-for-missing
  (let [rng (gen/ids 23015)
        root (Files/createTempDirectory "org"
                                        (make-array FileAttribute 0))
        ident (gen/uuid rng)
        item (organizer/organizer root)
        result (organizer/existing item ident "valyu")]
    (is (not (.isPresent result))
        "Existing returned path for missing cover")))

(deftest the-organizer-existing-returns-path-when-exists
  (let [rng (gen/ids 23017)
        root (Files/createTempDirectory "org"
                                        (make-array FileAttribute 0))
        ident (gen/uuid rng)
        item (organizer/organizer root)
        cover (organizer/cover item ident "valyu")]
    (Files/createDirectories (.getParent cover)
                             (make-array FileAttribute 0))
    (spit (.toFile cover) "fake image" :encoding "UTF-8")
    (is (.isPresent (organizer/existing item ident "valyu"))
        "Existing returned empty for existing cover")))

(deftest the-organizer-saves-brief-as-markdown
  (let [rng (gen/ids 23019)
        root (Files/createTempDirectory "org"
                                        (make-array FileAttribute 0))
        ident (gen/uuid rng)
        item (organizer/organizer root)
        content (str "# Test Brief\n\nСодержимое " (gen/uuid rng))
        path (organizer/brief item ident "параллель" content)
        data (slurp (.toFile path) :encoding "UTF-8")]
    (is (= content data) "Brief content was not saved correctly")))

(deftest the-organizer-name-formats-correctly
  (let [root (Files/createTempDirectory "org"
                                        (make-array FileAttribute 0))
        item (organizer/organizer root)
        created (LocalDateTime/of 2025 12 7 15 30)
        topic "Coffee vs Tea"
        ident "589a125c-8ae7-4c28-ac95-7c1127b601d3"
        name (organizer/name item created topic ident)]
    (is (= "2025-12-07_coffee-vs-tea_589a125c" name)
        "Name format did not match expected pattern")))

(deftest the-organizer-name-handles-special-characters
  (let [rng (gen/ids 23023)
        root (Files/createTempDirectory "org"
                                        (make-array FileAttribute 0))
        item (organizer/organizer root)
        created (LocalDateTime/of 2025 1 15 10 0)
        topic "What's the deal with: émojis & symbols?"
        ident (gen/uuid rng)
        name (organizer/name item created topic ident)]
    (is (re-matches #"^2025-01-15_[a-z0-9-]+_[a-f0-9]{8}$" name)
        "Name contained invalid characters")))

(deftest the-organizer-name-transliterates-cyrillic
  (let [root (Files/createTempDirectory "org"
                                        (make-array FileAttribute 0))
        item (organizer/organizer root)
        created (LocalDateTime/of 2025 12 22 19 30)
        topic "ИИ-арт как творчество"
        ident "bb1ce2e7-1234-5678-9abc-def012345678"
        name (organizer/name item created topic ident)]
    (is (= "2025-12-22_ii-art-kak-tvorchestvo_bb1ce2e7" name)
        "Cyrillic topic was not transliterated correctly")))

(deftest the-organizer-name-falls-back-to-untitled
  (let [root (Files/createTempDirectory "org"
                                        (make-array FileAttribute 0))
        item (organizer/organizer root)
        created (LocalDateTime/of 2025 12 22 19 30)
        topic "中文主題"
        ident "abc12345-1234-5678-9abc-def012345678"
        name (organizer/name item created topic ident)]
    (is (= "2025-12-22_untitled_abc12345" name)
        "Empty slug did not fallback to untitled")))
