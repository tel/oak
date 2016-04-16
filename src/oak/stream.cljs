(ns oak.stream
  "Streams are essentially asynchronously produced sequences. They're similar
  to lazy sequences, but their delays are captured by ES6 Promises instead
  of the runtime system.

  In particular, given a stream we do not immediately have any values at
  all, but we'll 'eventually' have some (finite) chunk and (possibly)
  another stream producing the subsequent values. Here 'eventually' having
  something means that we have a promise of that thing. Generally, a stream
  is consumed by connecting it up to a reducer which (statefully) consumes
  all the values from the stream and eventually produces a result. Most
  likely your reducers will periodically cause side effects as well.

  Concretely, a stream is a Promise resolving to a map. If this map contains
  a value at key `:chunk` then this is a vector of elements from the stream.
  If it contains a value at key `:next` then it is a stream of subsequent
  values. Notably, a stream may *skip* if there is no `:chunk` value and it
  may *terminate* if there is no `:next` value. It may even do both
  signifying the end of the stream."
  (:refer-clojure :exclude [chunk])
  (:require
    [oak.promise :as promise]))


(defn chunk
  "An immediately returning finite stream."
  [vec] (promise/unit {:chunk vec}))

(defn zero
  "A stream which immediately terminates."
  [] (promise/unit {}))

(defn unit
  "A stream of a single immediately returned value."
  [v] (chunk [v]))

(defn stateful
  "Given an initial state and a function from states to promises of maps of the
  shape `{:chunk coll :next new-state}` we have a stream of values assembled
  by gluing together all of the returned `colls`. Note, either the `:chunk` or
  `:next` keys can be omitted to represent either a skip or a termination,
  respectively."
  [state unfold]
  (promise/bind
    (unfold state)
    (fn [{:keys [next] :as result}]
      (if next
        (let [new-next (promise/bind next unfold)]
          (assoc result :next new-next))
        result))))

(defn consume
  "Given a function `(reducer state value)` resulting in a new state we can
  consume an entire stream resulting in a Promise of the final state. More
  interestingly we can execute a side effect on each reduction step."
  ; TODO: Make this this won't blow the stack! How do we do recursion properly here?
  [stream initial-state reducer]
  (promise/bind
    stream
    (fn [{:keys [chunk next] :as result}]
      (let [new-state (if-not chunk
                        initial-state
                        (reduce reducer initial-state chunk))]
        (if-not next
          new-state
          (consume next new-state reducer))))))
