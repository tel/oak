(ns oak.core
  (:require
    [schema.core :as s]
    [oak.dom :as d]
    [quiescent.core :as q]))

; -----------------------------------------------------------------------------
; Utilities

(defn ^:private map-vals [f hashmap]
  (into {} (map (fn [p] [(key p) (f (val p))])) hashmap))

; -----------------------------------------------------------------------------
; Protocol and type

(defprotocol IComponent
  (state [this])
  (event [this])
  (query [this])
  (stepf [this])
  (factory [this]))

(deftype Component
  [state event stepf mergef query factory]

  IComponent
  (state [_] state)
  (event [_] event)
  (query [_] query)
  (stepf [_] stepf)
  (factory [_] factory)

  IFn
  (-invoke [this state] (-invoke this state (fn [_])))
  (-invoke [this state submit] (factory state submit))
  (-invoke [this state results submit] (factory (mergef state results) submit)))

; -----------------------------------------------------------------------------
; Introduction

(def +default-options+
  {:state (s/eq nil)
   :event (s/cond-pre) ; never succeeds
   :merge (fn default-merge [state results] (if results [state results] state))
   :step (fn default-step [_event state] state)})

(def +oak-option-keys+
  [:state :event :step :view :merge :query])

(defn make [& {:as options}]
  (let [options (merge +default-options+ options)
        {:keys [state event step view merge query]} options
        quiescent-options (apply dissoc options +oak-option-keys+)
        factory (q/component view quiescent-options)]
    (Component. state event step merge query factory)))

; -----------------------------------------------------------------------------
; Examples
;
;(def c1
;  (make
;    :requests
;    (fn [state q]
;      {:name (q [:name state])})
;    :view
;    (fn [state submit responses]
;      (d/pre {}
;        (str {:state state :responses responses})))))
;
;(def c2
;  (make
;    :requests
;    (fn [state q]
;      {:a (reqs c1 (:a state) q)
;       :b (reqs c1 (:b state) q)})
;    :view
;    (fn [state submit responses]
;      (d/div {}
;        (c1 (:a state) submit (:a responses))
;        (c1 (:b state) submit (:b responses))))))
;
;(defn qrender [c]
;  (q/render c (.getElementById js/document "app"))
;  nil)
;
;(defn get-reqs [c state]
;  (let [cache (atom [])]
;    ((.-requests c) state (partial swap! cache conj))
;    @cache))
;
;(defn main []
;  (let [c c2
;        submit (fn [event] (println event))
;        state {:a :payal :b :joseph}
;        db0 {}
;        db1 {[:name :joseph] "Joseph Abrahamson"
;             [:name :payal] "Payal Patnaik"}
;        responses0 ((.-requests c) state #(get db0 % :not-found))
;        responses1 ((.-requests c) state #(get db1 % :not-found))]
;    (println (get-reqs c state))
;    (qrender (c state submit responses0))
;    (println (get-reqs c state))
;    (qrender (c state submit responses1))))
;
;;; -----------------------------------------------------------------------------
;;; Queries
;;
;;;(defprotocol IOracle
;;;  "Implementors of IOracle can be inquired about the world. Each view cycle
;;;   potentially offers a new set of answers from the Oracle."
;;;  (q [this query] "Execute a query returning a result."))
;;;
;;;(defprotocol IAddress
;;;  "An address is a sink for messages, in particular *events*."
;;;  (act [this event] "Send an event to the address.")
;;;  (route [this fn] "Transform messages before sending them onward."))
;;
;;; -----------------------------------------------------------------------------
;;; Intros
;;
;;(defprotocol IComponent
;;  "The public interface of Component types."
;;  (state [this] "Get the schema describing this component's state.")
;;  (event [this] "Get the schema describing this component's event.")
;;  (step [this] "Get the state transition 'step' function for this component."))
;;
;;(deftype Component
;;  [state    ; Schema describing the state for this component
;;   event   ; Schema describing the events for this component
;;   step     ; Function (A, M) -> M indicating a state update
;;   view     ; Function from {:state state :queries queries :result results}
;;            ; to a React virtual dom tree.
;;   factory  ; A Quiescent factory for this component
;;   queries  ; Map from local names to query fragments or functions from the
;;            ; state to a query fragment; queries are attempted on view.
;;            ; While, in theory, you can use the IOracle protocol to execute
;;            ; arbitrary queries on the context, only ones registered here
;;            ; will cause re-renders.
;;   ]
;;
;;  IComponent
;;  (state [_] state)
;;  (event [_] event)
;;  (step [_] step)
;;
;;  IFn
;;  (-invoke [_ the-state the-context]
;;    (let [reified-queries (queries the-state)
;;          captured-queries (atom [])
;;          result (factory
;;                   the-state
;;                   (assoc
;;                     the-context
;;                     :report-queries #(swap! captured-queries concat %)))]
;;      ((:report-queries the-context) (concat (vals reified-queries) captured-queries))
;;      result)))
;;
;;(defn stepf
;;  ([comp event] (fn [state] (stepf comp event state)))
;;  ([comp event state] ((step comp) event state)))
;;
;;; -----------------------------------------------------------------------------
;;; Intros
;;
;;(def +default-options+
;;  {:state (s/eq nil)
;;   :event (s/cond-pre) ; never succeeds
;;   :step (fn default-step [_event state] state)
;;   :queries {}})
;;
;;(defn make [& {:as options}]
;;  (let [{:keys [state event step view queries]
;;         :as refined-options} (merge +default-options+ options)
;;        fixed-view (fn [{:keys [state query-cache]} ctx]
;;                     (view state (assoc ctx :cache query-cache)))
;;        factory (q/component fixed-view refined-options)]
;;    (Component. state event step view factory queries)))
;;
;;; -----------------------------------------------------------------------------
;;; Examples
;;
;;(defn qrender [c] (q/render c (.getElementById js/document "app")))
;;
;;(defn q [ctx q]
;;  (assoc (get (:cache ctx) q {:pending true})
;;    :query q))
;;
;;(defn add-q [ctx q] (swap! (:seen ctx) conj q))
;;
;;(defn rndr [^Component c state ctx]
;;  (let [result ((.-factory c) {:state state :query-cache (:cache ctx)} ctx)
;;        queries (.-queries c)]
;;    (when queries
;;      (doseq [q (vals (queries state))]
;;        (add-q ctx q)))
;;    result))
;;
;;(def c1
;;  (make
;;    :name "C1"
;;    :state (s/enum :dog :cat)
;;    :queries (fn [state]
;;               {:a [:animal state]})
;;    :view
;;    (fn [state ctx]
;;      (d/div {} (str (q ctx [:animal state]))))))
;;
;;(def c2
;;  (make
;;    :name "C2"
;;    :queries (fn [_state] {:a [:phase]})
;;    :view
;;    (fn [_ ctx]
;;      (d/div {}
;;        (str (q ctx [:phase]))
;;        (rndr c1 :dog ctx)
;;        (rndr c1 :cat ctx)))))
;;
;;(defn main []
;;  (let [c c2
;;        ctx1 {:seen (atom {})
;;              :cache {[:animal :cat] {:value 3}
;;                      [:animal :dog] {:value 4}}}
;;        ctx2 {:seen (atom {})
;;              :cache {[:animal :cat] {:value 3}
;;                      [:animal :dog] {:value 4}
;;                      [:phase] {:value :waning}}}]
;;    (qrender (rndr c nil ctx1))
;;    (qrender (rndr c nil ctx2))
;;    (println "Qs 1" @(:seen ctx1))
;;    (println "Qs 2" @(:seen ctx2))))
;;
;;;(defn main []
;;;  (let [c c2
;;;        qsys (make-query-fn (fn [[_ x]] (* 2 x)))
;;;        state nil
;;;        looper-fn (atom)
;;;        context (reify
;;;                  IAddress
;;;                  (act [_ _] (@looper-fn))
;;;                  (route [this _] this)
;;;                  IOracle
;;;                  (q [_ q]
;;;                    (let [result (qsys q)]
;;;                      (println "querying" q result)
;;;                      result)))
;;;        looper (fn []
;;;                 (println "rendering...")
;;;                 (q/render (c state context) +mount-point+))]
;;;    (reset! looper-fn looper)
;;;    (looper)))
;;
;;; arbitrary structure of query data; duplication of sub-state capture code;
;;; explicit subquery embedding; must know how to deconstruct subqueries; is
;;; this painful? state-constant query-needing child nodes are painful (this
;;; breaks the state-deconstruction parallelism)
;;
;;; Really what we're talking about here is a top-to-bottom request-response
;;; system. Why is this not subsumed in the :events? It *could* be, but (a)
;;; we'd like to separate to make application state different from interface
;;; state and (b) most of these interevents will shoot straight to the top.

