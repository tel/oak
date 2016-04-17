(ns oak.oracle
  "An Oracle is a system for determining, perhaps only eventually, answers to
  queries. For instance, a database is naturally a (synchronous) Oracle. So
  is a REST API, though this one is asynchronous.

  Oracles differ in the kinds of queries they respond to and the nature of
  their responses. They are the same in that they manage state in a way
  that's compatible with the explicit nature of Oak.

  In particular, an Oracle operates in stages. During the 'respond' stage,
  the Oracle answers queries to the best of its ability atop a fixed 'state'
  value. After the 'respond' stage the Oracle gets a chance to have a 'research'
  stage updating the 'state' value in knowledge of all of the queries it
  received during the 'respond' stage.

  Notably, an Oracle must usually respond even before doing any research such
  that asynchronous Oracles will probably return empty responses at first.
  Importantly, the 'respond' stage must be completely pure---no side effects
  allowed! All of the side effects occur during the 'research' phase offering a
  mechanism for asynchronous data loading."
  (:require
    [schema.core :as s]))

; -----------------------------------------------------------------------------
; Utilities

(defn ^:private map-vals [f hashmap]
  (into {} (map (fn [p] [(key p) (f (val p))])) hashmap))

; -----------------------------------------------------------------------------
; Type

(defprotocol IOracle
  (state [this])
  (event [this])
  (stepf [this])
  (respondf [this])
  (refreshf [this]))

(defn step
  ([oracle action] (fn [state] (step oracle action state)))
  ([oracle action state] ((stepf oracle) action state)))

(defn respond
  [oracle query] ((respondf oracle) query))

(defn refresh
  [oracle state queries submit] ((refreshf oracle) state queries submit))

; -----------------------------------------------------------------------------
; Type

(deftype Oracle
  [state event stepf respondf refreshf]

  IOracle
  (state [_] state)
  (event [_] event)
  (stepf [_] stepf)
  (respondf [_] respondf)
  (refreshf [_] refreshf))

; -----------------------------------------------------------------------------
; Intro

(defn make
  [& {:keys [state event step respond refresh]}]
  (Oracle. state event step respond refresh))

(defn parallel
  [oracle-map]
  (make
    :state (map-vals oracle-map state)
    :event (apply s/cond-pre
                  (map (fn [[k v]] (s/pair k :index (event v) :subevent))
                       oracle-map))
    :step
    (fn [[index event] state]
      (update state index (step (get oracle-map index) event)))
    :respond
    (fn [[index query]]
      (respond (get oracle-map index) query))
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
            (refresh local-oracle local-state local-queries local-submit)))))))

