(ns irony.elm.start
  "Functions and configuation for starting a basic Elm application."
  (:refer-clojure :exclude [update])
  (:require
    [irony.elm :as elm]
    [schema.core :as s]
    [quiescent.core :as q])
  (:import
    (goog.async AnimationDelay)))

(defn make
  [root target
   & {:keys [state-atom]
      :or   {state-atom (atom)}}]
  (let [model (elm/model root)
        action (elm/action root)
        update (elm/update root)

        validate-model! (s/validator model)
        validate-action! (s/validator action)

        is-dirty? (atom true)
        updater (atom)
        loop-fn (atom)
        previous-queries (atom {})

        invalidate! (fn [] (reset! is-dirty? true))
        commit! (fn [action]
                  (let [old-state @state-atom
                        new-state (update (validate-action! action) old-state)]
                    (when-not (identical? new-state old-state)
                      (validate-model! new-state)
                      (invalidate!)
                      (reset! state-atom new-state))))
        try-render! (fn []
                      (if @is-dirty?
                        (let [queries (atom {})
                              ctx (elm/make-context commit! queries)
                              vtree (root @state-atom ctx)]
                          (q/render vtree target)
                          ;(println "QUERIES SEEN: " @queries)
                          (reset! is-dirty? false)
                          true)
                        false))
        clean! (fn []
                 (when-let [^AnimationDelay timer @updater]
                   (.stop timer)
                   (.dispose timer)
                   (reset! updater nil)))
        loop! (fn []
                (clean!)
                (try-render!)
                (reset! updater (doto (AnimationDelay. @loop-fn)
                                  .start)))
        stop! (fn []
                (clean!)
                (q/unmount target))
        start! (fn []
                 (invalidate!)
                 (loop!))]

    ; Tie the recursive loop. In the above code we have co-recursive
    ; dependencies between commit! and loop!. To resolve this, the loop is
    ; broken with an atom, loop-fn, that we have to establish the value of
    ; after all the functions have been defined.
    ;
    ; NOTE: This may have some stack frame issues. How can we be sure that
    ; they're getting unwound?
    (reset! loop-fn loop!)

    {:state state-atom
     :updater updater
     :is-dirty? is-dirty?

     :invalidate! invalidate!
     :try-render! try-render!
     :start! start!
     :stop! stop!}))
