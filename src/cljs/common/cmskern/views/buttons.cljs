(ns cmskern.views.buttons
  (:require-macros [cmskern.ui.core :refer [handler-fn]])
  (:require
   [reagent.core :as r]

   [cmskern.views.utils :refer [deref-or-value]]
   ))

(defn button
  "Returns the markup for a basic button"
  []
  (let [showing? (r/atom false)]
    (fn
      [& {:keys [label on-click tooltip tooltip-position disabled? class style attr]
          :or   {class "pure-button-primary pure-button"}
          :as   args}]
      (when-not tooltip (reset! showing? false)) ;; To prevent tooltip from still showing after button drag/drop
      (let [disabled? (deref-or-value disabled?)
            the-button [:button
                        (merge
                          {:class    class
                           :disabled disabled?
                           :on-click (handler-fn
                                       (when (and on-click (not disabled?))
                                         (on-click event)))}
                          (when tooltip
                            {:on-mouse-over (handler-fn (reset! showing? true))
                             :on-mouse-out  (handler-fn (reset! showing? false))})
                          attr)
                        label]]
        (when disabled?
          (reset! showing? false))
        the-button
        ))))

