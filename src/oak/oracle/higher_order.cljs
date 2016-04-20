(ns oak.oracle.higher-order
  "Functions for constructing Oracles from sub-Oracles."
  (:require
    [schema.core :as s]
    [schema.core :as s]
    [oak.component :as oak]
    [oak.oracle :as oracle]
    [oak.internal.utils :as util]))

(defn parallel
  [oracle-map]
  (oracle/make
    :state (util/map-vals oracle-map oak/state)
    :event (apply s/cond-pre
                  (map (fn [[k v]] (s/pair k :index (oak/event v) :subevent))
                       oracle-map))

    :step
    (fn [[index event] state]
      (update state index (oak/step (get oracle-map index) event)))

    :respond
    (fn [[index query]]
      (oracle/respond (get oracle-map index) query))

    :refresh
    (fn [state queries submit]
      (let [querysets (reduce
                        (fn [sets [index subquery]]
                          (update sets index conj subquery))
                        {} queries)]
        (for [[index local-queries] querysets]
          (let [local-oracle (get oracle-map index)
                local-state (get state index)
                local-submit (fn [event] (submit [index event]))]
            (oracle/refresh local-oracle local-state local-queries local-submit)))))))
