(ns oak.component
  (:require
    [schema.core :as s]
    [quiescent.core :as q]))

; -----------------------------------------------------------------------------
; Protocol and type

(defprotocol IComponent
  (state [this])
  (event [this])
  (queryf [this])
  (stepf [this])
  (factory [this]))

(deftype Component
  [state event stepf mergef queryf factory]

  IComponent
  (state [_] state)
  (event [_] event)
  (queryf [_] queryf)
  (stepf [_] stepf)
  (factory [_] factory)

  IFn
  (-invoke [_] (factory (mergef nil nil) (fn [_])))
  (-invoke [_ st] (factory (mergef st nil) (fn [_])))
  (-invoke [_ st submit] (factory (mergef st nil) submit))
  (-invoke [_ st results submit] (factory (mergef st results) submit)))

(defn query
  [it state q] ((queryf it) state q))

(defn step
  ([it event] (fn transition-fn [state] (step it event state)))
  ([it event state] ((stepf it) event state)))

; -----------------------------------------------------------------------------
; Introduction

(def +oak-option-keys+
  [:state :event :step :view :merge :query])

(def +default-options+
  {:state (s/eq nil)
   :event (s/cond-pre) ; never succeeds
   :query (fn [_state _q] nil)
   :merge (fn default-merge [state results] (if results [state results] state))
   :step  (fn default-step [_event state] state)

   ; By default we use Quiescent, but we're not really married to it in any way.
   ; If you can build a factory in any way, e.g. a function from two args,
   ; the state and the submit function, then you're good! For best
   ; performance, use a Quiescent-style shouldComponentUpdate which assumes
   ; the first arg is all of the state.
   :build-factory
          (fn [{:keys [view] :as options}]
            (let [quiescent-options (apply dissoc options +oak-option-keys+)]
              (q/component view quiescent-options)))})

(defn make* [options]
  (let [options (merge +default-options+ options)
        {:keys [build-factory state event step merge query]} options
        factory (build-factory options)
        event-validator (s/validator event)
        state-validator (s/validator state)
        validated-step (fn validated-step [event state]
                         (state-validator
                           (step (event-validator event)
                                 (state-validator state))))]
    (Component. state event validated-step merge query factory)))

(defn make [& {:as options}] (make* options))
