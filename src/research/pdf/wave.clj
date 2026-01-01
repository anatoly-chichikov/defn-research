(ns research.pdf.wave
  (:require [research.pdf.palette :as palette]))

(defprotocol Rendered
  "Object that can render to string."
  (render [item] "Return rendered string."))

(defrecord Wave [palette]
  Rendered
  (render [_]
    (str "<svg viewBox=\"0 0 1200 200\" preserveAspectRatio=\"none\" "
         "xmlns=\"http://www.w3.org/2000/svg\">"
         "<defs><linearGradient id=\"waveGradient\" x1=\"0%\" y1=\"0%\" "
         "x2=\"0%\" y2=\"100%\">"
         "<stop offset=\"0%\" style=\"stop-color:"
         (palette/link palette)
         ";stop-opacity:0.9\"/>"
         "<stop offset=\"100%\" style=\"stop-color:"
         (palette/heading palette)
         ";stop-opacity:1\"/>"
         "</linearGradient></defs>"
         "<path d=\"M0,100 C100,150 200,50 300,100 C400,150 500,50 600,"
         "100 C700,150 800,50 900,100 C1000,150 1100,50 1200,100 "
         "L1200,200 L0,200 Z\" fill=\"url(#waveGradient)\"/>"
         "<path d=\"M0,120 C150,80 250,160 400,120 C550,80 650,160 800,"
         "120 C950,80 1050,160 1200,120 L1200,200 L0,200 Z\" fill=\""
         (palette/heading palette)
         "\" opacity=\"0.7\"/>"
         "<path d=\"M0,140 C200,180 400,100 600,140 C800,180 1000,100 "
         "1200,140 L1200,200 L0,200 Z\" fill=\""
         (palette/text palette)
         "\" opacity=\"0.5\"/>"
         "<circle cx=\"100\" cy=\"90\" r=\"8\" fill=\""
         (palette/bg palette)
         "\" opacity=\"0.8\"/>"
         "<circle cx=\"350\" cy=\"70\" r=\"6\" fill=\""
         (palette/bg palette)
         "\" opacity=\"0.6\"/>"
         "<circle cx=\"600\" cy=\"85\" r=\"10\" fill=\""
         (palette/bg palette)
         "\" opacity=\"0.7\"/>"
         "<circle cx=\"850\" cy=\"75\" r=\"7\" fill=\""
         (palette/bg palette)
         "\" opacity=\"0.5\"/>"
         "<circle cx=\"1100\" cy=\"80\" r=\"9\" fill=\""
         (palette/bg palette)
         "\" opacity=\"0.8\"/>"
         "</svg>")))

(defrecord Footer [palette]
  Rendered
  (render [_]
    (str "<svg viewBox=\"0 0 1200 100\" preserveAspectRatio=\"none\" "
         "xmlns=\"http://www.w3.org/2000/svg\">"
         "<path d=\"M0,0 L0,50 C150,80 350,20 600,50 C850,80 1050,20 "
         "1200,50 L1200,0 Z\" fill=\""
         (palette/link palette)
         "\" opacity=\"0.3\"/>"
         "<path d=\"M0,0 L0,30 C200,60 400,10 600,30 C800,50 1000,10 "
         "1200,30 L1200,0 Z\" fill=\""
         (palette/heading palette)
         "\" opacity=\"0.5\"/>"
         "</svg>")))

(defn wave
  "Create wave from palette."
  [item]
  (->Wave item))

(defn footer
  "Create footer from palette."
  [item]
  (->Footer item))
