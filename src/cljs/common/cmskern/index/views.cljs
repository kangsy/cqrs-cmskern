(ns cmskern.index.views
  (:require-macros [cmskern.ui.core :refer [handler-fn]])
  (:require [re-frame.core :as rf]
            [reagent.core :as r]
            [taoensso.timbre :as log]

            [cmskern.pages :as p]
            [cmskern.views.misc :as misc]
            [cmskern.routes :as route]
            [cmskern.functions :as f :refer [tr]]

            [cmskern.index.events]
            [cmskern.views.widgets :as w]
            ))



(defn sidebar
  ""
  []
  (let [uuid (random-uuid)
        status (rf/subscribe [:results uuid :status])
        dbid (rf/subscribe [:current-db])
        cts (rf/subscribe [:current-cts])]
    (fn []
      [:div.sidebar
       (if @dbid
         [:ul.cts
          (doall
           (map-indexed
            (fn [idx ct] ^{:key idx}
              [:li [:a {:href (route/make-path :ct-index-page :dbid @dbid :ctid (:name ct))} (:displayName ct)]]
              ) @cts))]
        misc/spinner )]
      )))

(defn status->label
  [st]
  (let [l (case st
          20 ["danger" :deleted]
          50 ["default" :draft]
          100 ["info" :un-published]
          200 ["success" :live]
          :unkown
          )]
    [:span.label {:class (str "label-" (first l))} (second l)]))

(defn contents-table
  [contents dbid ctid]
  [:table.table.table-hover.table-striped.table-condensed.contents
   [:thead
    [:tr
     [:th (tr ["Id #"])]
     [:th (tr ["Status"])]
     [:th (tr ["Titel"])]
     [:th (tr ["Modifier / Creator"])]
     [:th (tr ["Zuletzt verändert"])]
     ]]
   [:tbody
    (doall
     (map-indexed
      (fn [idx cont]
        (.log js/console ::ct-content-list idx cont)
        (let [link (route/make-path :content-edit :dbid dbid :ctid ctid :cid (:_id cont))]
          ^{:key idx}
          [:tr
           [:td nil
            [:a {:href link}
             (:_id cont)]]
           [:td nil [:a {:href link} (status->label (:_status cont))]]
           [:td nil
            [:a {:href link}
             (-> cont :data :title)]
            ]
           [:td nil (:_modifier cont) " / " (:_creator cont)]
           [:td nil (when (:_modified cont)
                      (str (f/epoch->str (:_modified cont) "dd.MM.y - HH:mm:ss") " Uhr"))]
           ])
        ) contents))]])
(defn ct-content-list
  ""
  []
  (let [
        status (rf/subscribe [:results :global])
        contents (rf/subscribe [:contents])
        dbid (rf/subscribe [:current-db])
        ctid (rf/subscribe [:current-ctid])
        ct (rf/subscribe [:current-ct])
        new-content (fn [ct]
                      (handler-fn (.preventDefault event)
                                  (rf/dispatch [:new-content ct])))]
    (fn []
      [:div.content-list
       [:h4 (tr ["Contents "]) (:displayName @ct) ]
       [:div.actions
        [:a {:href (route/make-path :content-new :ctid @ctid :dbid @dbid)} (tr ["Neu erstellen"])]]
       [misc/status-panel]
       [contents-table @contents @dbid @ctid]
       ]
      )))

(defn db-content-list
  ""
  []
  (let [
        contents (rf/subscribe [:contents])
        dbid (rf/subscribe [:current-db])
        cts (rf/subscribe [:current-cts])
        new-content (fn [ct]
                      (handler-fn (.preventDefault event)
                                  (rf/dispatch [:new-content ct])))]

    (fn []
      [:div.content-list
       [misc/status-panel]
       [:div.contents
        (doall
         (map
          (fn [[ctid conts]]
            (let [ct (some #(and (= (:name %) ctid) %) @cts)]
              ^{:key ctid}
              [:div.panel.panel-default
               [:div.panel-heading
                [:h4 nil (or (:displayName ct) ctid) " " [:a {:href (route/make-path :content-new :dbid @dbid :ctid ctid)} [:small nil "new"]]]]
               [contents-table conts @dbid ctid]
               ])
            ) @contents))]]
      )))

(defmethod p/page :ct-index-page
  [r]
  [:div.container-fluid
   [p/header]
   [:div.db-index
    [:div.row
     [:div.col-md-2
      [sidebar]
      ]
     [:div.col-md-10
      [:div.main
       nil
       [ct-content-list]]]]
    ]
   ]
  )
(defmethod p/page :db-index-page
  [r]
  [:div.container-fluid
   [p/header]
   [:div.db-index
    [:div.row
     [:div.col-md-2
      [sidebar]
      ]
     [:div.col-md-10
      [:div.main
       nil
       [db-content-list]]]]
    ]
   ]
  )

(defn list-user-dbs
  ""
  []
  (let [dbs (rf/subscribe [:user-dbs])]
    (fn []
      (when (not-empty @dbs)
        [:ul
         (doall
          (map-indexed (fn [idx db]
                         ^{:key idx} [:li [:a {:href (route/make-path :db-index-page :dbid (:dbid db))} (:name db)]]) @dbs))
         ]))))

(defmethod p/page :index-page
  [r]
  (let []
    [:div.container-fluid
     [p/header]
     [:div.index
      [:div.row
       [:div.col-md-2
        ]
       [:div.col-md-10
        [:div.main
         nil
         [list-user-dbs]]]]
      ]]))
