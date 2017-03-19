(ns cmskern.login.events
  (:require [re-frame.core :as rf]
            [ajax.core     :as ajax]

            [accountant.core :as accountant]
            [taoensso.timbre :as log]
            [day8.re-frame.http-fx] 

            [cmskern.session :as session]
            [cmskern.routes :as route]
            ))

(rf/reg-event-db
 ::on-error
 rf/debug
 (fn [db [_ txid result]]
   (log/debug ::bad-http-result result)
   (assoc-in db [:results txid] {:status :error :errors (:error (:response result))})
   ))

(rf/reg-event-fx
 ::on-success
 (fn [{:keys [db]} [_ txid result]]
   (log/debug ::good-http-result result)
   {:db (update-in (assoc db :waiting false) [:results] dissoc txid)
    :localstorage [:set [:token (:token (:result result))]]
    :dispatch-n (list [:handle-db-change (:result result)] [:handle-token-response (:result result)])
    :nav "/"
    }
   ))

(rf/reg-fx
 :nav
 (fn [url]
  (accountant/navigate! url) )
 )
(rf/reg-event-fx
 :login/logout
 (fn [{:keys [db]} [_]]
   (log/debug :login/logout)
   {:db nil
    :nav (route/make-path :login-page)
    :localstorage [:remove :token]
    }
   ))

(rf/reg-event-fx
 :login/submit-login
 rf/debug
 (fn [{:keys [db]} [_ txid data]]
   (log/debug ::data data)
   (let [
         params {:cmd "auth"
                 :args [(:email data)
                        (:password data)]
                 :eid txid}
         _ (log/debug ::params params)]
     {:db   (assoc-in (assoc db :waiting true)
                      [:results txid] {:status :waiting})
      :http-xhrio {:method          :post
                   :uri             "/c"
                   :timeout         2000
                   :params          params
                   :response-format (ajax/transit-response-format)
                   :format (ajax/transit-request-format)
                   :on-success      [::on-success txid]
                   :on-failure      [::on-error txid]}})))

(rf/reg-event-fx
 :login-page
 [rf/debug]
 (fn [{:keys [db]} [_ route]]
   (let [{:keys []} (:route-params route)]
     {:db (assoc db :current-db nil 
                 :route route)
      })
   ))
