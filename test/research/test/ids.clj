(ns research.test.ids)

(defn- token
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

(defn ascii
  "Return deterministic ascii string."
  [rng size]
  (token rng size 97 26))

(defn latin
  "Return deterministic latin string."
  [rng size]
  (token rng size 256 64))

(defn cyrillic
  "Return deterministic cyrillic string."
  [rng size]
  (token rng size 1040 32))

(defn greek
  "Return deterministic greek string."
  [rng size]
  (token rng size 945 24))

(defn hiragana
  "Return deterministic hiragana string."
  [rng size]
  (token rng size 12354 32))

(defn armenian
  "Return deterministic armenian string."
  [rng size]
  (token rng size 1328 32))

(defn hebrew
  "Return deterministic hebrew string."
  [rng size]
  (token rng size 1424 32))

(defn arabic
  "Return deterministic arabic string."
  [rng size]
  (token rng size 1536 32))

(defn ids
  "Return deterministic generator."
  [seed]
  (java.util.Random. seed))
