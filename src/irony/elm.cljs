(ns irony.elm
  (:refer-clojure :exclude [update])
  (:require
    [schema.core :as s]
    [quiescent.core :as q]
    [irony.dom :as d]))

; -----------------------------------------------------------------------------
; Protocols and types

(defprotocol IElm
  (model [this])
  (action [this])
  (init [this])
  (update [this])
  (view [this]))

(defn updatef [this action model] ((update this) action model))
(defn viewf [this model dispatch] ((view this) model dispatch))

(defrecord Component [model action init update view]
  IElm
  (model [_] model)
  (action [_] action)
  (init [_] init)
  (update [_] update)
  (view [_] view))

(defn as-record
  "Reify any type implementing IElm into a Elm Component record type."
  [it]
  (map->Component
    {:model (model it)
     :action (action it)
     :init (init it)
     :update (update it)
     :view (view it)}))

; -----------------------------------------------------------------------------
; Component intros

(defn make
  "Construct a Elm-like component by defining the model, view, and update.
  This function wraps Quiescent's component construction function allowing
  you to specify React lifecycle callbacks and metadata according to that
  convention. See Quiescent's documentation for details on the following
  extra options [:keyfn, :name, :on-mount, :on-update, :on-unmount,
  :on-render, :will-enter, :did-enter, :will-leave, :did-leave]."
  [& {:keys [model action init update view]
      :or {model s/Any
           action s/Any
           init nil
           update (fn [_ s] s)}
      :as options}]
  (let [component-keys [:model :action :init :update :view]
        quiescent-opts (apply dissoc options component-keys)
        quiescence (q/component view quiescent-opts)
        component-opts (assoc (select-keys options component-keys)
                         :view quiescence)]
    (map->Component component-opts)))

(defn ^:private map-keys [f hashmap]
  (into {} (map (fn [p] [(key p) (f (val p))])) hashmap))

(defn parallel
  "Construct a Component from a set of named Elm components operating 'in
  parallel'. Generally compositions of components are better performed by
  creating a new component which explicitly embodies the exact composition
  desired, but this combinator can be used in situations where the desired
  composition is very lightweight and close if not identical to the parallel,
  non-interacting composition of the subcomponents.

  A component constructed as (parallel routes) expects
  that routes is a map from keys to components. The resulting combination
  component operates by maintaining the models of each subcomponent in
  parallel. By default the views of each subcomponent are merged sequentially
  into a div and the actions are submitted completely independently of one
  another (no crosstalk). These last two behaviors can be customized,
  however, through the :view-composition and :update-transform keys.

  The :view-composition key is similar to a :view key in a normal component
  but with a special model and action built from the subcomponents: the view
  composition model is a map from the keys in your routes to fully-rendered
  views for each subcomponent and the dispatch operates on actions of form
  [key action] where key corresponds to a key in routes and action is a
  action of the form corresponding to the subcomponent at that key.

  The :update-transform key must be a function from update functions to
  update functions over actions of the same form as seen above in
  :view-composition and models of the form of a map from your routes keys to
  the models of each corresponding subcomponent. The 'original' update
  function received in the transform will pass each action along to its
  corresponding subcomponent model update function, but more sophisticated
  'wire crossing' update functions can be crafted here as necessary."

  [routes
   & {:keys [view-composition update-transform]
      :or {update-transform identity
           view-composition (fn [views _dispatch]
                              (apply d/div (vals views)))}}]

  (make
    :init (map-keys init routes)
    :model (map-keys model routes)

    :action
    (let [match-keyword-pred (fn [kw-here] (fn [pair] (= kw-here (key pair))))
          get-pred-and-schema (fn [pair] [(match-keyword-pred (key pair))
                                          (action (val pair))])
          preds-and-schemas (mapcat get-pred-and-schema routes)]
      (apply s/conditional preds-and-schemas))

    :update
    (let [update-set (map-keys update routes)]
      (update-transform
        (fn [[key action] model]
         (let [update-fn (get update-set key)]
           (cljs.core/update model key (partial update-fn action))))))

    :view
    (fn [model dispatch]
      (let [build-subview (fn [key]
                            (let [subc (get routes key)
                                  subm (get model key)
                                  subd (fn [action] (dispatch [key action]))]
                              (viewf subc subm subd)))
            view-set (map-keys build-subview routes)]
        (view-composition view-set dispatch)))))

