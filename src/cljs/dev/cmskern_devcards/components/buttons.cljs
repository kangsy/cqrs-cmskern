(ns cmskern-devcards.components.buttons

  (:require
   [reagent.core :as r]
   [sablono.core :as sab :include-macros true]
   [devcards.core :as dc :include-macros true]

   [cljs.test :as t :include-macros true :refer-macros [testing is]]

   [cmskern.views.buttons :as vb])
  (:require-macros
   [devcards.core :refer [defcard deftest]])
  )

(defcard Button

  (dc/reagent
   [vb/button :label "Standard Button"]))
