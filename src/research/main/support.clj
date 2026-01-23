(ns research.main.support
  (:require [clojure.string :as str]
            [org.httpkit.client :as http]
            [research.storage.organizer :as organizer])
  (:import (java.nio.file Files LinkOption)
           (java.nio.file.attribute FileAttribute)))

(defn env
  "Return environment value by key."
  [key]
  (System/getenv key))

(defn store
  "Store Valyu images for output folder."
  [name provider data root]
  (let [items (or (:images data) [])]
    (when (= provider "valyu")
      (let [org (organizer/organizer root)
            tag (organizer/slug provider)
            tag (if (str/blank? tag) "provider" tag)
            folder (organizer/folder org name provider)
            folder (.resolve folder (str "images-" tag))]
        (doseq [item items]
          (let [link (or (:image_url item) "")
                code (or (:image_id item) "")
                path (if (or (str/blank? link) (str/blank? code))
                       ""
                       (try (or (.getPath (java.net.URI. link)) "")
                            (catch Exception _ "")))
                part (second (re-find #"(\\.[^./]+)$" path))
                part (if (str/blank? part) ".png" part)
                file (if (str/blank? code)
                       ""
                       (.resolve folder "images"))
                file (if (str/blank? (str file))
                       ""
                       (.resolve file (str code part)))]
            (when (and (not (str/blank? link)) (not (str/blank? (str file))))
              (Files/createDirectories
               (.getParent file)
               (make-array FileAttribute 0))
              (when-not (Files/exists file (make-array LinkOption 0))
                (let [response @(http/get link {:timeout 60000
                                                :as :byte-array})]
                  (when (< (:status response) 300)
                    (Files/write
                     file
                     (:body response)
                     (make-array java.nio.file.OpenOption 0))))))))))))
