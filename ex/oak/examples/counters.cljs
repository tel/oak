(ns oak.examples.counters
  (:require
    [cljs.core.match :refer-macros [match]]
    [devcards.core :as devcards :include-macros true]
    [oak.core :as oak]
    [oak.experimental.devcards :as oak-devcards]
    [oak.dom :as d]
    [schema.core :as s]))

(def counter
  (oak/make
    :state s/Int
    :event (s/enum :inc :dec)
    :step
    (fn [event state]
      (case event
        :inc (inc state)
        :dec (dec state)))
    :view
    (fn [state submit]
      (let [clicker (fn clicker [event] (fn [_] (submit event)))]
        (d/div {}
          (d/button {:onClick (clicker :dec)} "-")
          (d/span {} state)
          (d/button {:onClick (clicker :inc)} "+"))))))

(def counter-with-controls
  (oak/make
    :state (oak/state counter)
    :event (s/cond-pre :del (oak/event counter))
    :step (fn [event state]
            (case event
              :del state
              (oak/step counter event state)))
    :view (fn [state submit]
            (d/div {:style {:padding "10px 4px"}}
              (counter state submit)
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
      :state [(oak/state counter)]
      :event (s/cond-pre
               (s/eq :new)
               (s/pair s/Int :index (oak/event counter) :subevent))

      :step
      (fn [event state]
        (match event
          :new (conj state 0)
          [index :del] (delete-from-vec state index)
          [index inner-event] (update state index
                                      (oak/step counter inner-event))))

      :view
      (fn [state submit]
        (d/div {}
          (d/button {:onClick (fn [_] (submit :new))} "New Counter")
          (apply d/div {}
                 (for [[index substate] (map-indexed (fn [i v] [i v]) state)]
                   (counter substate (fn [ev] (submit [index ev]))))))))))

(defonce event-queue
  (atom {:state #queue []}))

(declare single-counter)
(devcards/defcard single-counter
  (oak-devcards/render counter-set)
  {:state [] :cache {}}
  {:on-event (fn [ev]
               (swap! event-queue update
                      :state #(oak-devcards/add-new-event % ev)))})

(declare event-set)
(devcards/defcard event-set
  (oak-devcards/render oak-devcards/event-demo)
  event-queue)
