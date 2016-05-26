(ns oak.internal.utils
  "Non-public utility functions needed for the implementation of Oak.")

(defn map-kvs [f hashmap]
  (persistent!
    (reduce
      (fn [acc [k v]]
        (assoc! acc k (f k v)))
      (transient {})
      hashmap)))

(defn map-vals [f hashmap]
  (map-kvs (fn [_k v] (f v)) hashmap))
