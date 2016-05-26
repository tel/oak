(ns oak.internal.utils
  "Non-public utility functions needed for the implementation of Oak.")

(defn map-kvs [f hashmap]
  (persistent!
    (reduce
      (fn [acc [k v]]
        (assoc! acc k (f k v)))
      (transient {})
      hashmap)))
