(ns research.api.xai.bridge.collect
  (:require [clojure.string :as str]
            [research.api.xai.bridge.fetch :as fetch]))

(defn collect
  "Collect response data for multi prompt."
  [chat part core kit model turns tokens tags tools head items top]
  (loop [parts [] marks [] links [] items items]
    (if (seq items)
      (let [item (first items)
            note (if (and (seq top)
                          (not
                           (str/includes?
                            (str/lower-case item)
                            (str/lower-case top))))
                   (str top " - " item)
                   item)
            pos (last (keep-indexed
                       (fn [idx line]
                         (when (not
                                (str/blank?
                                 (str/trim line)))
                           idx))
                       head))
            base (if (some? pos)
                   (assoc head pos note)
                   (conj head note))
            prompt (str/trim (str/join "\n" base))
            data (fetch/fetch chat part core kit model turns tokens tags tools
                              prompt)
            body (:body data)
            cells (:cells data)
            refs (:links data)
            rows (str/split-lines body)
            rows (map
                  (fn [row]
                    (let [trim (str/triml row)]
                      (if (str/starts-with? trim "#")
                        (str "##"
                             (str/replace
                              trim
                              #"^#+"
                              ""))
                        row)))
                  rows)
            body (str/trim (str/join "\n" rows))
            links (concat links refs)
            parts (conj parts (str "# " prompt "\n\n" body))
            marks (into marks cells)]
        (recur parts marks links (rest items)))
      {:parts parts
       :marks marks
       :links links})))
