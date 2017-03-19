(ns cmskern.websockets
  (:require 
   [taoensso.timbre :as log]
   [cljs.core.async :refer [<! >! put! close! chan]]
   [re-frame.core :as rf]
   [reagent.core :as r]
   [mount.core :as mount]
   [taoensso.sente  :as sente :refer (cb-success?)]
   [taoensso.sente.packers.transit :as sente-transit]
   [accountant.core :as accountant]

   [cmskern.session :as session]
   [cmskern.config :as config]

   [cljs.core.async :as a])
  (:require-macros [mount.core :refer [defstate]]
                   [cljs.core.async.macros :refer [go go-loop]]))

(defn handle-msg [new-msg]
  ;; todo
  (log/info ::handle-msg new-msg)
  (let [transactions (:transact new-msg)]
    (doseq [[k v] (apply array-map (:db/add transactions))]
      (do
        (log/debug "updating " k v)
        (rf/dispatch [:db/add k v])
        )
      )
    (doseq [path (:db/delete transactions)]
      (do
        (log/debug "deleting " path)
        (rf/dispatch [:db/delete path])
        )
      )))



(defstate sente-socket
  :start  (let [s (sente/make-channel-socket! "/chsk" ; Note the same path as before
                                              {:type :auto ; e/o #{:auto :ajax :ws}
                                               :packer (sente-transit/get-transit-packer
                                                        :json
                                                        {}
                                                        {;:handlers {"bson-objectid" (fn [o] (str o))}
                                                         }
                                                        )
                                               })
                ]
            (log/info ::sente-socket :started)
            (.log js/console s)
            s))

(defmulti -event-msg-handler
  "Multimethod to handle Sente `event-msg`s"
  :id ; Dispatch on event-id
  )

(defn event-msg-handler
  "Wraps `-event-msg-handler` with logging, error catching, etc."
  [{:as ev-msg :keys [id ?data event]}]
  (-event-msg-handler ev-msg))

(defstate ws-router
  :start (sente/start-client-chsk-router! (:ch-recv @sente-socket) event-msg-handler)
  :stop (when-let [stopf @ws-router] (stopf)))

(defn send! [& args]
  (log/debug :send! args)
  (apply (:send-fn @sente-socket) args))

(defn send-auth! []
  (log/debug ::ws-auth :called)
  (when-let [token (.get session/storage :token)]
    (log/debug ::ws-auth :init token)
    (send! [:token/auth {:token token}]
           3000 (fn [resp]
                  (if-not (:errors resp)
                    (do
                      (rf/dispatch [:handle-db-change resp])
                      (rf/dispatch [:handle-token-response resp]))
                    (do
                      (accountant/navigate! "/login")
                      )))))
  )

(defmethod -event-msg-handler
  :default ; Default/fallback case (no other matching handler)
  [{:as ev-msg :keys [event]}]
  (log/warn "Unhandled event: %s" event))

(defmethod -event-msg-handler :chsk/state
  [{:as ev-msg :keys [?data]}]
  (let [[old-state-map new-state-map] ?data]
    (if (:first-open? new-state-map)
      (do
        (log/info "Channel socket successfully established!: %s" new-state-map)
        (if (.get session/storage :token)
          (send-auth!)
          (do
            (accountant/navigate! "/login")
            (accountant/dispatch-current!))))
      (do
        (log/info "Channel socket state change: %s"              new-state-map)
        (rf/dispatch [:db/add [:connection-lost? (:open? new-state-map)]])
        ))))

(defmethod -event-msg-handler :chsk/recv
  [{:as ev-msg :keys [?data]}]
  (log/info "Push event from server: %s" ?data))

(defmethod -event-msg-handler :chsk/handshake
  [{:as ev-msg :keys [?data]}]
  (let [[?uid ?csrf-token ?handshake-data] ?data]
    (log/info "Handshake: %s" ?data)))

(rf/reg-fx
 :send-ws
 (fn [[& args]]
   (if (keyword? (ffirst args))
     (apply send! args)
     (doseq [arg args]
       (when arg
         (apply send! arg))
       )
     )
   ))
