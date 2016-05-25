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
               :url    (str "https://api.github.com/users/" name)}))

(def ex
  (oak/make
    :model {:value                       s/Str
            (s/optional-key :last-query) (s/maybe s/Str)}
    :action (s/cond-pre
              (s/eq :query!)
              (s/pair (s/eq :set) :keyword s/Str :name))
    :step
    (fn [action model]
      (match action
        :query! (do (println "model" model)
                    (assoc model :last-query (:value model)))
        [:set name] (assoc model :value name)))

    :query
    (fn [{:keys [last-query]} q]
      (if last-query
        {:last-query last-query
         :result     (q [:q last-query])}
        {:last-query nil}))

    :view
    (fn [{:keys [model result]} submit]
      (d/div {}
        (d/form {:onSubmit (fn [e] (.preventDefault e) (submit :query!))}
          (d/uinput {:value    (:value model)
                     :onChange (fn [e] (submit [:set (.-value (.-target e))]))})
          (d/input {:type  "submit"
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
    :model {:last-query s/Inst
            :memory     {s/Str s/Any}}
    :step (fn [action model]
            (match action
              [:queried date] (assoc model :last-query date)
              [:set query result] (assoc-in model [:memory query] result)))
    :respond (fn [model [_ name]]
               (get-in model [:memory name] {:meta :pending}))
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

(defonce action-queue
  (atom {:model #queue []}))

(declare github-display)
(devcards/defcard github-display
  (oak-devcards/render ex oracle)
  {:model {} :cache (oracle-initial-model)}
  {:on-action (fn [ev]
                (match ev
                  [:local [:set _]] nil
                  :else (swap! action-queue update
                               :model #(oak-devcards/add-new-action % ev))))})

(declare action-set)
(devcards/defcard action-set
  (oak-devcards/render oak-devcards/action-demo)
  action-queue)
