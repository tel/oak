(ns oak.test-runner
  (:require
    [doo.runner :as doo :include-macros true]
    [oak.core-test]))

(doo/doo-all-tests)

