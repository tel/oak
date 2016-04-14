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
    (fn updater [action state]
      (case action
        :inc (inc state)
        :dec (dec state)))

    :view
    (fn viewer [{:keys [state]} ctx]
      (letfn [(clicker [action body]
                (d/button {:onClick (fn [_] (elm/act ctx action))} body))]
        (d/div {}
          (clicker :dec "-")
          (d/div {} (str state))
          (clicker :inc "+"))))))

(defn WithControls-init
  [initial-count]
  initial-count)

(def WithControls
  (elm/make
    :name "Counter-Single-Controlled"
    :model (elm/model Single)
    :queries {:foo 1
              :bar 2}
    :action (s/cond-pre
              (s/eq :remove)
              (elm/action Single))
    :update
    (fn updater [action state]
      (case action
        :remove state
        (elm/updatef Single action state)))
    :view
    (fn viewer [{:keys [state]} ctx]
      (letfn [(clicker [action body]
                (d/button {:onClick (fn [_] (elm/act ctx action))} body))]
        (d/div {}
          (clicker :remove "Remove")
          (Single state ctx))))))

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
    (fn updater [action state]
      (match action
        :new (conj state (WithControls-init 0))

        [target :remove]
        (remove-at-index state target)

        [target inner-action]
        (update
          state target
          (elm/updatef WithControls inner-action))))

    :view
    (fn viewer [{:keys [state] :as props} ctx]
      (letfn [(clicker [action body]
                (d/button {:onClick (fn [_] (elm/act ctx action))} body))]
        (apply d/div {}
               (clicker :new "New Counter")
               (map-indexed
                 (fn [ix inner-state]
                   (WithControls
                     inner-state
                     (elm/address-to ctx (fn [action] [ix action]))))
                 state))))))
