(ns oak.experimental.devcards
  "Alternative render methods for prototyping Oak components in a Devcards
  environment."
  (:require
    [devcards.core :as devcards :include-macros true]
    [oak.oracle :as oracle]
    [oak.component :as oak]))

(defprotocol IRender
  (-render [component options]))

(defn render
  ([component] (render component {}))
  ([component options] (-render component options)))

(extend-type oak/Component
  IRender
  (-render [component {:keys [on-action]
                       :or   {on-action (fn [_domain _action])}}]
    (fn devcard-context [model _owner]
      (let [local-submit (fn local-submit [action]
                           (on-action :local action)
                           (swap! model (oak/step component action)))]
        (component @model local-submit)))))

(extend-type oak/QueryComponent
  IRender
  (-render [component {:keys [oracle on-action]
                       :or   {oracle    (oracle/make)
                              on-action (fn [_domain _action])}}]

    (fn devcard-context [total-model _owner]
      (let [{:keys [model cache]} @total-model
            {:keys [result queries]} (oracle/substantiate oracle cache component model)

            oracle-submit (fn oracle-submit [action]
                            (on-action :oracle action)
                            (swap! total-model update
                                   :cache (oracle/step oracle action)))

            local-submit (fn local-submit [action]
                           (on-action :local action)
                           (swap! total-model update
                                  :model (oak/step component action)))]

        (js/setTimeout #(oracle/refresh oracle cache queries oracle-submit) 100)
        (component model result local-submit)))))

