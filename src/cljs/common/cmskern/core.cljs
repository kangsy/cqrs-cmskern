(ns cmskern.core
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [devtools.core :as devtools]
            [accountant.core :as accountant]
            [bidi.bidi :as bidi]
            [re-frisk.core :refer [enable-re-frisk!]]
            [mount.core :as mount]
            [taoensso.timbre :as log]
            [day8.re-frame.http-fx] 

            [cmskern.websockets]
            [cmskern.routes :as route]
            [cmskern.events]

            [cmskern.subs]
            [cmskern.views.core :as views]
            [cmskern.config :as config]
            ))


(defn dev-setup []
  (when config/debug?
    (enable-console-print!)
    (println "dev mode")
    (devtools/install!)))

(defn mount-root []
  (r/render [views/layout]
                  (.getElementById js/document "app")))

(defn ^:export init []
  (log/info ::init "--== Starting System ==--")
  (accountant/configure-navigation!
   {:nav-handler (fn
                   [path]
                   (let [match (bidi/match-route route/cmskern-routes path)]
                     (log/debug ::matching-path path match)
                     (rf/dispatch [(:handler match) match])
                     ))
    :path-exists? (fn [path]
                    (boolean (bidi/match-route route/cmskern-routes path)))
    })
  (doseq [component (:started (mount/start))]
    (log/info component "started"))
  (rf/dispatch-sync [:initialize])
  (enable-re-frisk!)
  (dev-setup)
  (mount-root)
  )
