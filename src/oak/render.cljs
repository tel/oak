(ns oak.render
  (:require
    [oak.oracle :as oracle]
    [quiescent.core :as q]
    [oak.component :as oak]
    [cljs.core.async :as async])
  (:require-macros
    [cljs.core.async.macros :as asyncm])
  (:import goog.async.AnimationDelay))

(defprotocol IRender
  (render [component options]))

; TODO: Refactor these things. There's a lot of shared code---essentially
; because one needs to corecursively define all of the names--so there
; ought to be a way to minimize repetation.

(extend-type oak/Component

  IRender
  (render [component options]
    (let [{:keys [target initial-model model-atom on-action]
           :or   {on-action (fn [_action])
                  target    (.-body js/document)}} options

          model (or model-atom (atom initial-model))

          alive? (atom)
          current-timer (atom)
          dirty? (atom false)]

      (letfn [(dirty! [] (reset! dirty? true))

              (submit-local! [ev]
                (on-action :local ev)
                (let [new-model (oak/step component ev @model)]
                  (when (not= @model new-model)
                    (reset! model new-model)
                    (dirty!))))

              (force-render! []
                (q/render (component @model submit-local!) target)
                (reset! dirty? false))

              (render! []
                (when @dirty? (force-render!)))

              (render-loop! []
                (render!)
                (let [timer (doto (AnimationDelay. render-loop!) .start)]
                  (reset! current-timer timer)))

              (stop! []
                (reset! alive? false)
                (q/unmount target)
                (when-let [timer @current-timer]
                  (.stop timer)))]

        (dirty!)
        (render-loop!)
        (reset! alive? true)

        {:force-render!   force-render!
         :request-render! dirty!
         :submit-local!   submit-local!
         :current-timer   current-timer
         :alive?          alive?
         :stop!           stop!}))))

(extend-type oak/QueryComponent

  IRender
  (render [component options]
    (let [{:keys [oracle target
                  initial-model initial-omodel
                  model-atom omodel-atom
                  intent
                  on-action]
           :or   {oracle    (oracle/make)
                  intent    (async/chan)
                  on-action (fn [_target _event])
                  target    (.-body js/document)}} options

          model (or model-atom (atom initial-model))
          omodel (or omodel-atom (atom initial-omodel))
          result (atom)
          kill-chan (async/chan)

          alive? (atom)
          current-timer (atom)
          dirty? (atom false)

          oracle-rts (atom)]

      (letfn [(dirty! [] (reset! dirty? true))

              (submit-oracle! [ev]
                (async/put! intent [:oracle ev]))

              (submit-local! [ev]
                (on-action :local ev)
                (let [new-model (oak/step component ev @model)]
                  (when (not= @model new-model)
                    (reset! model new-model)
                    (dirty!))))

              (update-result! []
                (let [subst (oracle/substantiate oracle @omodel component @model)]
                  (reset! result (:result subst))
                  (oracle/refresh oracle @omodel (:queries subst) submit-oracle!)))

              (force-render! []
                (update-result!)
                (q/render (component @model @result submit-local!) target)
                (reset! dirty? false))

              (render! [] (when @dirty? (force-render!)))

              (render-loop! []
                (render!)
                (let [timer (doto (AnimationDelay. render-loop!) .start)]
                  (reset! current-timer timer)))

              (oracle-loop! []
                (asyncm/go-loop []
                  (let [[action-pair resolved-chan] (async/alts! [intent kill-chan])]
                    (when (not= resolved-chan kill-chan)
                      (when-let [[_oracle-kw action] action-pair]
                        (on-action :oracle action)
                        (let [new-omodel (oracle/step oracle action @omodel)]
                          (when (not= @omodel new-omodel)
                            (reset! omodel new-omodel)
                            (dirty!)))
                        (recur))))))

              (stop! []
                (reset! alive? false)
                (async/put! kill-chan true)
                (oracle/stop oracle @oracle-rts)
                (q/unmount target)
                (when-let [timer @current-timer]
                  (.stop timer)))]

        (dirty!)
        (reset! oracle-rts (oracle/start oracle submit-oracle!))
        (oracle-loop!)
        (render-loop!)
        (reset! alive? true)

        {:force-render!   force-render!
         :request-render! dirty!
         :submit-local!   submit-local!
         :submit-oracle!  submit-oracle!
         :current-timer   current-timer
         :alive?          alive?
         :stop!           stop!}))))

