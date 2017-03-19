(ns cmskern.index.events
  (:require [re-frame.core :as rf]
            [accountant.core :as accountant]
            [taoensso.timbre :as log]

            [cmskern.config :as config]
            ))


(rf/reg-event-fx
 :db-index-page
 [rf/debug (rf/inject-cofx :query-dbs)]
 (fn [{:keys [db]} [_ route]]
   (let [{:keys [dbid]} (:route-params route)]
     {:db (assoc db :current-db nil 
                 :contents nil
                 :route route)
      :dispatch [:select-database dbid]
      ;:dispatch-later [{:ms 200 }]
      })
   ))

(rf/reg-event-fx
 :index-page
 [rf/debug (rf/inject-cofx :query-dbs)]
 (fn [{:keys [db]} [_ route]]
   (let [{:keys []} (:route-params route)]
     {:db (assoc db :current-db nil 
                 :contents nil
                 :route route)
      })
   ))

(rf/reg-event-fx
 :ct-index-page
 [rf/debug (rf/inject-cofx :query-dbs)]
 (fn [{:keys [db]} [_ route]]
   (let [{:keys [dbid ctid]} (:route-params route)]
     {:db (assoc db :current-db dbid
                 :contents nil
                 :current-ctid ctid
                 :current-cts nil
                 :route route)
      :send-ws [[[:db/find {:token (:token db)
                            :db dbid
                            :col "contentTypes"
                            :query {}}]
                 config/default-timeout
                 (fn [res] (rf/dispatch [:db/add [:current-cts] res]))
                 ]
                [[:db/find {:token (:token db)
                            :db dbid
                            :col "content"
                            :query {:_type ctid :_status {"$gt" 20}}}]
                 config/default-timeout
                 (fn [res] (rf/dispatch [:db/add [:contents] res]))
                 ]]
      })
   ))
