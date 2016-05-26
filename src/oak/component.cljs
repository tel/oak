(ns oak.component
  (:require
    [quiescent.core :as q]))

; -----------------------------------------------------------------------------
; Protocol and type

(defprotocol IComponent
  (stepf [this])
  (factory [this]))

(defprotocol IQueryComponent
  (queryf [this]))

(defn query
  ([it] (queryf it))
  ([it model q] ((queryf it) model q)))

(defn step
  ([it] (stepf it))
  ([it action] (fn transition-fn [model] (step it action model)))
  ([it action model] ((stepf it) action model)))

(deftype Component
  [stepf factory]

  IComponent
  (stepf [_] stepf)
  (factory [_] factory)

  IFn
  (-invoke [_ model submit]
    (factory model submit)))

(deftype QueryComponent
  [stepf queryf factory]

  IComponent
  (stepf [_] stepf)
  (factory [_] factory)

  IQueryComponent
  (queryf [_] queryf)

  IFn
  (-invoke [_ model result submit]
    (factory model result submit)))

; -----------------------------------------------------------------------------
; Introduction

(def +oak-option-keys+
  [:step :view :query])

(def +default-options+
  {:step  (fn default-step [_action model] model)

   ; If no query is provided then we will, by default, construct a basic
   ; Component. If one *is* provided we expect that the view function will
   ; receive the result value and then generate a QueryComponent.
   :query nil

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
        {:keys [build-factory step query]} options
        factory (build-factory options)]
    (if (nil? query)
      (Component. step factory)
      (QueryComponent. step query factory))))

(defn make [& {:as options}] (make* options))
