(ns cmskern.admin.events
  (:require [re-frame.core :as rf]
            [taoensso.timbre :as log]
            [accountant.core :as accountant]
            [cognitect.transit :as t]

            [cmskern.config :as config]
            [cmskern.routes :as route]
            [cmskern.websockets :as ws]
            ))

(rf/reg-cofx
 :query-dbs
 (fn [cfx _]
   (ws/send! [:db/find {:token (:token (:db cfx))
                         :db "cmskern"
                         :col "databases"
                         :query {}}]
              config/default-timeout
              (fn [res] (rf/dispatch [:db/add [:databases] res])))
   cfx))

(rf/reg-event-fx
 :admin-page
 rf/debug
 (fn [{:keys [db]} [_ route]]
   (let [{:keys [dbid ct]} (:route-params route)]
     {:db (assoc db :current-db nil 
                 :route route)
      })
   ))

(rf/reg-event-fx
 :admin-dbs-page
 [rf/debug (rf/inject-cofx :query-dbs)]
 (fn [{:keys [db]} [_ route]]
   (let [{:keys [dbid ct]} (:route-params route)]
     {:db (assoc db :current-db nil 
                 :route route)
      })
   ))
(rf/reg-event-fx
 :admin-users-page
 rf/debug
 (fn [{:keys [db]} [_ route]]
   (let [{:keys [dbid ct]} (:route-params route)]
     {:db (assoc db :current-db nil 
                 :route route)
      :send-ws [[:db/find {:token (:token db)
                           :db "cmskern"
                           :col "users"
                           :query {}}]
                config/default-timeout
                (fn [res] (rf/dispatch [:db/add [:users] res]))
                ]
      })
   ))

(rf/reg-event-fx
 :admin/submit-change-pw
 rf/debug
 (fn [{:keys [db]} [_ uid {:keys [password password-confirm]}]]
   (let [
         ]
     (log/debug :admin/submit-user-form uid password password-confirm)
     {:send-ws [[:admin/command {:token (:token db)
                                 :cmd "change-user-password"
                                 :args [(t/tagged-value "bson-objectid" uid) password password-confirm]}]
                ]
      })
   ))
(rf/reg-event-fx
 :admin-submit-user-form
 rf/debug
 (fn [{:keys [db]} [_ args]]
   (let [data (js->clj args :keywordize-keys true)
         form-data (js->clj (:formData data))
         ]
     (log/debug ::admin-submit-user-form data)
     {:send-ws [[:admin/command {:token (:token db)
                                 :cmd "save-user"
                                 :args [form-data]}]
                config/default-timeout
                (fn [res] (accountant/navigate! (route/make-path :admin-users-page))
                  )]
      })
   ))

(rf/reg-event-fx
 :admin-user-edit-page
 [rf/debug (rf/inject-cofx :query-dbs)
  ]
 (fn [{:keys [db]} [_ route]]
   (let [{:keys [uid ct]} (:route-params route)]
     {:db (assoc db :route route :admin-data nil)
      :send-ws [[[:db/get {:token (:token db)
                           :db "cmskern"
                           :col "content-types"
                           :query {:name "user"}}]
                 config/default-timeout
                 (fn [res] (rf/dispatch [:db/add [:admin-schema] (or res config/default-user-schema)]))
                 ]
                (when uid
                  [[:db/get {:token (:token db)
                             :db "cmskern"
                             :col "users"
                             :query {:_id (t/tagged-value "bson-objectid" uid)}}]
                   config/default-timeout
                   (fn [res] (rf/dispatch [:db/add [:admin-data] res]))
                   ])]
      })))

(rf/reg-event-fx
 :admin-db-edit-page
 [rf/debug (rf/inject-cofx :query-dbs)
  ]
 (fn [{:keys [db]} [_ route]]
   (let [{:keys [dbid ct]} (:route-params route)]
     {:db (assoc db :route route :admin-data nil)
      :send-ws [[[:db/get {:token (:token db)
                           :db "cmskern"
                           :col "content-types"
                           :query {:name "database"}}]
                 config/default-timeout
                 (fn [res] (rf/dispatch [:db/add [:admin-schema] (or res config/default-db-schema)]))
                 ]
                (when dbid
                  [[:db/find {:token (:token db)
                             :db dbid
                             :col "contentTypes"
                             :query {}}]
                   config/default-timeout
                   (fn [res] (rf/dispatch [:db/add [:admin-cts] res]))
                   ])
                (when dbid
                  [[:db/get {:token (:token db)
                             :db "cmskern"
                             :col "databases"
                             :query {:dbid dbid}}]
                   config/default-timeout
                   (fn [res] (rf/dispatch [:db/add [:admin-data] res]))
                   ])]
      })))

(rf/reg-event-fx
 :admin-ct-edit-page
 rf/debug
 (fn [{:keys [db]} [_ route]]
   (let [{:keys [dbid ctid]} (:route-params route)]
     {:db (assoc db :route route :admin-data nil)
      :send-ws [[[:db/get {:token (:token db)
                            :db "cmskern"
                            :col "content-types"
                            :query {:name "content-type"}}]
                  config/default-timeout
                  (fn [res] (rf/dispatch [:db/add [:admin-schema] (or res config/default-ct-schema)]))
                 ]
                (when ctid
                  [[:db/get {:token (:token db)
                              :db dbid
                              :col "contentTypes"
                              :query {:name ctid}}]
                   config/default-timeout
                   (fn [res] (rf/dispatch [:db/add [:admin-data] (or res config/default-ct-schema)]))
                   ])]
      })
   ))

(rf/reg-event-fx
 :admin-submit-ct-form
 rf/debug
 (fn [{:keys [db]} [_ dbid args]]
   (let [data (js->clj args :keywordize-keys true)
         form-data (js->clj (:formData data))
         ]
     (log/debug ::admin-submit-ct-form data)
     {:send-ws [[:admin/command {:token (:token db)
                                 :cmd "save-ct"
                                 :args [dbid form-data]}]
                config/default-timeout
                (fn [res] (accountant/navigate! (route/make-path :admin-dbs-page))
                  )]
      })
   ))
(rf/reg-event-fx
 :admin-submit-db-form
 rf/debug
 (fn [{:keys [db]} [_ args]]
   (let [data (js->clj args :keywordize-keys true)]
     (log/debug ::admin-submit-db-form data)
     {:send-ws [[:admin/command {:token (:token db)
                                 :cmd "save-db"
                                 :args [(:formData data)]}]
                config/default-timeout
                (fn [res] (accountant/navigate! (route/make-path :admin-dbs-page))
                  )]
      })
   ))

