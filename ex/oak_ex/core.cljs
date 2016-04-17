(ns oak-ex.core
  (:require
    [devcards.core :as devcards :include-macros true]
    [devcards.util.edn-renderer :as edn-rend]
    [oak.core :as oak]
    [oak.oracle :as oracle]
    [oak.dom :as d]
    [schema.core :as s]))

(enable-console-print!)

(defonce event-queue (atom {:state #queue []}))

(def counter
  (oak/make
    :state s/Int
    :event (s/enum :inc :dec)
    :step
    (fn [event state]
      (case event
        :inc (inc state)
        :dec (dec state)))
    :view
    (fn [state submit]
      (let [clicker (fn clicker [event] (fn [_] (submit event)))]
        (d/div {}
          (d/button {:onClick (clicker :dec)} "-")
          (d/span {} state)
          (d/button {:onClick (clicker :inc)} "+"))))))

(defn render-oak
  ([component] (render-oak component (oracle/make)))
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
                                   (on-event [:oracle event])
                                   (swap! total-state update
                                          :cache (oracle/step oracle event)))
                   local-submit (fn local-submit [event]
                                  (on-event [:local event])
                                  (swap! total-state update
                                         :state (oak/step component event)))]
               (oracle/refresh oracle cache queries oracle-submit)
               (component state result local-submit)))))))))

(def event-row
  (oak/make
    :state {:domain     (s/enum :local :oracle)
            :event    s/Any
            :expanded s/Bool}
    :view
    (fn [state submit]
      (println "State: " state)
      (d/tr {}
        (case (:domain state)
          :local (d/td {} "local")
          :oracle (d/td {} "oracle"))
        (d/td {} (edn-rend/html-edn (:event state)))))))

(def event-demo
  (oak/make
    :state (s/queue (oak/state event-row))
    :view
    (fn [state submit]
      (println state)
      (let [rows (for [event (reverse state)]
                   (event-row event))]
        (d/table {} (apply d/tbody {} rows))))))

(defn new-event [[domain ev]]
  {:domain domain
   :event ev
   :expanded false})

(defn add-new-event [queue ev]
  (conj (if (> (count queue) 10)
          (pop queue)
          queue)
        (new-event ev)))

(declare single-counter)
(devcards/defcard single-counter
  (render-oak counter)
  {:state 10 :cache {}}
  {:on-event (fn [ev]
               (swap! event-queue update
                      :state #(add-new-event % ev)))})

(declare event-set)
(devcards/defcard event-set
  (render-oak event-demo)
  event-queue)
