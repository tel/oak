(ns irony.elm
  (:refer-clojure :exclude [reify update])
  (:require
    [schema.core :as s]
    [quiescent.core :as q]
    [irony.elm.dom :as d]))

; -----------------------------------------------------------------------------
; Protocols and types

(defprotocol IElm
  (model [this]
         "The 'model' of an Elm component is a Schema describing the shape of
          the (immutable) data which substantiates this component.")
  (action [this]
          "The 'action' of an Elm component is a Schema describing the shape
          of messages emitted from and routed to the 'update' function of
          this component. These actions indicate state changes occurring
          over the component's model.")
  (update [this]
          "The 'update' mechanism of an Elm component relates a state
          transition to each valid action. It's a 'reducer' over streams of
          actions producing a trajectory through model space.")
  (view [this]
        "The 'view' function describes the virtual dom interpretation of the
        model space. It is a function from a model state and a 'dispatcher'
        function to a React virtual dom tree."))

(defn updatef
  "Given a component and an action for that component produce this component's
  model transition function over that action. If the model is also provided
  then the result model is returned."
  ([this action] (fn [model] ((update this) action model)))
  ([this action model] ((update this) action model)))

(deftype Component [model action update view]
  IElm
  ; A component is just a concrete description of the IElm interface.
  (model [_] model)
  (action [_] action)
  (update [_] update)
  (view [_] view)

  IAssociative
  (-contains-key? [_ k] (#{:model :action :update :view} k))
  (-assoc [_ k v]
    (case k
      :model (Component. v action update view)
      :action (Component. model v update view)
      :update (Component. model action v view)
      :view (Component. model action update v)))

  IFn
  ; We let the invocation of components run their view function since this is
  ; the most popular place for a component to be used.
  (-invoke [_ state dispatch] (view state dispatch)))

(defn map->Component [{:keys [model action update view]}]
  (Component. model action update view))

(defn reify
  "Reify any type implementing IElm into a Elm Component record type. This is
   useful if an implementer of the protocol either does some work whenever
   the IElm methods are called and you'd like to cache it OR when you'd like
   to get the IFn behavior of a fully instantiated Component value."
  [it]
  (map->Component
    {:model (model it)
     :action (action it)
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
  [& {:keys [model action update view]
      :or {model s/Any
           action s/Any
           update (fn [_ s] s)}
      :as options}]
  (let [component-keys [:model :action :update :view]
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
                              (subc subm subd)))
            view-set (map-keys build-subview routes)]
        (view-composition view-set dispatch)))))

