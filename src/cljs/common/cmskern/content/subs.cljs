(ns cmskern.content.subs
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :as rf]
            [reagent.ratom :as ra]
            [cognitect.transit :as t]

            [cmskern.db :as cdb]
            ))


(rf/reg-sub
 :current-content-data
 (fn [db [_]]
   (or
    (:data (:current-content db))
    {})
   ))

(rf/reg-sub
 :current-content
 (fn [db [_]]
   (:current-content db)
   ))

(rf/reg-sub
 :original-content
 (fn [db [_]]
   (:original-content db)
   ))

(rf/reg-sub
 :content/changed?
 :<- [:content/changes]
 (fn [[changes] _]
     (not (and (nil? (first changes)) (nil? (second changes))))
   ))

(rf/reg-sub
 :content/changes
 :<- [:current-content]
 :<- [:original-content]
 (fn [[current-content original-content] _]
   (let [d (clojure.data/diff (:data current-content) (:data original-content))]
     d
     )
   ))

(rf/reg-sub
 :content/ui-schema
 :<- [:current-cts]
 :<- [:current-ctid]
 (fn [[current-cts current-ctid] _]
   (let [r (t/reader :json)
         ct (some #(and (= current-ctid (:name %)) %) current-cts)
         s (:uiSchema ct)
         res (t/read r s)
         ]
     res
     )))
(rf/reg-sub
 :content/json-schema
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
