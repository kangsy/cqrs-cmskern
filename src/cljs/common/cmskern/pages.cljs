(ns cmskern.pages
  (:require [re-frame.core :as rf]
            [reagent.core :as r]
            [taoensso.timbre :as log]

            [cmskern.functions :as f :refer [tr]]
            [cmskern.routes :as route]
            )
  (:require-macros [cmskern.ui.core :refer [handler-fn]]))

(defmulti page :handler)
(defn render-page
  [r]
  (page r)
  )

(defn lost-connection []
  [:div.modal-open
   [:div.modal.fade.in {:style {:display :block} :tabIndex "-1", :role "dialog", :aria-labelledby "myModalLabel", :aria-hidden "true"}
    [:div.modal-backdrop.fade.in {:style {:z-index 0}}]
    [:div.modal-dialog
     [:div.modal-content
      [:div.modal-header
       [:button {:type "button", :class "close", :data-dismiss "modal", :aria-hidden "true"} "×"]
       [:h3 "Websocket-Verbindung verloren"]]
      [:div {:class "modal-body"} "Bitte die Seite neu laden."]]]]]
  )

(defn admin-header []
  (let [dbs (rf/subscribe [:db [:databases]])
        dropdown-open? (r/atom false)
        user (rf/subscribe [:db [:user]])
        lost-conn? (rf/subscribe [:db [:connection-lost?]])
        ]
    (fn []
      [:div.header.admin
       (when @lost-conn?
         [lost-connection]
         )
       [:nav.navbar.navbar-inverse
        [:div.container-fluid
         [:div.navbar-header
          [:button {:type "button", :class "navbar-toggle collapsed", :data-toggle "collapse", :data-target "#bs-example-navbar-collapse-1", :aria-expanded "false"}
           [:span {:class "sr-only"} "Toggle navigation"]
           [:span {:class "icon-bar"}]
           [:span {:class "icon-bar"}]
           [:span {:class "icon-bar"}]]
          [:a {:class "navbar-brand", :href "/"} "CMSKern"]]
         [:div {:class "collapse navbar-collapse", :id "bs-example-navbar-collapse-1"}
          [:ul {:class "nav navbar-nav"}
           [:li [:a {:href (route/make-path :admin-users-page)} (tr ["Users"])]]
           [:li [:a {:href (route/make-path :admin-dbs-page)} (tr ["Databases"])]]
           ]
          ]
         ]]
       ]
      )))

(defn header []
  (let [dbs (rf/subscribe [:user-dbs])
        dbid (rf/subscribe [:current-db])
        current-ct (rf/subscribe [:current-ct])
        dropdown-open? (r/atom false)
        login-dropdown? (r/atom false)
        user (rf/subscribe [:db [:user]])
        lost-conn? (rf/subscribe [:db [:connection-lost?]])
        logout (handler-fn (.preventDefault event) (rf/dispatch [:login/logout]))
        ]
    (fn []
      [:div.header
       (when @lost-conn?
         [lost-connection]
         )
       [:nav.navbar.navbaar-default
        [:div.container-fluid
         [:div.navbar-header
          [:button {:type "button", :class "navbar-toggle collapsed", :data-toggle "collapse", :data-target "#bs-example-navbar-collapse-1", :aria-expanded "false"}
           [:span {:class "sr-only"} "Toggle navigation"]
           [:span {:class "icon-bar"}]
           [:span {:class "icon-bar"}]
           [:span {:class "icon-bar"}]]
          [:a {:class "navbar-brand", :href "/"} "CMSKern"]]

         [:div {:class "collapse navbar-collapse", :id "bs-example-navbar-collapse-1"}
          (when @dbid
            [:ul {:class "nav navbar-nav"}
             [:li.dropdown {:class (when @dropdown-open? "open")}
              [:a {:on-click (handler-fn (.preventDefault event) (swap! dropdown-open? not)) :class "dropdown-toggle", :data-toggle "dropdown", :role "button", :aria-haspopup "true"} "Databases " 
               [:span {:class "caret"}]]
              (when (not-empty @dbs)
                [:ul.dropdown-menu
                 (doall
                  (map-indexed (fn [idx db]
                                 ^{:key idx} [:li [:a {:href (route/make-path :db-index-page :dbid (:dbid db)) :class (when (and @dbid (= @dbid (:dbid db))) "active")} (:name db)]]) @dbs))
                 ])
              ]
             [:li 
              [:a {:href (route/make-path :db-index-page :dbid @dbid)} @dbid
               [:span {:class "sr-only"} "(current)"]]]
             (when @current-ct
               [:li 
                [:a {:href (route/make-path :ct-index-page :dbid @dbid :ctid (:name @current-ct))} (:displayName @current-ct)
                 [:span {:class "sr-only"} "(current)"]]]
               )
             ])
          [:ul.nav.navbar-nav.navbar-right
           (when f/admin? @user
                 [:li 
                  [:a {:href (route/make-path :admin-page)} "AdminArea" 
                   [:span {:class "sr-only"} "(current)"]]])
           [:li.dropdown {:class (when @login-dropdown? "open")}
            [:a {:on-click (handler-fn (.preventDefault event) (swap! login-dropdown? not)) :class "dropdown-toggle", :data-toggle "dropdown", :role "button", :aria-haspopup "true"} (:username @user) [:span {:class "caret"}]]
            [:ul.dropdown-menu
             [:li [:a {:on-click logout :href "#"} (tr ["Logout"])]]
             ]
            ]]]
         ]]
       ]
      )
    ))
