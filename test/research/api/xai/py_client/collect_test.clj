(ns research.api.xai.py-client.collect-test
  (:require [clojure.edn :as edn]
            [clojure.string :as str]
            [clojure.test :refer [deftest is]]
            [research.api.xai.py-client.collect :as collect]
            [research.api.xai.py-client.fetch :as fetch]
            [research.test.ids :as gen]))

(deftest the-collector-normalizes-heading-levels
  (let [rng (gen/ids 18311)
        topic (gen/greek rng 6)
        alpha (gen/armenian rng 5)
        beta (gen/arabic rng 5)
        gamma (gen/hebrew rng 5)
        delta (gen/hiragana rng 5)
        eta (gen/hebrew rng 4)
        theta (gen/arabic rng 4)
        iota (gen/armenian rng 4)
        lang (str "Response language: " (gen/greek rng 4) ".")
        head [lang "" topic]
        body (str "# " eta "\n\n## " theta "\n\n### " iota)
        chat (gen/latin rng 4)
        part (gen/latin rng 5)
        core (gen/latin rng 6)
        kit {:id (gen/latin rng 3)}
        model (gen/latin rng 4)
        turns (inc (.nextInt rng 4))
        tokens (inc (.nextInt rng 100))
        tags [(gen/latin rng 4)]
        tools [(gen/latin rng 4)]
        left (str alpha ": " beta " — " gamma)
        right (str delta " — " gamma)
        data (with-redefs
              [fetch/fetch (fn [_ _ _ _ _ _ _ _ _ _]
                             {:body body
                              :cells []
                              :links []})]
               (collect/collect
                chat
                part
                core
                kit
                model
                turns
                tokens
                tags
                tools
                head
                [{:depth 1
                  :text left}
                 {:depth 1
                  :text right}]
                topic))
        parts (:parts data)
        target [(str "## " eta "\n\n### " theta "\n\n### " iota)
                (str "## " eta "\n\n### " theta "\n\n### " iota)]]
    (is (= target parts) "output did not match expected headings")))

(deftest the-collector-drops-duplicate-headings
  (let [rng (gen/ids 18313)
        topic (gen/greek rng 6)
        alpha (gen/armenian rng 5)
        beta (gen/hebrew rng 5)
        gamma (gen/hiragana rng 4)
        lang (str "Response language: " (gen/greek rng 4) ".")
        head [lang "" topic]
        body (str "# " alpha "\n\n## " alpha "\n\n### " gamma)
        chat (gen/latin rng 4)
        part (gen/latin rng 5)
        core (gen/latin rng 6)
        kit {:id (gen/latin rng 3)}
        model (gen/latin rng 4)
        turns (inc (.nextInt rng 4))
        tokens (inc (.nextInt rng 100))
        tags [(gen/latin rng 4)]
        tools [(gen/latin rng 4)]
        item (str beta " — " gamma)
        data (with-redefs
              [fetch/fetch (fn [_ _ _ _ _ _ _ _ _ _]
                             {:body body
                              :cells []
                              :links []})]
               (collect/collect
                chat
                part
                core
                kit
                model
                turns
                tokens
                tags
                tools
                head
                [{:depth 1
                  :text item}]
                topic))
        parts (:parts data)
        target [(str "## " alpha "\n\n### " gamma)]]
    (is (= target parts) "Duplicate heading was not removed")))

(deftest the-collector-builds-prompt-with-section-context
  (let [rng (gen/ids 18317)
        topic (gen/greek rng 6)
        alpha (gen/arabic rng 5)
        beta (gen/hebrew rng 6)
        lang (str "Response language: " (gen/greek rng 4) ".")
        head [lang "" topic]
        text (str alpha " — " beta)
        chat (gen/latin rng 4)
        part (gen/latin rng 5)
        core (gen/latin rng 6)
        kit {:id (gen/latin rng 3)}
        model (gen/latin rng 4)
        turns (inc (.nextInt rng 4))
        tokens (inc (.nextInt rng 100))
        tags [(gen/latin rng 4)]
        tools [(gen/latin rng 4)]
        cell (volatile! "")
        _ (with-redefs
           [fetch/fetch (fn [_ _ _ _ _ _ _ _ _ note]
                          (vreset! cell note)
                          {:body ""
                           :cells []
                           :links []})]
            (collect/collect
             chat
             part
             core
             kit
             model
             turns
             tokens
             tags
             tools
             head
             [{:depth 1
               :text text}]
             topic))
        want (str/trim (subs lang (count "Response language:")))
        want (if (str/ends-with? want ".")
               (subs want 0 (dec (count want)))
               want)
        data (edn/read-string @cell)
        check (and (= topic (:topic data))
                   (= text (:section-title data))
                   (= text (:section-details data))
                   (= want (:response-language data))
                   (string? (:scope data))
                   (string? (:heading-guidance data))
                   (string? (:task data))
                   (str/includes? (:task data) "deep research"))]
    (is check "prompt text did not match expected format")))

(deftest the-collector-expands-nested-items
  (let [rng (gen/ids 18318)
        topic (gen/greek rng 6)
        alpha (gen/arabic rng 5)
        beta (gen/hebrew rng 6)
        gamma (gen/hiragana rng 4)
        delta (gen/armenian rng 5)
        lang (str "Response language: " (gen/greek rng 4) ".")
        head [lang "" topic]
        chat (gen/latin rng 4)
        part (gen/latin rng 5)
        core (gen/latin rng 6)
        kit {:id (gen/latin rng 3)}
        model (gen/latin rng 4)
        turns (inc (.nextInt rng 4))
        tokens (inc (.nextInt rng 100))
        tags [(gen/latin rng 4)]
        tools [(gen/latin rng 4)]
        data (with-redefs
              [fetch/fetch (fn [_ _ _ _ _ _ _ _ _ _]
                             {:body ""
                              :cells []
                              :links []})]
               (collect/collect
                chat
                part
                core
                kit
                model
                turns
                tokens
                tags
                tools
                head
                [{:depth 1
                  :text alpha}
                 {:depth 2
                  :text beta}
                 {:depth 2
                  :text gamma}
                 {:depth 1
                  :text delta}]
                topic))
        list (mapv edn/read-string (:prompts data))
        keep (set (map :section-title list))
        check (and (= 3 (count list))
                   (= beta (:section-title (nth list 0)))
                   (= (str "Context: "
                           alpha
                           "\nFocus: "
                           beta)
                      (:section-details (nth list 0)))
                   (= gamma (:section-title (nth list 1)))
                   (= (str "Context: "
                           alpha
                           "\nFocus: "
                           gamma)
                      (:section-details (nth list 1)))
                   (= delta (:section-title (nth list 2)))
                   (= delta (:section-details (nth list 2)))
                   (not (contains? keep alpha)))]
    (is check "Nested items were not expanded into leaf prompts")))
