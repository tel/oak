(ns oak.experimental.devcards.ui
  (:require
    [oak.component :as oak]
    [oak.dom :as d]
    [devcards.util.edn-renderer :as edn-rend]))

(def action-list
  (oak/make
    :view
    (fn [model _submit]
      (d/ul {:style #js {:overflow-y  "scroll"
                         :font-size   "10pt"
                         :list-style  "none"
                         :max-height  "100px"
                         :padding     0
                         :line-height 1}}
        (for [[domain action] (reverse model)]
          (d/li {}
            (edn-rend/html-edn [domain action])))))))

