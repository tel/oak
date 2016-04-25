(ns oak.schema
  "Some event and state schemata are standard but not immediately
  well-supported by Schema. This namespace provides combinators and schemata
  for efficient, convenient descriptions of compositional Oak events and
  states.

  NOT YET IMPLEMENTED CORRECTLY! In particular, the pre-conditions that help
  this to work with cond-pre are not available yet."
  (:require
    [schema.core :as s]))

(defn cmdp
  "In the event that you are not using cond-pre, it is convenient to be able
  to describe the pre-conditions of a cmd quickly for use in conditional. The
  value (cmdp :foo) is the pre-condition for (cmd :foo)."
  [name]
  (fn cmd-predicate [[the-name & _]] (= name the-name)))

(defn cmd
  "The schema (cmd :foo scm1 scm2 scm3 ...) matches arguments of the form
  [:foo x y z ...] where each of x, y, and z must match the cooresponding
  schema argument. Moreover, this schema has a two-stage pre-condition that the
  datum is first a vector and second begins with :foo enabling the easy use of
  cond-pre."
  [name & arg-schemata]
  (apply vector
         (s/one (s/eq name) :name)
         (map #(s/one % :argument) arg-schemata)))

(defn targeted
  "The schema (targeted scmT scmE) matches arguments of the form [t e] so it
  is similar to (pair scmT :target scmE :event) but targeted is more
  convenient to use and works better with cond-pre since it will attempt to
  match the target schema as a precondition."
  [target-schema payload-schema]
  (s/pair target-schema :target payload-schema :payload))

(defn cond-pair
  "Given an arbitrary number of [a b] vectors as arguments, pair-sum is a schema
  which matches pairs [x y] such that first a matches x and then b matches y
  for some vector argument. As a shortcut, a may be a keyword which is
  interpreted as keyword equality. This provides schema/cond-pre-like semantics to
  vector pairs where normally one would need to use schema/conditional."
  [& args]
  (apply s/conditional
         (mapcat (fn [[a b]]
                   (let [a-schema (if (keyword? a)
                                    (s/eq a)
                                    a)
                         a-conditional (fn cond-pair-head-precondition [x]
                                         (when (and (vector? x)
                                                  (= 2 (count x)))
                                           (let [[fst _snd] x]
                                             (nil? (s/check a-schema fst)))))]
                     [a-conditional (s/pair a-schema :fst b :snd)]))
                 args)))