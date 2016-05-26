(ns oak.oracle.higher-order
  "Functions for constructing Oracles from sub-Oracles."
  (:require
    [oak.oracle :as oracle]
    [oak.internal.utils :as util]))

(defn parallel
  [oracle-map]
  (oracle/make
    :step
    (fn parallel-step [[index action] model]
      (update model index (oracle/step (get oracle-map index) action)))

    :start
    (fn parallel-start [submit]
      (util/map-kvs
        (fn [index subo]
          (oracle/start subo (fn [action] (submit [index action]))))
        oracle-map))

    :stop
    (fn parallel-stop [rts-map]
      (util/map-kvs
        (fn [index subo]
          (oracle/stop subo (get rts-map index)))
        oracle-map))

    :respond
    (fn parallel-respond [model [index query]]
      (oracle/respond (get oracle-map index) (get model index) query))

    :refresh
    (fn parallel-refresh [model queries submit]
      (let [querysets (reduce
                        (fn [sets [index subquery]]
                          (update sets index conj subquery))
                        {} queries)]
        (for [[index local-queries] querysets]
          (let [local-oracle (get oracle-map index)
                local-model (get model index)
                local-submit (fn [action] (submit [index action]))]
            (oracle/refresh local-oracle local-model local-queries local-submit)))))))
