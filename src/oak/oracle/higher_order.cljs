(ns oak.oracle.higher-order
  "Functions for constructing Oracles from sub-Oracles."
  (:require
    [schema.core :as s]
    [schema.core :as s]
    [oak.oracle :as oracle]
    [oak.internal.utils :as util]))

(defn parallel
  [oracle-map]
  (oracle/make
    :model (util/map-vals oracle-map oracle/model)
    :action (apply s/cond-pre
                   (map (fn [[k v]] (s/pair k :index (oracle/action v) :subaction))
                        oracle-map))

    :step
    (fn [[index action] model]
      (update model index (oracle/step (get oracle-map index) action)))

    :respond
    (fn [[index query]]
      (oracle/respond (get oracle-map index) query))

    :refresh
    (fn [model queries submit]
      (let [querysets (reduce
                        (fn [sets [index subquery]]
                          (update sets index conj subquery))
                        {} queries)]
        (for [[index local-queries] querysets]
          (let [local-oracle (get oracle-map index)
                local-model (get model index)
                local-submit (fn [action] (submit [index action]))]
            (oracle/refresh local-oracle local-model local-queries local-submit)))))))
