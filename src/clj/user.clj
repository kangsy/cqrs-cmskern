(ns user
  (:require [mount.core :as mount]
            [taoensso.timbre :as log]
            [cmskern.main :refer [init]]
            ))

(defn start []
  (init)
  (doseq [comp (:started (mount/start-without #'cmskern.main/repl-server))]
    (log/info comp "started"))
  )

(defn stop []
  (doseq [comp (:stopped (mount/stop-except #'cmskern.main/repl-server))]
    (log/info comp "stopped"))
  )

(defn restart []
  (stop)
  (start))
