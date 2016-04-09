(ns irony.ui
  (:require
    [irony.elm :as elm]
    [irony.dom :as d]
    [schema.core :as s]))

(def root
  (elm/make
    :state s/Int
    :action s/Any
    :base 0
    :reducer (fn [_ state] (println "hi") (inc state))
    :render
    (fn [state submit]
      (d/span {:onClick (fn [_] (submit :inc))} (str "Count
      is "
                                                               state)))))
