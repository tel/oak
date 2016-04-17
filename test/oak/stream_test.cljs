(ns oak.stream-test
  (:require
    [oak.stream :as stream]
    [oak.promise :as promise]
    [cljs.test :as t :include-macros true]))

(t/deftest zero-is-empty
  (t/async done
    (-> (stream/zero)
        stream/realize
        (promise/bind
          (fn [res]
            (t/is (= res []))
            (done))))))

(t/deftest iterate-range
  (let [sum (atom 0)]
    (t/async done
      (-> (stream/range 10)
          (stream/iterate
            (fn [v] (swap! sum + v))
            #(do
              (t/is (= @sum (reduce + (range 10))))
              (done)))))))

(t/deftest realize-range
  (t/async done
    (-> (stream/range 10)
        stream/realize
        (promise/bind
          (fn [res]
            (t/is (= res (vec (range 10))))
            (done))))))

(t/deftest stack-heavy
  ; Increasing count another order of magnitude will cause Karma to time out
  ; but we don't appear to be hitting a stack limit!
  ;
  ; 10^4 -- easy peasy, minimal test suite speed impact
  ; 10^5 -- approx 2s to run, slows tests, no explosions
  ; 10^6 -- > 10s to run, karma times out, no explosions
  (let [count (.pow js/Math 10 4)]
    (t/async done
      (-> (stream/range count)
          (stream/consume
            0 (fn [st _] (inc st)))
          (promise/bind
            (fn [res]
              (t/is (= res count))
              (done)))))))