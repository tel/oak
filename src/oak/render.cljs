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
             initial-model initial-omodel
             model-atom omodel-atom
             on-event]
      :or {oracle (oracle/make)
           on-event (fn [_target _event])
           target (.-body js/document)}}]

  (let [model (or model-atom (atom initial-model))
        omodel (or omodel-atom (atom initial-omodel))
        oracle-result (atom)

        alive? (atom)
        current-timer (atom)
        dirty? (atom true)]

    (letfn [(dirty! [] (reset! dirty? true))

            (submit-oracle! [ev]
              (on-event :oracle ev)
              (let [new-omodel (oracle/step oracle ev @omodel)]
                (when (not= @omodel new-omodel)
                  (reset! omodel new-omodel)
                  (dirty!))))

            (submit-local! [ev]
              (on-event :local ev)
              (let [new-model (oak/step component ev @model)]
                (when (not= @model new-model)
                  (reset! model new-model)
                  (dirty!))))

            (update-oracle! []
              (let [subst (oracle/substantiate oracle @omodel component @model)]
                (reset! oracle-result (:result subst))
                (oracle/refresh oracle @omodel (:queries subst) submit-oracle!)))

            (force-render! []
              (update-oracle!)
              (q/render (component @model @oracle-result submit-local!) target)
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
