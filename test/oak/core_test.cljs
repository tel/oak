(ns oak.core-test
  (:require
    [cljs.test :as t :include-macros true]))

(t/deftest stupid-test
  (t/is (= true true)))
