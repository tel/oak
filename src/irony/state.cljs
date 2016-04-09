(ns irony.state
  (:require
    [mount.core :as mount :include-macros true]
    [irony.elm :as elm]
    [irony.ui :as ui]))

(declare ^:export state)
(mount/defstate state
  :start (atom (elm/base ui/root)))
