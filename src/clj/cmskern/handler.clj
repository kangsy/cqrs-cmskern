(ns cmskern.handler
  (:require [mount.core :refer [defstate]]
            [taoensso.timbre :as log]
            [clj-uuid :as uuid]
            [clojure.core.async :refer [put! chan <! >! go-loop close!] :as a]

            [cmskern.consumer :as dc]
            [cmskern.producer :as dp]
            [cmskern.ui.commands]
            [cmskern.admin]
            [cmskern.content]
            [cmskern.auth]
            ))

(def commands-registry
  {
   "auth" {:fn 'cmskern.auth/auth-by-id-and-password
           :result-handling :sync ;; sync, async, void
           :middleware cmskern.ui.commands/auth
           }
   "save-db" {:fn 'cmskern.admin/save-database
              :result-handling :sync
              :middleware cmskern.ui.commands/save-db}
   "change-user-password" {:fn 'cmskern.admin/change-user-password
                :result-handling :async
                :middleware cmskern.ui.commands/change-user-password}
   "save-user" {:fn 'cmskern.admin/save-user
                :result-handling :sync
                :middleware cmskern.ui.commands/save-user}
   "save-ct" {:fn 'cmskern.admin/save-content-type
              :result-handling :sync
              :middleware cmskern.ui.commands/save-ct}
   "unpublish-content" {:fn 'cmskern.content/unpublish-content
                      :result-handling :sync
                      :middleware cmskern.ui.commands/inject-username}
   "publish-content" {:fn 'cmskern.content/publish-content
                   :result-handling :sync
                   :middleware cmskern.ui.commands/inject-username}
   "delete-content" {:fn 'cmskern.content/delete-content
                   :result-handling :sync
                   :middleware cmskern.ui.commands/inject-username}
   "save-content" {:fn 'cmskern.content/save-content
              :result-handling :sync
              :middleware cmskern.ui.commands/inject-username}
   })


(defn not-found-error
  [& args]

  (log/error ::not-found-called args)
  )
(defn id-wrapper
  [f]
  (fn [record]
    (apply f (:args record))
    ))

(defn consumer-command-handler
  "Gets a command-record (from kafka).
  Maps the command's name to the artifact.
  Runs the command and returns the result via kafka
  Returns prodcuer metadata"
  [{:keys [value] :as record} & {:keys [return-topic]}]
  (log/debug ::consumer-command-handler record)
  (let [{:keys [cmd args eid]} value
        id (uuid/v1)
        registered (get commands-registry cmd)
        result (when registered
                 (log/debug ::consumer-command-handler :registered registered)
                 (let [resolved (resolve (:fn registered))]
                   (log/debug ::consumer-command-handler :resolved resolved value)
                   (when resolved
                     (try
                       (((or (:middleware registered) id-wrapper)
                         resolved
                         )
                        value)
                       (catch Exception e e)))))
        data {:parent eid
              :eid id
              :result result}
        ]
    (log/debug ::sending data)
    (dp/send! dp/producer (or return-topic "events") data)
    ))

(defn start-command-handler
  ""
  [process-fn]
  (let [
        incoming (first dc/commands-ch)
        ]

    (a/go-loop []
      (when-let [record (a/<! incoming)]
        (when (= :record (:type record))
          (log/debug ::incoming record)
          (process-fn record))
        (recur)))
    ))

(defstate handler
  :start (start-command-handler consumer-command-handler)
  :stop (a/close! handler))
