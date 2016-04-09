(ns irony.cs.counters
  (:require
    [irony.elm :as elm]
    [schema.core :as s]
    [cljs.core.match :refer-macros [match]]
    [irony.elm.dom :as d]))

(defn One-init
  [initial-count]
  initial-count)

(def Single
  (elm/make
    :name "Counter-Single"
    :model s/Int
    :action (s/enum :inc :dec)

    :update
    (fn updater [action model]
      (case action
        :inc (inc model)
        :dec (dec model)))

    :view
    (fn viewer [model dispatch]
      (letfn [(clicker [action body]
                (d/button {:onClick (fn [_] (dispatch action))} body))]
        (d/div {}
          (clicker :dec "-")
          (d/div {} (str model))
          (clicker :inc "+"))))))

(defn WithControls-init
  [initial-count]
  initial-count)

(def WithControls
  (elm/make
    :name "Counter-Single-Controlled"
    :model (elm/model Single)
    :action (s/cond-pre
              (s/eq :remove)
              (elm/action Single))
    :update
    (fn updater [action model]
      (case action
        :remove model
        (elm/updatef Single action model)))
    :view
    (fn viewer [model dispatch]
      (letfn [(clicker [action body]
                (d/button {:onClick (fn [_] (dispatch action))} body))]
        (d/div {}
          (clicker :remove "Remove")
          (Single model dispatch))))))

(def Set-init [])

(defn ^:private remove-at-index
  "Remove from a vector at a particular index"
  [v i]
  (persistent!
    (reduce conj!
            (transient
              (vec (subvec v 0 i)))
            (subvec v (inc i)))))

(def Set
  (elm/make
    :name "Counter-Set"
    :model [(elm/model WithControls)]
    :action (s/cond-pre
              (s/eq :new)
              [(s/one s/Int :target) (elm/action WithControls)])
    :update
    (fn updater [action model]
      (match action
        :new (conj model (WithControls-init 0))

        [target :remove]
        (remove-at-index model target)

        [target inner-action]
        (update
          model target
          (elm/updatef WithControls inner-action))))
    :view
    (fn viewer [model dispatch]
      (letfn [(clicker [action body]
                (d/button {:onClick (fn [_] (dispatch action))} body))]
        (apply d/div {}
               (clicker :new "New Counter")
               (map-indexed
                 (fn [ix inner-model]
                   (WithControls inner-model #(dispatch [ix %])))
                 model))))))
