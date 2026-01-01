(ns research.pdf.palette)

(defprotocol Colored
  "Object with palette colors."
  (bg [item] "Return background color.")
  (text [item] "Return text color.")
  (heading [item] "Return heading color.")
  (link [item] "Return link color.")
  (muted [item] "Return muted color.")
  (quote [item] "Return quote background color.")
  (accent [item] "Return accent color.")
  (codebg [item] "Return code block color.")
  (codeinline [item] "Return inline code color.")
  (border [item] "Return border color."))

(defrecord Palette [data]
  Colored
  (bg [_] (:bg data))
  (text [_] (:text data))
  (heading [_] (:heading data))
  (link [_] (:link data))
  (muted [_] (:muted data))
  (quote [_] (:quote data))
  (accent [_] (:accent data))
  (codebg [_] (:codebg data))
  (codeinline [_] (:codeinline data))
  (border [_] (:border data)))

(defn palette
  "Create palette with Hokusai defaults."
  []
  (->Palette {:bg "#F6EFE3"
              :text "#1C2430"
              :heading "#193D5E"
              :link "#3A5F88"
              :muted "#6B645A"
              :quote "#E3D9C6"
              :accent "#D04A35"
              :codebg "#1C2833"
              :codeinline "#DDD5C5"
              :border "#BFB5A3"}))
