(ns oak.promise
  "A simplified promise library atop ES6 Promises. In particular, it is not
  easy to use this library to access the `reject` handlers of a standard ES6
  promise. Instead, return errorful values directly and avoid exceptions."
  (:refer-clojure :exclude [apply all]))

(defn make
  "A Promise built from an immediately executed side-effecting function,
  works a bit like `apply` but returns immediately even if the actual
  function is slow."
  [eff & args]
  (js/Promise. (fn [resolve _reject] (resolve (cljs.core/apply eff args)))))

(defn simplify
  "Given a general ES6 Promise which may be returning values via 'resolve' or
  'reject', create a simplified promise which only resolves either `{:resolve
  value}` or `{:reject reason}` instead."
  [promise]
  (.then promise
         (fn [value] {:resolve value})
         (fn [reason] {:reject reason})))

(defn unit
  "A Promise immediately resolving to a value."
  [v] (make (fn [] v)))

(defn bind
  "Chains a Promise. Given a process consuming an input and eventually
  producing an output (e.g., a function from a single argument to a Promise
  of output) and a Promise of the input we'll get a Promise of the output."
  [promise process] (.then promise process))

(defn star
  "Given a process from inputs to a Promise of output, `star` lifts this to a
   process from Promises of inputs to Promises of outputs."
  [process] (fn [promise] (bind promise process)))

(defn all
  "Given a collection of Promised values, a Promise of a collection of values."
  [coll] (.all js/Promise coll))

(defn race
  "Given a collection of Promised values, a Promise of whichever value from
  that collection arrives first. The remainder are cancelled."
  [coll] (.race js/Promise coll))

(defn apply
  "Given a function `f` of multiple arguments, `(apply f ...)` also takes in
  any number of Promises of values and returns a Promise of the result of
  applying `f` to all of those values."
  [f & args] (bind (all args) (partial cljs.core/apply f)))
