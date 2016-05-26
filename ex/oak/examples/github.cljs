(ns oak.examples.github
  (:require
    [cognitect.transit :as transit]
    [cljs.core.match :refer-macros [match]]
    [devcards.core :as devcards :include-macros true]
    [oak.component :as oak]
    [oak.experimental.devcards :as oakdc]
    [oak.experimental.devcards.ui :as oakdc-ui]
    [oak.dom :as d]
    [httpurr.client :as http]
    [httpurr.client.xhr :as xhr]
    [promesa.core :as p]
    [oak.oracle :as oracle]))

(let [json-reader (transit/reader :json)]
  (defn json-read [string]
    (transit/read json-reader string)))

(defn search-profile [name]
  (http/send! xhr/client
              {:method :get
               :url    (str "https://api.github.com/users/" name)}))

(def ex
  (oak/make
    :step
    (fn [action model]
      (match action
        :query! (assoc model :last-query (:value model))
        [:set name] (assoc model :value name)))

    :query
    (fn [{:keys [last-query]} q]
      (if last-query
        {:last-query last-query
         :result     (q [:q last-query])}
        {:last-query nil}))

    :view
    (fn [model result submit]
      (d/div {}
        (d/form {:onSubmit (fn [e] (.preventDefault e) (submit :query!))}
          (d/uinput {:value    (:value model)
                     :onChange (fn [e] (submit [:set (.-value (.-target e))]))})
          (d/input {:type  "submit"
                    :value "Search"}))
        (when-let [result (get result :result)]
          (if-not (= 200 (:status result))
            (d/p {} (str "Status: " result))
            (d/img {:src    (get (json-read (:body result)) "avatar_url")
                    :width  150
                    :height 150})))))))

(def oracle
  (oracle/make
    :respond (fn [model [_ name]]
               (get-in model [:memory name] {:meta :pending}))
    :step (fn [action model]
            (match action
              [:queried date] (assoc model :last-query date)
              [:set query result] (assoc-in model [:memory query] result)))
    :refresh (fn [model queries submit]
               (let [last-query (:last-query model)
                     now (js/Date.)
                     diff (.abs js/Math (- (.getTime now) (.getTime last-query)))]
                 (when (< 1000 diff)
                   (doseq [[_sub query] queries]
                     (when-not (find (:memory model) query)
                       (submit [:queried (js/Date.)])
                       (p/then (search-profile query)
                               (fn [result]
                                 (submit [:set query result]))))))))))

(defn oracle-initial-model []
  {:last-query (js/Date.)
   :memory     {}})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defonce actions (atom []))

(declare display)
(devcards/defcard display
  (oakdc/render
    ex {:oracle oracle
        :on-action (fn [domain action]
                (swap! actions conj [domain action]))})
  {:model {} :cache (oracle-initial-model)})

(declare action-set)
(devcards/defcard action-set
  (oakdc/render oakdc-ui/action-list)
  actions)
