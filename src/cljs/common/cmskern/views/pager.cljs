(ns cmskern.views.pager
  (:require-macros
   [reagent.ratom :refer [reaction]]
   [cmskern.ui.core :refer [handler-fn]])
  (:require
   [reagent.core :as r]
   [taoensso.timbre :as log]

   [cmskern.functions :as f :refer [tr]]
   [cmskern.views.utils :refer [deref-or-value]]
   ))


(defn pagination
  [{:keys [total start limit on-change]
    :or {limit 30
         start 1
         on-change (fn [p] (log/debug ::pager :change p))
         }}]
  (let [
        model (r/atom {:total total :start start :limit limit})
        current (r/atom (:start @model))
        maxp (reaction (int (Math/ceil (/ (:total @model) (:limit @model)))))
        prev-fn (handler-fn (.preventDefault event)
                            (log/debug ::pagination :prev @current @maxp)
                            (when (> @current 1)
                              (swap! current dec)
                              (on-change @current))
                            )
        next-fn (handler-fn (.preventDefault event)
                            (log/debug ::pagination :next @current @maxp)
                            (when (< @current @maxp)
                              (swap! current inc)
                              (on-change @current))
                            )
        select-fn (fn [p] (handler-fn (.preventDefault event)
                                      (reset! current p)
                                      (on-change @current)))
        ]
     (fn
       [{:keys [total limit start]}]
       (let [
             latest {:total total :start start :limit limit}
             ]
         (when-not (= latest @model)
           (reset! model latest))
         (log/debug ::pagination :inner total limit @current @maxp)
         [:div
          [:ul.pagination.pagination-sm
           [:li { :class (when (< @current 2) "disabled")} [:a {:on-click prev-fn :href "#"} "<<"]]
           (doall (map (fn [idx] ^{:key idx}
                         [:li { :class (when (= @current idx) "active")}
                          [:a {:on-click (select-fn idx) :href "#"} idx]]) (range 1 (inc @maxp))))
           [:li { :class (when (>= @current @maxp) "disabled")} [:a {:on-click next-fn :href "#"} ">>"]]]
          
          [:p.small nil (tr ["%1 - %2 von gesamt: %3 - ( %4 / Seite)"] [(inc (* (dec @current) (:limit @model))) (min (* @current (:limit @model)) (:total @model)) (:total @model) (:limit @model)])]
          ])

       )))
