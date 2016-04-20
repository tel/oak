(ns oak.examples.github
  (:require
    [cognitect.transit :as transit]
    [cljs.core.match :refer-macros [match]]
    [devcards.core :as devcards :include-macros true]
    [oak.component :as oak]
    [oak.experimental.devcards :as oak-devcards]
    [oak.dom :as d]
    [schema.core :as s]
    [httpurr.client :as http]
    [httpurr.client.xhr :as xhr]
    [promesa.core :as p]
    [oak.oracle :as oracle]
    [devcards.util.edn-renderer :as edn-rend]))

(let [json-reader (transit/reader :json)]
  (defn json-read [string]
    (transit/read json-reader string)))

(defn search-profile [name]
  (http/send! xhr/client
              {:method :get
               :url (str "https://api.github.com/users/" name)}))

(def ex
  (oak/make
    :state {:value s/Str :last-query (s/maybe s/Str)}
    :event (s/cond-pre
             (s/eq :query!)
             (s/pair (s/eq :set) :keyword s/Str :name))
    :step
    (fn [event state]
      (match event
        :query! (do (println "state" state)
                    (assoc state :last-query (:value state)))
        [:set name] (assoc state :value name)))

    :query
    (fn [{:keys [last-query]} q]
      (if last-query
        {:last-query last-query
         :result (q [:q last-query])}
        {:last-query nil}))

    :view
    (fn [[state result] submit]
      (d/div {}
        (d/form {:onSubmit (fn [e] (.preventDefault e) (submit :query!))}
          (d/uinput {:value    (:value state)
                     :onChange (fn [e] (submit [:set (.-value (.-target e))]))})
          (d/input {:type "submit"
                    :value "Search"}))
        (let [{:keys [result]} result]
          (when result
            (if (= 200 (:status result))
              (let [body (json-read (:body result))]
                (d/div {}
                  (edn-rend/html-edn body)))
              "Error")))))))

(def oracle
  (oracle/make
    :state {:last-query s/Inst
            :memory {s/Str s/Any}}
    :step (fn [event state]
            (match event
              [:queried date] (assoc state :last-query date)
              [:set query result] (assoc-in state [:memory query] result)))
    :respond (fn [state [_ name]]
               (get-in state [:memory name] {:meta :pending}))
    :refresh (fn [state queries submit]
               (let [last-query (:last-query state)
                     now (js/Date.)
                     diff (.abs js/Math (- (.getTime now) (.getTime last-query)))]
                 (when (< 1000 diff)
                   (doseq [[_sub query] queries]
                     (when-not (find (:memory state) query)
                       (submit [:queried (js/Date.)])
                       (p/then (search-profile query)
                               (fn [result]
                                 (submit [:set query result]))))))))))

(defn oracle-initial-state []
  {:last-query (js/Date.)
   :memory {}})

(defonce event-queue
  (atom {:state #queue []}))

(declare github-display)
(devcards/defcard github-display
  (oak-devcards/render ex oracle)
  {:state {} :cache (oracle-initial-state)}
  {:on-event (fn [ev]
               (match ev
                 [:local [:set _]] nil
                 :else (swap! event-queue update
                              :state #(oak-devcards/add-new-event % ev))))})

(declare event-set)
(devcards/defcard event-set
  (oak-devcards/render oak-devcards/event-demo)
  event-queue)
