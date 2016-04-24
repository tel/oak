(ns oak.component
  (:require
    [schema.core :as s]
    [quiescent.core :as q]))

; -----------------------------------------------------------------------------
; Protocol and type

(defprotocol IComponent
  (model [this])
  (action [this])
  (queryf [this])
  (stepf [this])
  (factory [this]))

(deftype Component
  [model action stepf queryf factory]

  IComponent
  (model [_] model)
  (action [_] action)
  (queryf [_] queryf)
  (stepf [_] stepf)
  (factory [_] factory)

  IFn
  (-invoke [_] (factory {} (fn [_])))
  (-invoke [_ st] (factory {:model st} (fn [_])))
  (-invoke [_ st submit] (factory {:model st} submit))
  (-invoke [_ st result submit] (factory {:model st :result result} submit)))

(defn query
  [it model q] ((queryf it) model q))

(defn step
  ([it action] (fn transition-fn [model] (step it action model)))
  ([it action model] ((stepf it) action model)))

; -----------------------------------------------------------------------------
; Introduction

(def +oak-option-keys+
  [:model :action :step :view :query])

(def +default-options+
  {:model (s/eq nil)
   :action (s/cond-pre) ; never succeeds
   :query (fn [_model _q] nil)
   :step  (fn default-step [_action model] model)

   ; By default we use Quiescent, but we're not really married to it in any way.
   ; If you can build a factory in any way, e.g. a function from two args,
   ; the model and the submit function, then you're good! For best
   ; performance, use a Quiescent-style shouldComponentUpdate which assumes
   ; the first arg is all of the state.
   :build-factory
   (fn [{:keys [view] :as options}]
     (let [quiescent-options (apply dissoc options +oak-option-keys+)]
       (q/component view quiescent-options)))})

(defn make* [options]
  (let [options (merge +default-options+ options)
        {:keys [build-factory model action step query]} options
        factory (build-factory options)
        action-validator (s/validator action)
        model-validator (s/validator model)
        validated-step (fn validated-step [action model]
                         (model-validator
                           (step (action-validator action) model)))]
    (Component. model action validated-step query factory)))

(defn make [& {:as options}] (make* options))
