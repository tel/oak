(ns oak.core
  (:require
    [schema.core :as s]
    [oak.dom :as d]
    [quiescent.core :as q]))

(defn ^:private map-vals [f hashmap]
  (into {} (map (fn [p] [(key p) (f (val p))])) hashmap))

; -----------------------------------------------------------------------------
; Queries

(defprotocol IOracle
  "Implementors of IOracle can be inquired about the world. Each view cycle
   potentially offers a new set of answers from the Oracle."
  (q [this query] "Execute a query returning a result."))

(defprotocol IAddress
  "An address is a sink for messages, in particular *actions*."
  (act [this action] "Send an action to the address.")
  (route [this fn] "Transform messages before sending them onward."))

; -----------------------------------------------------------------------------
; Intros

(defprotocol IComponent
  "The public interface of Component types."
  (model [this] "Get the schema describing this component's model.")
  (action [this] "Get the schema describing this component's action.")
  (step [this] "Get the state transition 'step' function for this component."))

(deftype Component
  [model    ; Schema describing the model for this component
   action   ; Schema describing the actions for this component
   step     ; Function (A, M) -> M indicating a model update
   view     ; Function from {:model model :queries queries :result results}
            ; to a React virtual dom tree.
   factory  ; A Quiescent factory for this component
   queries  ; Map from local names to query fragments or functions from the
            ; model to a query fragment; queries are attempted on view.
            ; While, in theory, you can use the IOracle protocol to execute
            ; arbitrary queries on the context, only ones registered here
            ; will cause re-renders.
   ]

  IComponent
  (model [_] model)
  (action [_] action)
  (step [_] step)

  IFn
  (-invoke [_ the-model the-context]
    (let [fixed-queries (map-vals
                          (fn [v] (if-not (fn? v) v (v the-model)))
                          queries)
          results (map-vals (partial q the-context) fixed-queries)]
      (factory
        {:model   the-model
         :queries fixed-queries
         :results results}
        the-context))))

(defn stepf
  ([comp action] (fn [model] (stepf comp action model)))
  ([comp action model] ((step comp) action model)))

; -----------------------------------------------------------------------------
; Intros

(def +default-options+
  {:model (s/eq nil)
   :action (s/cond-pre) ; never succeeds
   :step (fn default-step [_action model] model)
   :queries {}})

(defn make [options]
  (let [{:keys [model action step view queries]
         :as refined-options} (merge +default-options+ options)
        factory (q/component view refined-options)]
    (Component. model action step view factory queries)))

; -----------------------------------------------------------------------------
; Examples

(defn make-query-fn [f]
  (let [cache (atom {})]
    (fn [q]
      (if-let [[_ val] (find @cache q)]
        {:value val :meta :complete}
        (do
          (swap! cache assoc q (f q))
          {:meta :pending})))))

(def c1
  (make
    {:name    "C1"
     :model   s/Int
     :queries {:b (fn [model] [:c1 model])}
     :view    (fn c1-renderer [in context]
                (println "rendering c1" in)
                (d/p {} (d/pre {} (str in))))}))

(def c2
  (make
    {:name    "C2"
     :queries {:a [:c2 1]}
     :action (s/eq :step)
     :view    (fn c2-renderer [{:keys [results] :as in} context]
                (println "rendering c2" in)
                (let [{:keys [a b]} model
                      children [(d/button
                                  {:type :button
                                   :onClick (fn [_] (act context :step))}
                                  "Hello")
                                (c1 100 context)
                                (c1 200 context)
                                (d/p {} "test")]
                      children (concat
                                 children
                                 (for [ix (range (get (:a results) :value 5))]
                                   (c1 ix context)))]
                  (apply d/div {} children)))}))

(def +mount-point+
  (.getElementById js/document "app"))

(defn main []
  (let [c c2
        qsys (make-query-fn (fn [[_ x]] (* 2 x)))
        model nil
        looper-fn (atom)
        context (reify
                  IAddress
                  (act [_ _] (@looper-fn))
                  (route [this _] this)
                  IOracle
                  (q [_ q]
                    (let [result (qsys q)]
                      (println "querying" q result)
                      result)))
        looper (fn []
                 (println "rendering...")
                 (q/render (c model context) +mount-point+))]
    (reset! looper-fn looper)
    (looper)))
