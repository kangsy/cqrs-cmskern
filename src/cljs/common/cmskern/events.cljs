(ns cmskern.events
  (:require
   [re-frame.core :as rf]
            [accountant.core :as accountant]
            [taoensso.timbre :as log]
            [ajax.core :as ajax :refer [GET POST PUT DELETE]]

            [cmskern.config :as config]
            [cmskern.websockets :as ws]
            [cmskern.session :as session]
            ))

(rf/reg-cofx               ;; new registration function
 :local-store
 (fn [coeffects local-store-key]
   (assoc coeffects
          :local-store
          (.get session/storage local-store-key))))

(rf/reg-event-db
 :handle-db-change
 rf/debug
 (fn [db [_ resp]]
   (let [trans (:transact resp)]
     (loop [coll (apply array-map (:db/add trans)) reg db]
       (if (empty? coll)
         reg
         (let [[k v] (first coll)]
           (recur (rest coll) (assoc-in reg k v))
           )))
     )
   ))

(rf/reg-cofx
 :logged-in
 (fn [cfx _]
   (when-not (:token (:db cfx))
     (accountant/navigate! "/login")
     ) cfx))
(rf/reg-fx
 :dispatch-current-url
 (fn []
   (log/debug
    :dispatch-current-url
    (aget js/window "location" )
    )
   (accountant/dispatch-current!)))

;; this is called after auth - after login-page-submit or ws-auth
(rf/reg-event-fx
 :handle-token-response
 rf/debug
 (fn [{:keys [db]} [_ resp]]
   (log/debug ::handle-token-response resp)
   (let [token (:token resp)]
     (merge
      {:db (merge (assoc db :token token); (when-not token {:route {:handler :login-page}})
                  nil
                  )
       ;:dispatch [:handle-db-change resp]
       }
      nil
      ;{:nav (if token "/" "/login")}
      (when token {:dispatch-current-url nil})
      )
     )
     ))

(rf/reg-event-fx
 :initialize
 []
 (fn  [cofx _]
   (let [
         db (:db cofx)]
     (log/debug ::initialize )
     {
      :db {:name "cmskern"
           :databases []
           :current-db nil
           :db nil
           }})))

(rf/reg-event-fx
 :new-content-page
 rf/debug
 (fn [{:keys [db]} [_ route]]
   (let [{:keys [dbid ct]} (:route-params route)]
     {:db (assoc db :current-ct ct
                 :route route)
      :dispatch [:select-database dbid]
      ;:dispatch-later [{:ms 200}]
      })
   ))

(rf/reg-event-fx
 :select-database
 rf/debug
 (fn [{:keys [db]} [_ dbid]]
   (log/debug ::dbid dbid)
   (when (and (:user db) (:token db))
     (let [
           ]
       {:db   (assoc-in (dissoc db :cuurent-cts)
                        [:current-db] dbid)
        :send-ws [[:db/select {:token (:token db)
                               :db dbid}]
                  2000
                  (fn [res] (rf/dispatch [:handle-db-change res]))
                  ]
        }))))

(rf/reg-event-fx
 :set-route
 (fn [{:keys [db]} [_ route]]
   (log/debug :params route)
   {:db (assoc db :route route)
    :dispatch-later [{:ms 200 :dispatch [:select-database (:dbid (:route-params route))]}
                     {:ms 800 :dispatch [(:handler route) (:route-params route)]}]}
   ))

(rf/reg-event-db
 :write-to
 rf/debug
 (fn [db [_ path data]]
   (assoc-in db path data)
   ))

(rf/reg-event-db
 :cleanup
 rf/debug
 (fn [db [_ path]]
   (update-in db (butlast path) dissoc (last path))
   ))

(rf/reg-event-db
 :db/delete
 rf/debug
 (fn [db [_ path]]
   (if (get-in db (butlast path))
     (update-in db (butlast path) dissoc (last path))
     db)
   ))

;; ------------------------------------------------------------------------------
(rf/reg-event-db
 :db/add
 ;;rf/debug
 (fn [db [_ path data]]
   (assoc-in db path data)
   ))


(rf/reg-event-fx
 :db-query-callout
 rf/debug
 (fn [{:keys [db]} [_ uuid {:keys [query] :as args}]]
   {:db (assoc-in db [:results uuid :result] nil)
    :send-ws [[[:db/count (assoc args
                                 ;; zum zählen den query-string ersetzen
                                 :query (:find query)
                                 :token (:token db))]
               config/default-timeout
               (fn [res] (rf/dispatch [:db/add [:results uuid :count] res]))]
              [[:db/query (assoc args :token (:token db))]
               config/default-timeout
               (fn [res] (rf/dispatch [:db/add [:results uuid :result] res]))]]}
   ))

(rf/reg-event-fx
 :gfs-query-callout
 rf/debug
 (fn [{:keys [db]} [_ uuid {:keys [query] :as args}]]
   {:db (assoc-in db [:results uuid] nil)
    :send-ws [[:db/gfs-query (assoc args :token (:token db))]
               config/default-timeout
               (fn [res] (rf/dispatch [:db/add [:results uuid] res]))]}
   ))

(rf/reg-event-fx
 :gfs-delete
 rf/debug
 (fn [{:keys [db]} [_ uuid args callback]]
   {:db (assoc-in db [:results uuid] nil)
    :send-ws [[:db/gfs-delete (assoc args :token (:token db))]
              config/default-timeout
              (fn [res] (when uuid (rf/dispatch [:db/add [:results uuid] res]))
                (when callback (callback res)))]}
   ))

(rf/reg-event-db
 ::gfs-upload-error
 rf/debug
 (fn [db [_ txid result]]
   (log/debug ::bad-http-result result)
   (assoc-in db (vec (flatten [:results txid])) {:status :error :errors (:error (:response result))})
   ))

(rf/reg-event-fx
 ::gfs-upload-success
 (fn [{:keys [db]} [_ txid res]]
   (log/debug ::good-http-result res)
   {:db (update-in (assoc db :waiting false) (vec (flatten [:results txid])) assoc :status {:status "success"
                                                                                         :msg (str (:_id res) " erfolgreich gesichert")})
    }
   ))
 (rf/reg-event-fx
 :gfs-upload-file
 rf/debug
 (fn [{:keys [db]} [_ dbid uuid file ]]
   (.log js/console ::gfs-upload-file dbid file)
   (comment
     (when false
       (let [upload (doto
                        (js/FormData.)
                      (.append "upload" file)
                      )]
         (log/debug ::gfs-upload-file-cljs :inner dbid upload file)
         (.log js/console ::gfs-upload-file :inner dbid upload file)
         (POST (str "/_asset/" dbid )
               {:body upload
                :headers {:authorization (str "Bearer " (:token db))}
                :response-format (ajax/raw-response-format)
                :format :raw
                :keywords? true
                :handler #(rf/dispatch [::on-success uuid %1])
                                        ;:error-handler handle-response-error
                })
         (assoc db :waiting? true))
       ))
   (when file
     (let [upload (doto (js/FormData.) (.append "upload" file))]
       (.log js/console ::gfs-upload-file :inner dbid upload)
       {:db (assoc-in db (vec (flatten [:results uuid])) {:status {:status :waiting}})
        :http-xhrio {:method          :post
                     :uri             (str "/_asset/" dbid )
                     :timeout         2000
                     :body          upload
                     :response-format (ajax/raw-response-format)
                     :headers {:authorization (str "Bearer " (:token db))}
                     :format :raw
                     :keywords? true
                     :on-success      [::gfs-upload-success uuid]
                     :on-failure      [::gfs-upload-error uuid]}}
       ))
   ))
