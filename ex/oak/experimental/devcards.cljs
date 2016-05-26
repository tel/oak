(ns oak.experimental.devcards
  (:require
    [devcards.core :as devcards :include-macros true]
    [oak.oracle :as oracle]
    [oak.component :as oak]
    [oak.dom :as d]
    [devcards.util.edn-renderer :as edn-rend]
    [promesa.core :as p]
    [forest.class-names :as forestcn]
    [forest.macros :as forest :include-macros true])
  (:import
    (goog.i18n DateTimeFormat)))

(defn render
  "Provided a component (and optionally an oracle) produce Devcard main-obj.
  Initial model should be a map of the form {:model model :cache cache} for
  the initial component model and initial oracle cache respectively. A
  function at the options key :on-action will receive every action from the
  system."
  ([component] (render component (oracle/make)))
  ([component oracle]
   (reify devcards/IDevcardOptions
     (-devcard-options [_ opts]
       (let
         [{:keys [on-action]
           :or   {on-action (fn [_])}} (:options opts)]
         (assoc opts
           :main-obj
           (fn devcard-context [total-model _owner]
             (let [{:keys [model cache]} @total-model
                   {:keys [result queries]} (oracle/substantiate oracle cache component model)
                   oracle-submit (fn oracle-submit [action]
                                   ; Why use a promise here?
                                   ; We have to get model modifications out
                                   ; of the render cycle. Without tossing a
                                   ; delay on here they'll happen
                                   ; synchronously and this will throw render
                                   ; errors.
                                   (p/then
                                     (p/delay 0)
                                     (fn [_]
                                       (on-action [:oracle action])
                                       (swap! total-model update
                                              :cache (oracle/step oracle action)))))
                   local-submit (fn local-submit [action]
                                  (on-action [:local action])
                                  (swap! total-model update
                                         :model (oak/step component action)))]
               (oracle/refresh oracle cache queries oracle-submit)
               (component model result local-submit)))))))))

(declare action-stylesheet
         EventDemo DateCell DomainCell DomainIsLocal DomainIsOracle)
(forest/defstylesheet action-stylesheet
  [.ActionDemo {:height     "300px"
               :overflow-y "auto"}]
  [.DateCell {:height      "20px"
              :line-height "20px"
              :padding     "3px 7px"
              :background  "#eee"
              :font-size   "0.6em"}]
  [.DomainCell {:height         "20px"
                :line-height    "20px"
                :padding        "3px 7px"
                :font-family    "Gill Sans"
                :font-size      "12px"
                :color          "#fff"
                :text-transform "uppercase"}]
  [.DomainIsLocal {:background "#1b6"}]
  [.DomainIsOracle {:background "#b16"}])

(def action-row
  (oak/make
    :model {:domain   (s/enum :local :oracle)
            :as-of    s/Inst
            :action    s/Any
            :expanded s/Bool}
    :view
    (fn [{{:keys [domain] :as model} :model} _submit]
      (d/tr {}
        (d/td {:className DateCell}
              (.format (DateTimeFormat. "KK:mm ss aa")
                       (:as-of model)))
        (d/td {:className (forestcn/class-names
                            DomainCell
                            {DomainIsLocal (= domain :local)
                             DomainIsOracle (= domain :oracle)})}
              (case domain
                :local "local"
                :oracle "oracle"))
        (d/td {}
              (edn-rend/html-edn {:action (:action model)}))))))

(def action-demo
  (oak/make
    :model (s/queue (oak/model action-row))
    :view
    (fn [{model :model} _submit]
      (let [rows (for [action (reverse model)]
                   (action-row action))]
        (d/div {:className ActionDemo}
          (d/table {}
            (apply d/tbody {} rows)))))))

(defn new-action [[domain ev]]
  {:domain domain
   :as-of (js/Date.)
   :action ev
   :expanded false})

(defn add-new-action [queue ev]
  (conj (if (> (count queue) 100)
          (pop queue)
          queue)
        (new-action ev)))
