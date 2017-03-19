(ns cmskern.ui.websockets
  (:import org.bson.types.ObjectId)
  (:require
   [mount.core :refer [defstate]]
   [taoensso.timbre :as log]
   [clj-uuid :as uuid]
   [taoensso.sente :as sente]
   [taoensso.sente.server-adapters.http-kit      :refer (get-sch-adapter)]
   [taoensso.sente.packers.transit :as sente-transit]
   [clojure.core.async :as a]

   [conqueress.monads :refer [attempt-all]]
   [conqueress.failure :refer [fail failure?]]
   [conqueress.core :refer [command-handler query-handler] :as cqrs]

   [cmskern.functions :as f]
   [cmskern.auth :as auth]
   [cmskern.db :as db]
   [cmskern.utils :as u]
   [cmskern.ui.commands :as cmd]
   ))

(defn message-handler
  ""
  [{:keys [command token args] :as msg}]
  (log/debug ::respond-to-ws msg)
  (case command
    :init {:init true
           :transact {:db/add [[:databases] [{:name "ea2"} {:name "getspicy"}]
                               ]
                      :db/delete []}}
    :check (if (= "token-1234" token) token :disconnect)
    :refresh {:refresh true}
    msg)
  )

(defmulti -event-msg-handler
  "Multimethod to handle Sente `event-msg`s"
  :id ; Dispatch on event-id
  )

(defn event-msg-handler
  "Wraps `-event-msg-handler` with logging, error catching, etc."
  [{:as ev-msg :keys [id ?data event]}]
  (-event-msg-handler ev-msg) ; Handle event-msgs on a single thread
  ;; (future (-event-msg-handler ev-msg)) ; Handle event-msgs on a thread pool
  )

(defmethod -event-msg-handler
  :default ; Default/fallback case (no other matching handler)
  [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn]}]
  (let [session (:session ring-req)
        uid     (:uid     session)]
    (log/debug ::event-msg)
    (clojure.pprint/pprint ev-msg)
    (log/warn "Unhandled event: %s" event)
    (when ?reply-fn
      (?reply-fn {:umatched-event-as-echoed-from-from-server event}))))

(def ping-counts (atom 0))

(defmethod -event-msg-handler :user/command
  [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn]}]
  (log/info :user/command ev-msg)
  (attempt-all [
                uid (or (auth/check-token (:token ?data)) (fail :token-invalid))
                _ (log/debug :admin/command :uid uid)

                user (or (db/get "users" {:_id (ObjectId. uid)}) (fail :no-user))
                _ (log/debug :admin/command :user user)
                data (cmd/handle-command-result (cqrs/command-handler ?data))
                _ (log/debug :admin/command :result data)
                ]
               (when ?reply-fn
                 (?reply-fn data))
               #(log/error :admin/command :failure %)
               ))

(defmethod -event-msg-handler :admin/command
  [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn]}]
  (log/info :admin/command ev-msg)
  (attempt-all [
                uid (or (auth/check-token (:token ?data)) (fail :token-invalid))
                _ (log/debug :admin/command :uid uid)

                user (or (db/get "users" {:_id (ObjectId. uid)}) (fail :no-user))
                _ (log/debug :admin/command :user user)
                admin? (or (f/admin? user) (fail :no-admin))
                data (cmd/handle-command-result (cqrs/command-handler ?data))
                _ (log/debug :admin/command :result data)
                ]
               (when ?reply-fn
                 (?reply-fn data))
               #(log/error :admin/command :failure %)
               ))

(defmethod -event-msg-handler :db/find
  [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn]}]
  (log/info :db/find ev-msg)
  (attempt-all [
                dbid (or (:db ?data) (fail :no-db))
                col (or (:col ?data) (fail :no-collection))
                q (or (:query ?data) (fail :no-query))
                uid (or (auth/check-token (:token ?data)) (fail :token-invalid))
                user (or (db/get "users" {:_id (ObjectId. uid)}) (fail :no-user))
                result (db/find dbid col q)
                ]
               (when ?reply-fn
                 (?reply-fn result))
               #(log/error :db/find :failure %)
               )
  )

(defmethod -event-msg-handler :db/count
  [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn]}]
  (log/info :db/count ev-msg)
  (attempt-all [
                dbid (or (:db ?data) (fail :no-db))
                col (or (:col ?data) (fail :no-collection))
                q (or (:query ?data) (fail :no-query))
                uid (or (auth/check-token (:token ?data)) (fail :token-invalid))
                user (or (db/get "users" {:_id (ObjectId. uid)}) (fail :no-user))
                result (db/count dbid col q)
                ]
               (when ?reply-fn
                 (?reply-fn result))
               #(log/error :db/count :failure %)))

(defmethod -event-msg-handler :db/gfs-upload-file
  [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn]}]
  (log/info :db/gfs-upload-file ev-msg)
  (attempt-all [
                dbid (or (:db ?data) (fail :no-db))
                f (or (:file ?data) (fail :no-file))
                uid (or (auth/check-token (:token ?data)) (fail :token-invalid))
                user (or (db/get "users" {:_id (ObjectId. uid)}) (fail :no-user))
                result (db/gridfs-store-file dbid f)
                ]
               (when ?reply-fn
                 (?reply-fn result))
               #(log/error :db/query :failure %)))

(defmethod -event-msg-handler :db/gfs-query
  [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn]}]
  (log/info :db/gfs-query ev-msg)
  (attempt-all [
                dbid (or (:db ?data) (fail :no-db))
                q (or (:query ?data) (fail :no-query))
                uid (or (auth/check-token (:token ?data)) (fail :token-invalid))
                user (or (db/get "users" {:_id (ObjectId. uid)}) (fail :no-user))
                result (db/gridfs-query-total-files dbid q)
                ]
               (when ?reply-fn
                 (?reply-fn result))
               #(log/error :db/query :failure %)))

(defmethod -event-msg-handler :db/gfs-delete
  [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn]}]
  (log/info :db/gfs-delete ev-msg)
  (attempt-all [
                dbid (or (:db ?data) (fail :no-db))
                assetid (or (:id ?data) (fail :no-asset-id))
                uid (or (auth/check-token (:token ?data)) (fail :token-invalid))
                user (or (db/get "users" {:_id (ObjectId. uid)}) (fail :no-user))
                result (db/gridfs-remove dbid {:_id assetid})
                ]
               (when ?reply-fn
                 (?reply-fn result))
               #(log/error :db/query :failure %)))

(defmethod -event-msg-handler :db/query
  [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn]}]
  (log/info :db/query ev-msg)
  (attempt-all [
                dbid (or (:db ?data) (fail :no-db))
                col (or (:col ?data) (fail :no-collection))
                q (or (:query ?data) (fail :no-query))
                uid (or (auth/check-token (:token ?data)) (fail :token-invalid))
                user (or (db/get "users" {:_id (ObjectId. uid)}) (fail :no-user))
                result (db/query dbid col q)
                ]
               (when ?reply-fn
                 (?reply-fn result))
               #(log/error :db/query :failure %)))

(defmethod -event-msg-handler :db/get
  [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn]}]
  (log/info :db/get ev-msg)
  (attempt-all [
                dbid (or (:db ?data) (fail :no-db))
                col (or (:col ?data) (fail :no-collection))
                q (or (:query ?data) (fail :no-query))
                uid (or (auth/check-token (:token ?data)) (fail :token-invalid))
                user (or (db/get "users" {:_id (ObjectId. uid)}) (fail :no-user))
                result (db/get dbid col q)
                ]
               (when ?reply-fn
                 (?reply-fn result))
               #(log/error :db/get :failure %)))

(defmethod -event-msg-handler :db/select
  [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn]}]
  (log/debug :db/select ?data ?reply-fn)
  (attempt-all [
                dbid (or (:db ?data) (fail :no-db))
                uid (or (auth/check-token (:token ?data)) (fail :token-invalid))
                user (or (db/get "users" {:_id (ObjectId. uid)
                                          "databases" dbid}) (fail :no-user))
                cts (db/find dbid "contentTypes" {})
                contents (doall  (into {} (map (fn [ct]
                                                 (hash-map
                                                  (:name ct)
                                                  (db/query dbid "content"
                                                            {
                                                             :find {:_type (:name ct)
                                                                    :_status {"$gt" 20}}
                                                             :limit 10
                                                             :skip 0
                                                             :fields []
                                                             :sort (array-map :_modified -1)
                                                             }))) cts)))
                res {:transact {:db/add [[:current-cts] cts
                                         [:contents] contents]}}
                _ (log/debug :db/select :result res)
                ]
               (when ?reply-fn
                 (?reply-fn res)))
  )

(defmethod -event-msg-handler :token/auth
  [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn]}]
  (log/debug ::token-auth ?data ?reply-fn)
  (attempt-all [
                uid (or (auth/check-token (:token ?data)) (fail :token-invalid))
                _ (log/debug ::token-auth :uid uid)
                user (or (db/get "users" {:_id (ObjectId. uid)}) (fail :no-user))
                _ (log/debug ::token-auth :user user)
                new-tok (or (auth/generate-new-token uid) (fail :token-generation-failure))
                _ (log/debug ::token-auth :newtok new-tok)
                res {:token new-tok
                     :transact {:db/add [[:user] (dissoc user :password)]}}
                _ (log/debug ::token-auth :res res)
                ]
               (when ?reply-fn
                 (?reply-fn res))
               #(when ?reply-fn
                 (?reply-fn %)))
  )

(defmethod -event-msg-handler :chsk/ws-ping
  [_]
  (swap! ping-counts inc)
  (when (= 0 (mod @ping-counts 10))
    (println "ping counts: " @ping-counts)))

(defstate sente-socket
  :start (let [m (sente/make-channel-socket!
                  (get-sch-adapter)
                  {:packer (sente-transit/get-transit-packer
                            :json
                            {:handlers
                             {org.joda.time.DateTime u/joda-time-writer
                              org.bson.types.ObjectId u/bson-objectid-write-handler}}
                            {:handlers {"bson-objectid" u/bson-objectid-read-handler}})})]
           (log/info ::sente-socket :started )
           (clojure.pprint/pprint m)
           (log/info ::handler-fn (:ajax-get-or-ws-handshake-fn
                                   m))
           m))

(defn broadcast!
  [data]
  (let [uids (:any @(:connected-uids sente-socket))]
    (log/debug "Broadcasting server>user: %s uids" (count uids))
    (doseq [uid uids]
      ((:send-fn sente-socket) uid [:db/update data]))))


(defmethod cmd/-handle-command-result :default
  [{:as return-data :keys [type result]}]
  (if (failure? return-data)
    (log/error ::handle-command-result :default return-data)
    (log/debug ::handle-command-result :default return-data))
  result
  )

(defmethod cmd/-handle-command-result :return
  [{:as return-data :keys [type result]}]
  (log/debug ::handle-command-result :return return-data)
  result
  )

(defmethod cmd/-handle-command-result :broadcast
  [{:as return-data :keys [type result]}]
  (log/debug ::handle-command-result :broadcast return-data)
  ;; send-ws
  (broadcast! result)
  nil
  )

(defn err-handler
  [& args]
  (log/error ::ws-error args))

(defstate ws-router
  :start (sente/start-server-chsk-router! (:ch-recv sente-socket) event-msg-handler {:trace-evs? true :error-handler err-handler})
  :stop (when-let [stopf ws-router] (stopf)))
