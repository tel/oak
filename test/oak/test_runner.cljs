(ns oak.test-runner
  (:require
    [doo.runner :as doo :include-macros true]
    [oak.promise-test]
    [oak.stream-test]
    [oak.core-test]))

(doo/doo-tests
  'oak.promise-test
  'oak.stream-test
  'oak.core-test)

