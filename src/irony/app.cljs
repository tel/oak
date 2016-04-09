(ns irony.app
  (:require
    [irony.elm.start :as elm-start]
    [irony.cs.counters :as counters]
    [mount.core :as mount :include-macros true]))

(def mount-point
  (.getElementById js/document "app"))

(defonce ^:export state-atom (atom counters/Set-init))

(declare app)
(mount/defstate app
  :start (let [this-app (elm-start/make
                          counters/Set mount-point
                          :state-atom state-atom)
               start-fn (:start! this-app)]
           (start-fn)
           this-app)
  :stop (let [stop-fn (:stop! @app)]
          (stop-fn)))

; -----------------------------------------------------------------------------
; User code

(defn ^:export state [] (clj->js @(:state @app)))

(defn ^:export reset-state
  "In the event that the application state ends up stuck use this function to
   reset it to the initial state."
  []
  (reset! state-atom counters/Set-init)
  ((:invalidate! @app))
  ((:try-render! @app)))

