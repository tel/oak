(ns oak.experimental.devcards
  (:require
    [devcards.core :as devcards :include-macros true]
    [oak.oracle :as oracle]
    [oak.component :as oak]
    [schema.core :as s]
    [oak.dom :as d]
    [devcards.util.edn-renderer :as edn-rend]
    [promesa.core :as p]
    [forest.class-names :as forestcn]
    [forest.macros :as forest :include-macros true])
  (:import
    (goog.i18n DateTimeFormat)))

(defn render
  "Provided a component (and optionally an oracle) produce Devcard main-obj.
  Initial state should be a map of the form {:state state :cache cache} for
  the initial component state and initial oracle cache respectively. A
  function at the options key :on-event will receive every event from the
  system."
  ([component] (render component (oracle/make)))
  ([component oracle]
   (reify devcards/IDevcardOptions
     (-devcard-options [_ opts]
       (let
         [{:keys [on-event]
           :or   {on-event (fn [_])}} (:options opts)]
         (assoc opts
           :main-obj
           (fn devcard-context [total-state _owner]
             (let [{:keys [state cache]} @total-state
                   {:keys [result queries]} (oracle/substantiate oracle cache component state)
                   oracle-submit (fn oracle-submit [event]
                                   ; Why use a promise here?
                                   ; We have to get state modifications out
                                   ; of the render cycle. Without tossing a
                                   ; delay on here they'll happen
                                   ; synchronously and this will throw render
                                   ; errors.
                                   (p/then
                                     (p/delay 0)
                                     (fn [_]
                                       (on-event [:oracle event])
                                       (swap! total-state update
                                              :cache (oracle/step oracle event)))))
                   local-submit (fn local-submit [event]
                                  (on-event [:local event])
                                  (swap! total-state update
                                         :state (oak/step component event)))]
               (oracle/refresh oracle cache queries oracle-submit)
               (component state result local-submit)))))))))

(declare events-stylesheet
         EventDemo DateCell DomainCell DomainIsLocal DomainIsOracle)
(forest/defstylesheet events-stylesheet
  [.EventDemo {:height     "300px"
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

(def event-row
  (oak/make
    :state {:domain   (s/enum :local :oracle)
            :as-of    s/Inst
            :event    s/Any
            :expanded s/Bool}
    :view
    (fn [{:keys [domain] :as state} _submit]
      (d/tr {}
        (d/td {:className DateCell}
              (.format (DateTimeFormat. "KK:mm ss aa")
                       (:as-of state)))
        (d/td {:className (forestcn/class-names
                            DomainCell
                            {DomainIsLocal (= domain :local)
                             DomainIsOracle (= domain :oracle)})}
              (case domain
                :local "local"
                :oracle "oracle"))
        (d/td {}
              (edn-rend/html-edn {:event (:event state)}))))))

(def event-demo
  (oak/make
    :state (s/queue (oak/state event-row))
    :view
    (fn [state _submit]
      (let [rows (for [event (reverse state)]
                   (event-row event))]
        (d/div {:className EventDemo}
          (d/table {}
            (apply d/tbody {} rows)))))))

(defn new-event [[domain ev]]
  {:domain domain
   :as-of (js/Date.)
   :event ev
   :expanded false})

(defn add-new-event [queue ev]
  (conj (if (> (count queue) 100)
          (pop queue)
          queue)
        (new-event ev)))
