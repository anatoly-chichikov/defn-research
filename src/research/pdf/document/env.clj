(ns research.pdf.document.env
  (:require [clojure.java.shell :as shell]
            [clojure.string :as str]
            [markdown.core :as md]
            [research.domain.pending :as pending]
            [research.domain.session :as sess]
            [research.domain.task :as task]
            [research.pdf.document.citations :as cite]
            [research.pdf.document.text :as text]
            [research.storage.organizer :as organizer])
  (:import (java.nio.file Files LinkOption)
           (java.nio.file.attribute FileAttribute)))

(declare provider)

(defn env
  "Return environment value by key."
  [key]
  (System/getenv key))

(defn emit
  "Render PDF using WeasyPrint."
  [html path]
  (let [tmp (Files/createTempFile "report" ".html" (make-array FileAttribute 0))
        _ (spit (.toFile tmp) html :encoding "UTF-8")
        vars (into {} (System/getenv))
        home (or (get vars "DYLD_FALLBACK_LIBRARY_PATH") "")
        list [home "/opt/homebrew/lib" "/usr/local/lib"]
        list (filter #(not (str/blank? %)) list)
        link (str/join ":" list)
        vars (assoc vars "DYLD_FALLBACK_LIBRARY_PATH" link)
        res (shell/sh
             "uv"
             "run"
             "--with"
             "weasyprint"
             "python"
             "-m"
             "weasyprint"
             (.toString tmp)
             (.toString path)
             :env vars)
        code (:exit res)
        _ (Files/deleteIfExists tmp)]
    (if (zero? code)
      path
      (throw (ex-info "Weasyprint failed" {:code code
                                           :out (:out res)
                                           :err (:err res)})))))

(defn author
  "Return report author from env."
  []
  (or (env "REPORT_FOR") ""))

(defn service
  "Return service name from latest task."
  [item]
  (let [list (sess/tasks item)
        last (last list)]
    (if last (task/provider last) "parallel.ai")))

(defn coverimage
  "Render cover image html."
  [item]
  (let [cover (:cover item)]
    (if (and (.isPresent cover)
             (Files/exists (.get cover) (make-array LinkOption 0)))
      (str "<div class=\"cover-image\"><img src=\""
           (.toString (.toUri (.get cover)))
           "\" alt=\"Cover\" /></div>")
      "")))

(defn briefpath
  "Return brief file path."
  [item]
  (let [sess (:session item)
        root (:root item)
        org (organizer/organizer root)
        name (organizer/name
              org
              (sess/created sess)
              (sess/topic sess)
              (sess/id sess))
        list (sess/tasks sess)
        tail (last list)
        hold (sess/pending sess)
        slot (if (and (not tail) (.isPresent hold)) (.get hold) nil)
        service (if tail
                  (provider tail)
                  (if slot (pending/provider slot) "provider"))
        tag (organizer/slug service)
        tag (if (str/blank? tag) "provider" tag)
        base (.resolve root name)
        brief (.resolve base (str "brief-" tag ".md"))
        input (.resolve base (str "input-" tag ".md"))
        path (if (Files/exists brief (make-array LinkOption 0)) brief input)]
    (.toFile path)))

(defn brief
  "Render brief section."
  [item]
  (let [list (sess/tasks (:session item))
        head (first list)
        path (briefpath item)
        text (if (and head (.exists path))
               (slurp path :encoding "UTF-8")
               (if head (task/query head) ""))
        text (text/listify text)
        text (text/normalize text)
        text (text/rule text)
        text (cite/stars text)
        html (md/md-to-html-string text)
        html (cite/tables html)
        html (cite/codeindent html)
        html (cite/backslash html)]
    (if (str/blank? html)
      ""
      (str "<div class=\"brief\"><div class=\"container\">"
           "<h1>Exploration Brief</h1>"
           "<div class=\"query\">"
           html
           "</div></div></div>"))))

(defn provider
  "Return provider slug from task service."
  [item]
  (let [name (task/provider item)]
    (if (str/ends-with? name ".ai") (first (str/split name #"\.")) name)))
