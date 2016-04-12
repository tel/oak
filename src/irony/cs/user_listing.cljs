(ns irony.cs.user-listing
  (:require
    [schema.core :as s :include-macros true]
    [irony.cs.bootstrap :as bs]
    [irony.elm :as elm]
    [irony.elm.dom :as d]))

(def Row
  (elm/make
    :name "User Row"
    :model {:user/gid s/Int
            :user/username s/Str
            :user/name s/Str
            :user/email s/Str
            :user/website s/Str}
    :keyfn :user/gid
    :view
    (fn [{:keys [user/gid user/username user/name
                 user/email user/website]} _dispatch]
      (d/tr {}
        (d/td {} gid)
        (d/td {} username)
        (d/td {} email)
        (d/td {} name)
        (d/td {} website)))))

(def Table
  (elm/make
    :name "User Table"
    :model [(elm/model Row)]
    :view
    (fn [state dispatch]
      (bs/Table {:condensed true :hover true}
        (d/thead {}
          (d/tr {}
            (d/th {} "Global ID")
            (d/th {} "User name")
            (d/th {} "Email")
            (d/th {} "Name")
            (d/th {} "Website")))
        (d/tbody {}
          (for [u state]
            (Row u dispatch)))))))
