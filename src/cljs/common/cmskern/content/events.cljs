(ns cmskern.content.events
  (:require [re-frame.core :as rf]
            [accountant.core :as accountant]
            [taoensso.timbre :as log]

            [cmskern.config :as config]
            [cmskern.routes :as route]
            ))

(rf/reg-event-fx
 :content-edit-page
 [rf/debug (rf/inject-cofx :query-dbs)]
 (fn [{:keys [db]} [_ route]]
   (let [{:keys [cid dbid ctid]} (:route-params route)]
     {:db (assoc db :current-db dbid
                 :contents nil
                 :current-content nil
                 :current-content-data nil
                 :current-content-copy nil
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
                (log/debug ::cid-0 cid)
                (when cid
                  (log/debug ::cid cid)
                  [[:db/get {:token (:token db)
                              :db dbid
                              :col "content"
                              :query {:_id (js/parseInt cid)}}]
                   config/default-timeout
                   (fn [res]
                     (rf/dispatch [:db/add [:current-content-copy] res])
                     (rf/dispatch [:db/add [:current-content-data] (:data res)])
                     (rf/dispatch [:db/add [:current-content] res]))
                   ])
                ]
      })
   ))

(rf/reg-event-fx
 :fetch-version
 rf/debug
 (fn [{:keys [db]} [_ dbid version callback-fn]]
   (let []
     (log/debug ::fetch-version version)
     {:send-ws [[:db/get {:token (:token db)
                          :db dbid
                          :col "versions"
                          :query {:_id version}
                          }]
                config/default-timeout
                callback-fn]
      })
   ))

(rf/reg-event-db
 :incure-data
 rf/debug
 (fn [db [_ data]]
   (assoc-in db [:current-content :data] data))
 )

(rf/reg-event-fx
 :submit-content
 rf/debug
 (fn [{:keys [db]} [_ dbid ctid cid args uuid]]
   (let [data (js->clj args :keywordize-keys true)]
     (log/debug ::submit-content data)
     {:send-ws [[:user/command {:token (:token db)
                                :cmd "save-content"
                                :args [{:dbid dbid
                                        :ctid ctid
                                        :cid (when cid (js/parseInt cid))
                                        :data (:formData data)
                                        :username (:username (:user db))}]}]
                config/default-timeout
                (fn [res] (rf/dispatch [:db/add [:results :global] {:status {:status "success"
                                                                             :msg (str (:_id res) " erfolgreich gesichert")}
                                                                    :result res}])
                  (accountant/navigate! (route/make-path :content-edit :dbid dbid :ctid ctid :cid (:_id res))))
                ]
      })
   ))

(rf/reg-event-fx
 :content/publish
 rf/debug
 (fn [{:keys [db]} [_ dbid ctid cid]]
   (let []
     (log/debug :content/publish )
     {:send-ws [[:user/command {:token (:token db)
                                :cmd "publish-content"
                                :args [{:dbid dbid
                                        :ctid ctid
                                        :cid (when cid (js/parseInt cid))
                                        :username (:username (:user db))}]}]
                config/default-timeout
                (fn [res] (rf/dispatch
                           [:db/add
                            [:results :global]
                            {:status {:status "success"
                                      :msg (str (:_id res) " erfolgreich publiziert")}
                             :result res}])
                  (accountant/navigate! (route/make-path :content-edit :dbid dbid :ctid ctid :cid (:_id res)))
                  )
                ]
      })
   ))

(rf/reg-event-fx
 :content/un-publish
 rf/debug
 (fn [{:keys [db]} [_ dbid ctid cid]]
   (let []
     (log/debug :content/unpublish )
     {:send-ws [[:user/command {:token (:token db)
                                :cmd "unpublish-content"
                                :args [{:dbid dbid
                                        :ctid ctid
                                        :cid (when cid (js/parseInt cid))
                                        :username (:username (:user db))}]}]
                config/default-timeout
                (fn [res] (rf/dispatch
                           [:db/add [:results :global]
                            {:status {:status "success"
                                      :msg (str (:_id res) " erfolgreich un-publiziert")}
                             :result res}])
                  (accountant/navigate! (route/make-path :content-edit :dbid dbid :ctid ctid :cid (:_id res)))
                  )
                ]
      })
   ))

(rf/reg-event-fx
 :content/delete
 rf/debug
 (fn [{:keys [db]} [_ dbid ctid cid]]
   (let []
     (log/debug :content/unpublish )
     {:send-ws [[:user/command {:token (:token db)
                                :cmd "delete-content"
                                :args [{:dbid dbid
                                        :ctid ctid
                                        :cid (when cid (js/parseInt cid))
                                        :username (:username (:user db))}]}]
                config/default-timeout
                (fn [res] (rf/dispatch
                           [:db/add [:results :global]
                            {:status {:status "success"
                                      :msg (str (:_id res) " erfolgreich gelöscht")}
                             :result res}])
                  (accountant/navigate! (route/make-path :ct-index-page :dbid dbid :ctid ctid))
                  )
                ]
      })
   ))

