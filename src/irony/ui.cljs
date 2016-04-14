(ns irony.ui
  (:require
    [irony.cs.counters :as counters]))

(def ui counters/Set)
(def init counters/Set-init)

;(def ui
;  (elm/make
;    :name "Ui-Top"
;    :view
;    (fn [state dispatch]
;      (let [users (ds/q
;                    '[:find [(pull ?u [:user/gid :user/username :user/name
;                                       :user/email :user/website]) ...]
;                      :where
;                             [?u :user/gid ?gid]]
;                    @db/db)]
;        (user-listing/Table users)))))