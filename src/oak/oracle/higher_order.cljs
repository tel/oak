(ns oak.oracle.higher-order
  "Functions for constructing Oracles from sub-Oracles."
  (:require
    [schema.core :as s]
    [schema.core :as s]
    [oak.oracle :as oracle]
    [oak.internal.utils :as util]
    [oak.schema :as os]))

(defn parallel
  [oracle-map]
  (oracle/make
    :model (util/map-vals oracle/model oracle-map)
    :action (apply os/cond-pair (util/map-vals oracle/action oracle-map))
    :query (apply os/cond-pair (util/map-vals oracle/query oracle-map))

    :step
    (fn [[index action] model]
      (update model index (oracle/step (get oracle-map index) action)))

    :respond
    (fn [model [index query]]
      (oracle/respond (get oracle-map index) (get model index) query))

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
