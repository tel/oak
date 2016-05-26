(ns oak.oracle.atom-listener
  "An atom-listener oracle is built around a provided atom. Every time the atom
  changes the cache is updated with the new value of the atom. Queries are
  paths into the atom's state."
  (:require
    [oak.oracle :as oracle]
    [cljs.core.match :refer-macros [match]]))

(defn oracle
  "Build an Oracle which watches an atom and updates its own cache with the
  atom's state. Queries are interpreted as paths into the atom with
  `get-in`. There is no refresh step."
  [the-atom]

  (oracle/make*
    {:step    (fn step [action _model]
                (match action
                  [:set new-state] new-state))

     :respond get-in

     :start   (fn start [submit]
                (submit [:set @the-atom])
                (let [uniq-name (gensym)]
                  (add-watch
                    the-atom uniq-name
                    (fn watch-atom [_uniq-key _the-atom _old-state new-state]
                      (submit [:set new-state])))
                  uniq-name))

     :stop    (partial remove-watch the-atom)}))
