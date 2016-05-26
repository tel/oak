(ns oak.experimental.devcards.ui
  (:require
    [forest.macros :as forest]
    [oak.component :as oak]
    [oak.dom :as d]
    [forest.class-names :as forestcn]
    [devcards.util.edn-renderer :as edn-rend])
  (:import
    (goog.i18n DateTimeFormat)))

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
    :view
    (fn [{{:keys [domain] :as model} :model} _submit]
      (d/tr {}
        (d/td {:className DateCell}
              (.format (DateTimeFormat. "KK:mm ss aa")
                       (:as-of model)))
        (d/td {:className (forestcn/class-names
                            DomainCell
                            {DomainIsLocal  (= domain :local)
                             DomainIsOracle (= domain :oracle)})}
              (case domain
                :local "local"
                :oracle "oracle"))
        (d/td {}
              (edn-rend/html-edn {:action (:action model)}))))))

(def action-demo
  (oak/make
    :view
    (fn [{model :model} _submit]
      (let [rows (for [action (reverse model)]
                   (action-row action))]
        (d/div {:className ActionDemo}
          (d/table {}
            (apply d/tbody {} rows)))))))

(defn new-action [domain ev]
  {:domain   domain
   :as-of    (js/Date.)
   :action   ev
   :expanded false})

(defn add-new-action [domain ev queue]
  (conj (if (> (count queue) 100)
          (pop queue)
          queue)
        (new-action domain ev)))
