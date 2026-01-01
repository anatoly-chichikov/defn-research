(ns research.image.generator-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.test :refer [deftest is]]
            [research.image.generator :as gen]))

(deftest the-generator-replaces-topic
  (let [rng (java.util.Random. 6093)
        size 7
        build (StringBuilder.)]
    (dotimes [_ size]
      (let [pick (.nextInt rng 27)
            code (if (= pick 26) 1040 (+ 97 pick))]
        (.append build (char code))))
    (let [text (.toString build)
          spec "prefix %s suffix"
          out (gen/prompt spec text)]
      (is (str/includes? out text) "Prompt replacement is incorrect"))))

(deftest the-generator-includes-wabi-sabi-principles
  "Ensure cover prompt includes wabi-sabi principles."
  (let [rng (java.util.Random. 9011)
        base (edn/read-string (slurp (io/resource "cover/parts.edn")))
        parts (or (:image base) [])
        item "cover/wabi_sabi.edn"
        entry (edn/read-string (slurp (io/resource item)))
        node (:wabi_sabi entry)
        items (or (:principles node) [])
        picks ["簡素"
               "不均正"
               "渋味"
               "自然"
               "幽玄"
               "脱俗"
               "清寂"]
        index (.nextInt rng (count picks))
        pick (nth picks index)
        names (map :name_jp items)
        ratio (get-in node [:rules :dominant_ratio])
        tones (get-in node [:rules :dominant_tones])
        result (and (some #{item} parts)
                    (= 7 (count items))
                    (some #{pick} names)
                    (str/includes? (or ratio "") "70_80")
                    (str/includes? (or tones "") "muted"))]
    (is result "Wabi-sabi principles were missing")))

(deftest the-generator-disallows-frames
  "Ensure cover prompt forbids frames."
  (let [rng (java.util.Random. 9017)
        path (io/resource "cover/quality_requirements.edn")
        base (edn/read-string (slurp path))
        value (get-in base [:quality_requirements :image_integrity :borders])
        table {"枠" "full_bleed_edge_to_edge_artwork"
               "縁" "full_bleed_edge_to_edge_artwork"
               "額" "full_bleed_edge_to_edge_artwork"}
        keys (vec (keys table))
        index (.nextInt rng (count keys))
        choice (nth keys index)
        target (get table choice)
        result (= value target)]
    (is result "Frames were permitted")))

(deftest the-generator-requires-edge-to-edge
  "Ensure cover prompt enforces edge to edge rules."
  (let [rng (java.util.Random. 9021)
        path (io/resource "cover/composition_guidelines.edn")
        node (io/resource "cover/quality_requirements.edn")
        comp (edn/read-string (slurp path))
        qual (edn/read-string (slurp node))
        edge (get-in comp [:composition_guidelines :edge_to_edge :rule])
        fill (get-in qual [:quality_requirements :image_integrity :edge_fill])
        list ["端" "縁" "際"]
        index (.nextInt rng (count list))
        pick (nth list index)
        table {"端" ["full_bleed_composition_with_cropping"
                     "edges_filled_with_scene_texture"]
               "縁" ["full_bleed_composition_with_cropping"
                     "edges_filled_with_scene_texture"]
               "際" ["full_bleed_composition_with_cropping"
                     "edges_filled_with_scene_texture"]}
        target (get table pick)
        result (and (= edge (first target)) (= fill (second target)))]
    (is result "Edge to edge rules were missing")))

(deftest the-generator-disallows-text
  "Ensure cover prompt forbids text and glyphs."
  (let [rng (java.util.Random. 9023)
        node (io/resource "cover/quality_requirements.edn")
        path (io/resource "cover/surface_treatment.edn")
        qual (edn/read-string (slurp node))
        base (edn/read-string (slurp path))
        text (get-in qual [:quality_requirements :image_integrity :text])
        rule (get-in base [:surface_treatment :all_readable_surfaces :text])
        list ["文" "字" "記"]
        index (.nextInt rng (count list))
        pick (nth list index)
        table {"文" ["no_text_no_letters_no_symbols_no_numbers_no_logos"
                     "no_text_or_glyphs_only_texture"]
               "字" ["no_text_no_letters_no_symbols_no_numbers_no_logos"
                     "no_text_or_glyphs_only_texture"]
               "記" ["no_text_no_letters_no_symbols_no_numbers_no_logos"
                     "no_text_or_glyphs_only_texture"]}
        target (get table pick)
        result (and (= text (first target)) (= rule (second target)))]
    (is result "Text restrictions were missing")))
