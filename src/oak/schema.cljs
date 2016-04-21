(ns oak.schema
  (:require
    [schema.core :as s]))

(defn cmd [name & arg-schemata]
  (apply vector
         (s/one (s/eq name) :name)
         (map #(s/one % :argument) arg-schemata)))

(defn cmdp [name]
  (fn cmd-predicate [[the-name & _]] (= name the-name)))

(defn targeted [target-schema payload-schema]
  (s/pair target-schema :target payload-schema :payload))
