(ns cmskern-devcards.views.login
  (:require
   [reagent.core :as r]
   [sablono.core :as sab :include-macros true]
   [devcards.core :as dc :include-macros true]

   [cljs.test :as t :include-macros true :refer-macros [testing is]]

   [cmskern.views.forms :as vf]
   [cmskern.login.views :as lv])
  (:require-macros
   [devcards.core :refer [defcard deftest]])
  )

(defcard Login-Panel

  (dc/reagent
   [:div.pure-g.login-page
    [lv/login-panel]])
  )
