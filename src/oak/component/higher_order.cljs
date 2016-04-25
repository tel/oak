(ns oak.component.higher-order
  "Functions for constructing Components from sub-Components."
  (:require
    [oak.internal.utils :as util]
    [oak.component :as oak]
    [oak.dom :as d]
    [schema.core :as s]
    [oak.schema :as os]))

; -----------------------------------------------------------------------------
; Higher-order components

(defn parallel
  "Construct a *static* component by gluing several subcomponents together
  without interaction. Parallel components are made of a fixed set of named
  subcomponents and have dramatically simplified wiring. Use parallel
  components whenever you do not need custom state management behavior.

  By default, a static component renders its subcomponents in a div in
  arbitrary order. Provide a :view-compositor function to choose how to render
  the subviews more specifically. It has a signature like (view-compositor
  subviews) where `subviews` is a map with the same keys as your static
  components containing ReactElements corresponding to each subcomponent.

  By default, all events generated by a static component are simply routed in
  parallel back to the originating subcomponents. Provide a
  :routing-transform function to choose how events are routed more
  specifically. It has a signature like (routing-transform target event
  continue) where `target` is a key in your subcomponent map, `action` is the
  event headed for that subcomponent, and `continue` is a callback of two
  arguments, the target for the action and the action itself.

  Any other options are forwarded on to `make`."
  [subcomponent-map
   & {:keys [view-compositor routing-transform]
      :or   {view-compositor   (fn [views] (apply d/div {} (vals views)))
             routing-transform (fn [target action cont] (cont target action))}
      :as   options}]

  (let [core-design
        {:model
         (util/map-vals oak/model subcomponent-map)

         :action
         (apply os/cond-pair
                (util/map-vals oak/action subcomponent-map))

         :step
         (fn static-step [[target action] model]
           (routing-transform
             target action
             (fn [target action]
               (update
                 model target
                 (oak/step
                   (get subcomponent-map target)
                   action)))))

         :query
         (fn static-query [model q]
           (util/map-kvs
             (fn [target subc] (oak/query subc (get model target) q))
             subcomponent-map))

         :view
         (fn static-view [{:keys [model result]} submit]
           (let [subviews (util/map-kvs
                            (fn [target subc]
                              (subc
                                (get model target)
                                (get result target)
                                (fn [ev] (submit [target ev]))))
                            subcomponent-map)]
             (view-compositor subviews)))}]

    ; Let the remaining options override
    (oak/make*
      (merge core-design
             (-> options
                 (dissoc :view-compositor)
                 (dissoc :routing-transform))))))