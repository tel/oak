(ns irony.els.Counter
  (:require
    [irony.elm :as elm]
    [schema.core :as s]
    [irony.dom :as d]))

(def count-style
  {:fontSize "20px"
   :fontFamily "monospace"
   :display "inline-block"
   :width "50px"
   :textAlign "center"})

(defn updater [action model]
  (case action
    :inc (inc model)
    :dec (dec model)))

(defn viewer [model dispatch]
  (d/div {}
    (d/button
      {:onClick (fn [_] (dispatch :dec))}
      "-")
    (d/div
      {:style count-style}
      (str model))
    (d/button
      {:onClick (fn [_] (dispatch :inc))}
      "+")))

(def root
  (elm/make
    :name "Counter"
    :model s/Int
    :action (s/enum :inc :dec)
    :init 0
    :update updater
    :view viewer))
