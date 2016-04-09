(ns irony.els.CounterSet
  (:require
    [irony.elm :as elm]
    [schema.core :as s]
    [irony.dom :as d]
    [cljs.core.match :refer-macros [match]]
    [irony.els.Counter :as Counter]))

(defn updater [action model]
  (match action
    :new (conj model (Counter/init 0))

    [target inner-action]
    (update
      model target
      (elm/updatef Counter/root inner-action))))

(defn viewer [model dispatch]
  (apply d/div {}
         (d/button {:onClick (fn [_] (dispatch :new))} "New Counter")
         (map-indexed
           (fn [ix inner-model]
             (Counter/root inner-model #(dispatch [ix %])))
           model)))

(def root
  (elm/make
    :name "CounterSet"
    :model [(elm/model Counter/root)]
    :action (s/cond-pre
              (s/eq :new)
              [(s/one s/Int :target) (elm/action Counter/root)])
    :update updater
    :view viewer))

(def init [])
