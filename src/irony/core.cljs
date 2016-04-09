(ns irony.core
  (:require
    [mount.core :as mount]
    [irony.app :as app]))

(enable-console-print!)

(defn ^:export start []
  (mount/start))

(defn ^:export stop []
  (mount/stop))

(defn ^:export reload []
  (stop)
  (start))

(start)
