(ns irony.app
  (:require
    [irony.ui :as ui]
    [mount.core :as mount :include-macros true]
    [quiescent.core :as q]
    [irony.elm :as elm]
    [schema.core :as s]))

(def mount-point
  (.getElementById js/document "app"))

(def model-validator (s/validator (elm/model ui/root)))
(def action-validator (s/validator (elm/action ui/root)))

(def state (atom (model-validator (elm/init ui/root))))

(declare dispatch)

(defn render []
  (q/render (elm/viewf ui/root @state dispatch) mount-point))

(defn dispatch [action]
  (let [old-state @state
        new-state (elm/updatef ui/root (action-validator action) old-state)]
    (reset! state (model-validator new-state))
    (render)))

(defn- stop [] (q/unmount mount-point))

(declare app)
(mount/defstate app :start (render) :stop (stop))
