(ns oak.examples.counters
  (:require
    [cljs.core.match :refer-macros [match]]
    [devcards.core :as devcards :include-macros true]
    [oak.component :as oak]
    [oak.experimental.devcards :as oakdc]
    [oak.experimental.devcards.ui :as oakdc-ui]
    [oak.dom :as d]))

(def counter
  (oak/make
    :step
    (fn [action model]
      (case action
        :inc (inc model)
        :dec (dec model)))
    :view
    (fn [model submit]
      (let [clicker (fn clicker [action] (fn [_] (submit action)))]
        (d/div {}
          (d/button {:onClick (clicker :dec)} "-")
          (d/span {} model)
          (d/button {:onClick (clicker :inc)} "+"))))))

(def counter-with-controls
  (oak/make
    :step (fn [action model]
            (case action
              :del model
              (oak/step counter action model)))
    :view (fn [model submit]
            (d/div {:style {:padding "10px 4px"}}
              (counter model submit)
              (d/button {:onClick (fn [_] (submit :del))} "Delete")))))

(defn ^:private delete-from-vec [v n]
  (persistent!
    (reduce
      conj!
      (transient (vec (subvec v 0 n)))
      (subvec v (inc n)))))

(def counter-set
  (let [counter counter-with-controls]
    (oak/make
      :step
      (fn [action model]
        (match action
          :new (conj model 0)
          [index :del] (delete-from-vec model index)
          [index inner-action] (update model index
                                       (oak/step counter inner-action))))

      :view
      (fn [model submit]
        (d/div {}
          (d/button {:onClick (fn [_] (submit :new))} "New Counter")
          (apply d/div {}
                 (for [[index submodel] (map-indexed (fn [i v] [i v]) model)]
                   (counter submodel (fn [ev] (submit [index ev]))))))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defonce actions (atom []))

(declare single-counter)
(devcards/defcard single-counter
  (oakdc/render
    counter-set
    {:on-action (fn [domain action]
                  (swap! actions conj [domain action]))})
  [])

(declare action-set)
(devcards/defcard action-set
  (oakdc/render oakdc-ui/action-list)
  actions)
