(ns irony.ui
  (:require
    [irony.elm :as elm]
    [irony.db :as db]
    [datascript.core :as ds]
    [irony.cs.user-listing :as user-listing]
    [schema.core :as s]
    [irony.elm.dom :as d]))

(def ui
  (elm/make
    :name "Ui-Top"
    :view
    (fn [state dispatch]
      (let [users (ds/q
                    '[:find [(pull ?u [:user/gid :user/username :user/name
                                       :user/email :user/website]) ...]
                      :where
                             [?u :user/gid ?gid]]
                    @db/db)]
        (user-listing/Table users)))))
