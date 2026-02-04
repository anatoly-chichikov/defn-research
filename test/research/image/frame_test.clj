(ns research.image.frame-test
  (:require [clojure.test :refer [deftest is]]
            [research.image.frame :as frame]
            [research.image.generator :as gen])
  (:import (java.awt.image BufferedImage)
           (java.nio.file Files LinkOption)
           (java.nio.file.attribute FileAttribute)
           (javax.imageio ImageIO)))

(defn- color
  "Return packed rgb int."
  [red green blue]
  (bit-or (bit-shift-left red 16) (bit-shift-left green 8) blue))

(defn- tone
  "Return random rgb int."
  [rng]
  (let [red (.nextInt rng 200)
        green (.nextInt rng 200)
        blue (.nextInt rng 200)]
    (color red green blue)))

(defn- contrast
  "Return high-contrast rgb int."
  [rgb]
  (let [red (bit-and (unsigned-bit-shift-right rgb 16) 255)
        green (bit-and (unsigned-bit-shift-right rgb 8) 255)
        blue (bit-and rgb 255)
        value (+ (* 0.299 red) (* 0.587 green) (* 0.114 blue))
        pick (if (> value 127.0) 0 255)]
    (color pick pick pick)))

(defn- fill
  "Fill rectangle with color."
  [image x y w h rgb]
  (dotimes [yy h]
    (dotimes [xx w]
      (.setRGB image (+ x xx) (+ y yy) rgb)))
  image)

(defn- canvas
  "Return blank image filled with color."
  [wide high rgb]
  (let [image (BufferedImage. wide high BufferedImage/TYPE_INT_RGB)]
    (fill image 0 0 wide high rgb)))

(defn- inset
  "Return image with inset frame."
  [image pad band rgb]
  (let [wide (.getWidth image)
        high (.getHeight image)
        x pad
        y pad
        w (- wide (* 2 pad))
        h (- high (* 2 pad))]
    (fill image x y w band rgb)
    (fill image x (- (+ y h) band) w band rgb)
    (fill image x y band h rgb)
    (fill image (- (+ x w) band) y band h rgb)
    image))

(defn- noise
  "Fill image with random noise."
  [image rng]
  (let [wide (.getWidth image)
        high (.getHeight image)]
    (dotimes [y high]
      (dotimes [x wide]
        (.setRGB image x y (tone rng))))
    image))

(defn- save
  "Write image to temp file."
  [image name]
  (let [dir (Files/createTempDirectory name (make-array FileAttribute 0))
        file (.resolve dir (str name ".png"))]
    (ImageIO/write image "png" (.toFile file))
    file))

(defn- maker
  "Return generator writing random bytes."
  [rng]
  (reify gen/Generated
    (generate [_ _ path]
      (let [size (+ 64 (.nextInt rng 64))
            data (byte-array size)]
        (.nextBytes rng data)
        (Files/createDirectories
         (.getParent path)
         (make-array FileAttribute 0))
        (Files/write path data (make-array java.nio.file.OpenOption 0))
        path))))

(defn- watcher
  "Return detector with queued frames."
  [state]
  (reify frame/Framed
    (detect [_ _]
      (let [list @state
            item (first list)
            tail (rest list)
            _ (reset! state tail)]
        {:frame (boolean item)
         :info {}}))
    (scan [_ _] {:total 0
                 :hits 0
                 :rows []})))

(deftest ^{:doc "Ensure detector finds frames."}
  the-detector-detects-frames
  (let [rng (java.util.Random. 61011)
        wide (+ 240 (.nextInt rng 40))
        high (+ 140 (.nextInt rng 30))
        band (+ 6 (.nextInt rng 3))
        pad (+ 8 (.nextInt rng 4))
        base (tone rng)
        edge (contrast base)
        image (canvas wide high base)
        image (inset image pad band edge)
        name (str "frame-" (.nextInt rng 10000))
        path (save image name)
        config {:cap 0.08
                :min 1
                :std 10.0
                :diff 10.0
                :edge 0.25
                :noise 0.05
                :sides 4
                :tone 25.0
                :span 512.0
                :sigma 0.33
                :floor 0.0
                :ridge 0.2
                :peak 2.0
                :band 1
                :lead "cover"
                :exts #{".png"}}
        detector (frame/detector config)
        result (frame/detect detector path)]
    (is (:frame result) "Frame was not detected")))

(deftest ^{:doc "Ensure detector ignores full bleed."}
  the-detector-ignores-full-bleed
  (let [rng (java.util.Random. 61013)
        wide (+ 220 (.nextInt rng 30))
        high (+ 130 (.nextInt rng 20))
        base (tone rng)
        image (canvas wide high base)
        image (noise image rng)
        name (str "test-" (.nextInt rng 10000))
        path (save image name)
        config {:cap 0.05
                :min 1
                :std 6.0
                :diff 18.0
                :edge 0.25
                :noise 0.05
                :sides 4
                :tone 12.0
                :span 512.0
                :sigma 0.33
                :floor 0.0
                :ridge 0.9
                :peak 12.0
                :band 1
                :lead "cover"
                :exts #{".png"}}
        detector (frame/detector config)
        result (frame/detect detector path)]
    (is (not (:frame result)) "Frame was detected")))

(deftest ^{:doc "Ensure retry stores first failed attempt."}
  the-retry-stores-the-first-failed-attempt
  (let [rng (java.util.Random. 61021)
        name (str "attempt-" (.nextInt rng 10000))
        dir (Files/createTempDirectory name (make-array FileAttribute 0))
        path (.resolve dir (str name ".jpg"))
        gen (maker rng)
        state (atom [true false])
        det (watcher state)
        _ (frame/retry gen det "тема" path 4)
        attempt (.resolve dir (str name ".attempt-1.jpg"))
        ok (Files/exists attempt (make-array LinkOption 0))]
    (is ok "Attempt backup was not created")))

(deftest ^{:doc "Ensure retry stores second failed attempt."}
  the-retry-stores-the-second-failed-attempt
  (let [rng (java.util.Random. 61022)
        name (str "attempt-" (.nextInt rng 10000))
        dir (Files/createTempDirectory name (make-array FileAttribute 0))
        path (.resolve dir (str name ".jpg"))
        gen (maker rng)
        state (atom [true true false])
        det (watcher state)
        _ (frame/retry gen det "тема" path 4)
        attempt (.resolve dir (str name ".attempt-2.jpg"))
        ok (Files/exists attempt (make-array LinkOption 0))]
    (is ok "Second attempt backup was not created")))

(deftest ^{:doc "Ensure retry skips backup when no frame."}
  the-retry-skips-backup-when-no-frame
  (let [rng (java.util.Random. 61023)
        name (str "attempt-" (.nextInt rng 10000))
        dir (Files/createTempDirectory name (make-array FileAttribute 0))
        path (.resolve dir (str name ".jpg"))
        gen (maker rng)
        state (atom [false])
        det (watcher state)
        _ (frame/retry gen det "тема" path 4)
        attempt (.resolve dir (str name ".attempt-1.jpg"))
        ok (Files/exists attempt (make-array LinkOption 0))]
    (is (not ok) "Unexpected backup was created")))
