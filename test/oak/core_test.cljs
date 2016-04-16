(ns oak.core-test
  (:require
    [cljs.test :as t :include-macros true]))

(t/deftest intentional-failure
  (t/is (= true false)))
