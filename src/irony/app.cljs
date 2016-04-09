(ns irony.app
  (:require
    [irony.ui :as ui]
    [irony.elm.start :as elm-start]
    [mount.core :as mount :include-macros true]))

(def mount-point
  (.getElementById js/document "app"))

(declare app)
(mount/defstate app
  :start (let [this-app (elm-start/make ui/root mount-point)
               start-fn (:start! this-app)]
           (start-fn)
           this-app)
  :stop (let [stop-fn (:stop! @app)]
          (stop-fn)))