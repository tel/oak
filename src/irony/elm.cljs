(ns irony.elm
  (:refer-clojure :exclude [reify update])
  (:require
    [schema.core :as s]
    [quiescent.core :as q]
    [irony.elm.dom :as d]))

(defn ^:private map-vals [f hashmap]
  (into {} (map (fn [p] [(key p) (f (val p))])) hashmap))

; -----------------------------------------------------------------------------
; Protocols and types


(defprotocol IContext
  "Methods expected on an Elm component context value."
  (address-to [this transform] "Return a new context such that actions sent
   to it are transformed as by 'transform' before being forwarded along the
   local dispatcher of this context.")
  (act [this action] "Delivers an action via the update cycle. Expect that
   any action sent on a context delivered to a component will be seen in its
   update function (unless it is intercepted by a parent).")
  (q [this query] "Returns the current known state of a query. Each query
   *must* be declared in the component's 'queries'."))

(defrecord Context [dispatch-fn queries-atom resolved-queries]
  IContext
  (address-to [this fn] (assoc this :dispatch-fn (comp dispatch-fn fn)))
  (act [_ action] (dispatch-fn action))
  (q [_ q]
    (swap! queries-atom cljs.core/update [q] (fnil inc 0))
    (get resolved-queries q {:meta {:resolved false}})))

(defn make-context
  ([dispatch] (->Context dispatch (atom {}) {}))
  ([dispatch queries-atom] (->Context dispatch queries-atom {})))

(def zero-context (make-context (fn [_action] nil)))

(defprotocol IComponent
  (model [this] "The 'model' of an Elm component is a Schema describing the
  shape of the (immutable) data which substantiates this component.")
  (action [this] "The 'action' of an Elm component is a Schema describing the
   shape of messages emitted from and routed to the 'update' function of this
   component. These actions indicate state changes occurring over the
   component's model.")
  (update [this] "The 'update' mechanism of an Elm component relates a state
  transition to each valid action. It's a 'reducer' over streams of actions
  producing a trajectory through model space.")
  (queries [this] "The 'queries' of an Elm component specify 'holes' in the
  data this component needs to render. These holes are not guaranteed to be
  filled at render time, but *may* be eventually.")
  (view [this] "The 'view' function describes the virtual dom interpretation
  of the model space. It is a function from a model state and a 'dispatcher'
  function to a React virtual dom tree."))

(defn updatef
  "Given a component and an action for that component produce this component's
  model transition function over that action. If the model is also provided
  then the result model is returned."
  ([this action] (fn [model] ((update this) action model)))
  ([this action model] ((update this) action model)))

(deftype Component [model action update queries view]
  IComponent
  ; A component is just a concrete description of the IComponent interface.
  (model [_] model)
  (action [_] action)
  (update [_] update)
  (queries [_] queries)
  (view [_] view)

  ; We let the invocation of components run their view function since this is
  ; the most popular place for a component to be used.
  IFn
  (-invoke [this state]
    (-invoke this state zero-context))
  (-invoke [_ state ctx]
    (let [qs (map-vals (partial q ctx) queries)]
      (view {:state state :queries qs} ctx))))

(defn map->Component [{:keys [model action update view queries]}]
  (Component. model action update queries view))

(defn reify
  "Reify any type implementing IComponent into a Elm Component record type.
  This is useful if an implementer of the protocol either does some work
  whenever the IComponent methods are called and you'd like to cache it OR when
  you'd like to get the IFn behavior of a fully instantiated Component value."
  [it]
  (map->Component
    {:model (model it)
     :action (action it)
     :update (update it)
     :queries (queries it)
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
  [& {:keys [model action queries update view]
      :or {model s/Any
           action s/Any
           queries {}
           update (fn [_ s] s)}
      :as options}]
  (let [options (assoc options
                  :model model
                  :action action
                  :update update
                  :queries queries
                  :view view)
        component-keys [:model :action :queries :update :view]
        quiescent-opts (apply dissoc options component-keys)
        quiescence (q/component view quiescent-opts)
        component-opts (assoc (select-keys options component-keys)
                         :view quiescence)]
    (map->Component component-opts)))

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
    :model (map-vals model routes)

    :action
    (let [match-keyword-pred (fn [kw-here] (fn [pair] (= kw-here (key pair))))
          get-pred-and-schema (fn [pair] [(match-keyword-pred (key pair))
                                          (action (val pair))])
          preds-and-schemas (mapcat get-pred-and-schema routes)]
      (apply s/conditional preds-and-schemas))

    :update
    (let [update-set (map-vals update routes)]
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
            view-set (map-vals build-subview routes)]
        (view-composition view-set dispatch)))))

