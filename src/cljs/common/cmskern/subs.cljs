(ns cmskern.subs
  (:require-macros [reagent.ratom :refer [reaction]]
                   [taoensso.timbre :as log])
  (:require [re-frame.core :as rf]
            [reagent.ratom :as ra]
            [cognitect.transit :as t]

            [cmskern.db :as cdb]
            ))

(rf/reg-sub
 :name
 (fn [db]
   (:name db)))

(rf/reg-sub
 :route
 (fn [db]
   (:route db)))


(rf/reg-sub :contents
 (fn [db _]
   (let [cont (:contents db)]
     (sort-by :_modified #(> %1 %2) cont))))

(rf/reg-sub
 :current-user
 (fn [db]
   (:user db)))

(rf/reg-sub
 :databases
 (fn [db]
   (:databases db)))

(rf/reg-sub
 :user-dbs
 :<- [:current-user]
 :<- [:databases]
 (fn [[user dbs]]
   (filter (fn [d] (some #(= % (:dbid d)) (:databases user))) dbs)
   ))

(rf/reg-sub
 :current-ctid
 (fn [db]
   (:current-ctid db)))

(rf/reg-sub
 :current-ct
 :<- [:current-cts]
 :<- [:current-ctid]
   (fn [[current-cts current-ctid] _]
     (some #(and (= current-ctid (:name %)) %) current-cts)))

(rf/reg-sub
 :current-schema
 :<- [:current-cts]
 :<- [:current-ctid]
 (fn [[current-cts current-ctid] _]
   (let [r (t/reader :json)
         ct (some #(and (= current-ctid (:name %)) %) current-cts)
         s (:jsonSchema ct)
         res (t/read r s)
         ]
     res
     )))

(rf/reg-sub
 :current-cts
 (fn [db]
   (sort-by :displayName
            (:current-cts db))))

(rf/reg-sub
 :current-db
 (fn [db]
   (:current-db db)))

(rf/reg-sub
 :initialised?
 (fn  [db _]
   (and (not (empty? db))
        (:route db)))) ;; do we have data

(rf/reg-sub
 :db
 (fn [db [_ path]]
   (get-in db path)
   ))


(rf/reg-sub-raw
 :data
 (fn [db [_ type query]]
   (let  [path (cdb/path-for-type type)
          query-token (cdb/get-data
                       type
                       query
                       :on-success #(rf/dispatch [:write-to path %]))]
     (ra/make-reaction
      (fn [] (get-in @db path []))
      :on-dispose #(do ;(terminate-items-query! query-token)
                        (rf/dispatch [:cleanup path]))))))


(rf/reg-sub
 :results
 (fn [db [_ uuid field]]
   (get-in db (flatten [:results uuid field]))))

(rf/reg-sub
 :result-status
 (fn [db [_ uuid]]
   (log/debug ::result-status (map :status (map :status (vals (get-in db [:results uuid])))))
   (and
    (not-every? nil? (map :status (map :status (vals (get-in db [:results uuid])))))
    (every? #{"success"} (map :status (map :status (vals (get-in db [:results uuid]))))))
   )
 )
