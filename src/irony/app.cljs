(ns irony.app
  (:require
    [irony.ui :as ui]
    [irony.state :as state]
    [mount.core :as mount :include-macros true]
    [quiescent.core :as q]
    [irony.elm :as elm]))

(declare app)
(mount/defstate app
  :start (let [submit-fn (atom nil)
               render-it #(q/render (elm/render ui/root % @submit-fn)
                                    (.getElementById js/document "app"))
               submit (fn [action]
                        (let [old-state @@state/state
                              new-state (elm/reduce ui/root action old-state)]
                          (reset! @state/state new-state)
                          (render-it new-state)))]
           (reset! submit-fn submit)
           (render-it @@state/state))
  :stop (q/unmount (.getElementById js/document "app")))
