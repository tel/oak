(ns oak.promise-test
  (:require
    [oak.promise :as promise]
    [cljs.test :as t :include-macros true]))

(t/deftest unit-resolve
  (t/async done
    (-> (promise/unit 10)
        (promise/bind
          (fn [ten]
            (t/is (= ten 10))
            (done))))))

(t/deftest make-immediate-resolve
  (t/async done
    (-> (promise/make identity 10)
        (promise/bind
          (fn [ten]
            (t/is (= ten 10))
            (done))))))
