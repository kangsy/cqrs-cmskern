(ns cmskern-devcards.core
  (:require
   [reagent.core :as r]
   [sablono.core :as sab :include-macros true]
   [devcards.core :as dc :include-macros true]
   [taoensso.timbre :as log]
   [mount.core :as mount]
   [re-frisk.core :refer [enable-re-frisk!]]
   [devtools.core :as devtools]

   [cljs.test :as t :include-macros true :refer-macros [testing is]]

   [cmskern-devcards.forms]
   [cmskern-devcards.views.login]
   [cmskern-devcards.views.file]
   [cmskern-devcards.components.buttons]
   [cmskern-devcards.widgets]
   [cmskern-devcards.misc]
   )
  (:require-macros
   [devcards.core :refer [defcard deftest]]))

(defn ^:export init []
  (log/info ::init "--== Starting Devcard System ==--")
  (enable-console-print!)
  (devtools/install!)
  (doseq [component (:started (mount/start))]
    (log/info component "started"))
  (enable-re-frisk!)
  )

