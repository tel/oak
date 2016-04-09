(ns irony.elm
  (:refer-clojure :exclude [reify])
  (:require
    [schema.core :as s]
    [quiescent.core :as q]
    [irony.dom :as d]))

(defprotocol IElm
  (state-schema [this])
  (action-schema [this])
  (base [this])
  (reducer [this])
  (renderer [this]))

(defn reduce [this action state]
  (let [reducer-fn (reducer this)]
    (reducer-fn action state)))

(defn render [this state submit]
  (let [render-fn (renderer this)]
    (render-fn state submit)))

(defn ^:private map-keys [f hashmap]
  (into {} (map (fn [p] [(key p) (f (val p))])) hashmap))

(extend-protocol IElm
  PersistentHashMap

  ; The product of all inner state schemata
  (state-schema [this] (map-keys state-schema this))

  ; The product of all the inner base states
  (base [this] (map-keys base this))

  ; Actions take the form [key inner-actions] where key indicates which inner
  ; Elm component will receive the action.
  (action-schema [this]
    (let [preds-and-schemas
          (mapcat
            (fn [[key-here inner]]
              (let [pred (fn [[key action]] (= key-here key))
                    schema (action-schema inner)]
                [pred schema]))
            this)]
      (apply s/conditional preds-and-schemas)))

  ; Each action is routed to each inner reducer which (in parallel) updates
  ; each component state
  (reducer [this]
    (let [reducer-set (map-keys reducer this)]
      (fn [[key action] state]
        (update state key (partial (get reducer-set key) action)))))

  ; The default render function just slaps all inner render results into a
  ; big div
  (renderer [this submit props]
    (let [render-map (map-keys renderer this)
          submit-map (into {} (map (fn [[k v]] (submit [k v]))) this)]
      (apply d/div
             (into []
                   (map (fn [[k this-props]]
                          (let [this-render (get render-map k)
                                this-submit (get submit-map k)]
                            (this-render this-props this-submit))))
                   props)))))

(defrecord Component [state action base reducer renderer]
  IElm
  (state-schema [_] state)
  (action-schema [_] action)
  (base [_] base)
  (reducer [_] reducer)
  (renderer [_] renderer))

(defn reify [it]
  (->Component
    (state-schema it)
    (action-schema it)
    (base it)
    (reducer it)
    (renderer it)))

(defn make
  [& {:keys [state action base reducer render]
      :or {state s/Any
           action s/Any
           base nil
           reducer (fn [_ s] s)}
      :as options}]
  (let [quiescent-opts (dissoc options :state :action :base :reducer :render)
        quiescence (q/component render quiescent-opts)]
    (->Component state action base reducer quiescence)))
