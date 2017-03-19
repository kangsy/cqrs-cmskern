(ns cmskern-devcards.misc
  (:require
   [reagent.core :as r]
   [sablono.core :as sab :include-macros true]
   [devcards.core :as dc :include-macros true]

   [cljs.test :as t :include-macros true :refer-macros [testing is]]

   [cmskern.views.misc :as misc]
   [cmskern.views.pager :as pager])
  (:require-macros
   [devcards.core :refer [defcard deftest]]))



(defcard Spinner
  (dc/reagent
   [:div nil
    misc/spinner
    ]))
(defcard Pagination
  (dc/reagent
   [:div nil
    [:h5 nil "40 20 " (int (Math/ceil (/ 40 20)))]
    [pager/pagination
     {:total 40 :limit 20}]
    [:br]
    [:h5 nil "146 30"]
    [pager/pagination
     {:total 146 :limit 30}]]))
