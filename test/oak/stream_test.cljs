(ns oak.stream-test
  (:require
    [oak.stream :as stream]
    [oak.promise :as promise]
    [cljs.test :as t :include-macros true]))

(t/deftest zero-is-empty
  (t/async done
    (-> (stream/realize (stream/zero))
        (promise/bind
          (fn [res]
            (t/is (= res []))
            (done))))))

(t/deftest stateful-stream-to
  (let [sum (atom 0)]
    (t/async done
      (stream/iterate
        (stream/range 10)
        (fn [v] (swap! sum + v))
        (fn []
          (t/is (= @sum (reduce + (range 10))))
          (done))))))
