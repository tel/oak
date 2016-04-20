(ns oak.render
  (:require
    [oak.oracle :as oracle]
    [quiescent.core :as q]
    [oak.component :as oak])
  (:import
    (goog.async AnimationDelay)))

(defn render
  [component
   & {:keys [oracle target
             initial-state initial-cache
             state-atom cache-atom
             on-event]
      :or {oracle (oracle/make)
           target (.-body js/document)}}]

  (let [state (or state-atom (atom initial-state))
        cache (or cache-atom (atom initial-cache))
        oracle-result (atom)

        alive? (atom)
        current-timer (atom)
        dirty? (atom true)]

    (letfn [(dirty! [] (reset! dirty? true))

            (submit-oracle! [ev]
              (on-event :oracle ev)
              (let [new-cache (oracle/step oracle ev @cache)]
                (when (not= @cache new-cache)
                  (reset! cache new-cache)
                  (dirty!))))

            (submit-local! [ev]
              (on-event :local ev)
              (let [new-state (oak/step component ev @state)]
                (when (not= @state new-state)
                  (reset! state new-state)
                  (dirty!))))

            (update-oracle! []
              (let [subst (oracle/substantiate oracle @cache component @state)]
                (reset! oracle-result (:result subst))
                (oracle/refresh oracle @cache (:queries subst) submit-oracle!)))

            (force-render! []
              (update-oracle!)
              (q/render (component @state @oracle-result submit-local!) target)
              (reset! dirty? false))

            (render! [] (when @dirty? (force-render!)))

            (loop! []
              (render!)
              (let [timer (doto (AnimationDelay. loop!) .start)]
                (reset! current-timer timer)))

            (stop! []
              (reset! alive? false)
              (when-let [timer @current-timer]
                (.stop timer)))]

      (loop!)
      (reset! alive? true)

      {:force-render! force-render!
       :request-render! dirty!
       :submit-local! submit-local!
       :submit-oracle! submit-oracle!
       :current-timer current-timer
       :alive? alive?
       :stop! stop!})))
