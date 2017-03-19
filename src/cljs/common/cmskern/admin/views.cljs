(ns cmskern.admin.views
  (:require-macros [cmskern.ui.core :refer [handler-fn]])
  (:require [re-frame.core :as rf]
            [clojure.string :as string]
            [taoensso.timbre :as log]
            [reagent.core :as r]

            [cmskern.pages :as p]
            [cmskern.admin.events]
            [cmskern.routes :as route]
            [cmskern.functions :as f :refer [tr]]

            [cmskern.views.inputs :as vi]
            [cmskern.views.misc :as misc]
            [cmskern.views.widgets :as w]))


(defn sidebar []
  [:div.sidebar
   [:ul
    [:li [:a {:href (route/make-path :admin-users-page)} "Users"]]
    [:li [:a {:href (route/make-path :admin-dbs-page)} "Databases"]]
    ]])
(defn admin-index
  []
  (let []
    [:div.index nil "admin index"]
    ))

(defn admin-users
  []
  (let [users (rf/subscribe [:db [:users]])]
    (fn []
      [:div.users nil
       [:h4 (tr ["Admin Users"])]

       [:div nil [:a {:href (route/make-path :admin-user-new)} [:span.btn.btn-primary  (tr ["New User"])]]]
       [:ul.list
        (doall (map-indexed
                (fn [idx ent]
                  (when (:_id ent)
                    ^{:key idx}
                    [:li nil [:a {:href (route/make-path :admin-user-edit :uid (.-rep (:_id ent)))} (:email ent)  ]])) @users))
        ]])))

(defn admin-dbs
  []
  (let [dbs (rf/subscribe [:db [:databases]])]
    (fn []
      [:div.dbs nil
       [:h4 (tr ["admin databases"])]
       [:div nil [:a {:href (route/make-path :admin-db-new)} [:span.btn  (tr ["New Database"])]]]

       [:ul.db-list
        (doall (map-indexed (fn [idx ent] ^{:key idx}
                              [:li nil [:a {:href (route/make-path :admin-db-edit :dbid (:dbid ent))} (:name ent)]]) @dbs))
        ]
       ])))

;; ==== pages ====
(defmethod p/page :admin-page
  []
  [:div.container-fluid
   [p/admin-header]
  [:div.admin
   [:div.row
    [:div.col-md-2
     [sidebar]]
    [:div.col-md-10
     [admin-index]]]]])

(defmethod p/page :admin-users-page
  []
  [:div.container-fluid
   [p/admin-header]
  [:div.admin
   [:div.row
    [:div.col-md-2
     [sidebar]]
    [:div.col-md-10
     [admin-users]]]]])

(defn admin-user-edit
  [{{:keys [uid]} :route-params}]
  (let [on-submit (fn [arg]
                    (.log js/console ::on-submit arg)
                    (rf/dispatch [:admin-submit-user-form arg]))
        schema-data (rf/subscribe [:db [:admin-schema]])
        form-data (rf/subscribe [:db [:admin-data]])
        dbs (rf/subscribe [:db [:databases]])
        fields {}
        ]
    (fn []
      (let [formdata (or @form-data {})]
        [:div.dbs.well
         [:legend (str " Einstellungen")]
         (if @schema-data
           [w/json-form {:schema (clj->js (:schema @schema-data))
                         :onSubmit on-submit
                         :uiSchema (clj->js (:ui-schema @schema-data))
                         :formData (clj->js formdata)
                         }]
           [:div.waiting "waiting"])
         ]))))

(defn user-actions
  [{{:keys [uid]} :route-params}]
  (let [change-pw? (r/atom false)
        data (r/atom {:password nil :password-confirm nil})
        submit (handler-fn (.preventDefault event)
                           (rf/dispatch [:admin/submit-change-pw uid @data])
                           (reset! change-pw? false))]
    (fn 
      [{{:keys [uid]} :route-params}]
      [:div.action-bar.well
       [:h5 nil (tr ["Actions"])]
       [:ul.cts
        [:li.action
         (if @change-pw?
           [:form {:class "pure-form pure-form-stacked" :on-submit submit}
         [:fieldset  
        [vi/input-control
         :model            (:password @data)
         :input-type       :text
         :status           nil
         :status-icon?     true
         :width            "300px"
         :placeholder      "Passwort"
         :label      "Password"
         :on-change        #(swap! data assoc :password %)
         :change-on-blur?  false
         :disabled?        false]
        [vi/input-control
         :model            (:password-confirm @data)
         :input-type       :text
         :status           nil
         :status-icon?     true
         :width            "300px"
         :placeholder      ""
         :label      "Passwort Wiederholung"
         :on-change        #(swap! data assoc :password-confirm %)
         :change-on-blur?  false
         :disabled?        false]

        [:div {:class "pure-controls"}
         [:button.pure-button.pure-button-primary {:type :submit} "Submit"]
         [:button.pure-button.pure-button-secondary {:on-click (fn [e] (.preventDefault e) (reset! change-pw? false))} "Cancel"]]
        ]]
           
           [:a {:href "#" :on-click (handler-fn (.stopPropagation event)
                                                (.preventDefault event)
                                                (swap! change-pw? not))} "Change Password"])
         ]
        ]
       ])
    ))

(defn action-bar
  [{{:keys [dbid]} :route-params}]
  (let [cts (rf/subscribe [:db [:admin-cts]])
        ]
    (fn 
      [{{:keys [dbid]} :route-params}]
      [:div.action-bar.well
       [:h5 nil "Content-Types"]
       [:ul.cts
        (doall
         (map-indexed (fn  [idx ct] ^{:key idx}
                        [:li nil [:a {:href (route/make-path :admin-ct-edit :dbid dbid :ctid (:name ct))} (or (:displayName ct) (:name ct))]])
                      @cts))
        (when dbid
          [:li.action
           [:a {:href (route/make-path :admin-ct-new :dbid dbid)} "Add new Content-Type"]
           ])
        ]
       ])
    ))

(defmethod p/page :admin-user-edit-page
  [r]
  [:div.container-fluid
   [p/admin-header]
   [:div
    [:div.row
     [:div.col-md-6
      [admin-user-edit r]]
     [:div.col-md-6
      [user-actions r]]]]]
  )

(defmethod p/page :admin-dbs-page
  []
  [:div.container-fluid
   [p/admin-header]
   [:div.admin
    [:div.row
     [:div.col-md-2
      [sidebar]]
     [:div.col-md-10
      [admin-dbs]]]]]
  )

(defn admin-db-edit
  [{{:keys [dbid]} :route-params}]
  (let [on-submit (fn [arg]
                    (.log js/console ::on-submit arg)
                    (rf/dispatch [:admin-submit-db-form arg]))
        schema-data (rf/subscribe [:db [:admin-schema]])
        form-data (rf/subscribe [:db [:admin-data]])
        dbs (rf/subscribe [:db [:databases]])
        fields {}
        ]
    (fn []
      (let [formdata (or @form-data {})]
        [:div.dbs.well
         [:legend (str dbid " Einstellungen")]
         (if @schema-data
           [w/json-form {:schema (clj->js (:schema @schema-data))
                         :onSubmit on-submit
                         :uiSchema (clj->js (:ui-schema @schema-data))
                         :formData (clj->js formdata)
                         }]
           [:div.waiting "waiting"])
         ]))))

(defn validate-mocker
  [formData errors]
  (let [formdata (js->clj formData)]
    (rf/dispatch [:db/add [:preview-data] (js->clj formdata :keywordize-keys true)]))
  errors
  )
(defn admin-ct-edit
  [{handler :handler {:keys [dbid ctid]} :route-params}]
  (let [on-submit (fn [arg]
                       (.log js/console ::on-submit arg)
                       (rf/dispatch [:admin-submit-ct-form dbid arg]))
        schema-data (rf/subscribe [:db [:admin-schema]])
        form-data (rf/subscribe [:db [:admin-data]])
        ct (rf/subscribe [:db [:current-ct]])
        live-validate? (r/atom false)
        ;dbid (rf/subscribe [:current-db])
        fields {}
        ]
    (fn
      [{handler :handler {:keys [dbid ctid]} :route-params}]
      (let [formdata (or @form-data {:jsonSchema ""
                                     :uiSchema ""})]
        [:div.dbs.well
         [:legend (if ctid "Content-Type bearbeiten " "Neuer Content-Type in DB ") dbid]

         (if @schema-data
           [w/json-form {:schema (clj->js (:schema @schema-data))
                                      :onSubmit on-submit
                                      :uiSchema (clj->js (:ui-schema @schema-data))
                                      :formData (clj->js formdata)
                                      :liveValidate true
                                      :validate validate-mocker
                                      }]
           misc/spinner)
         ]))))



(defn admin-ct-preview
  [{handler :handler {:keys [dbid ctid]} :route-params}]
  (let [preview-data (rf/subscribe [:db [:preview-data]])
        field {"select-content-ref"
               (w/content-callout {:dbid "test" ;; todo
                                   :col "content"
                                   :q {}
                                   :preview (fn [v] [:span {:style {:background (get-in v [:data :value])}} "X"])})
               }
        ]
    (fn
      [{handler :handler {:keys [dbid ctid]} :route-params}]
      (let [schema  (.parse js/JSON (or (not-empty (string/trim (or (get @preview-data "jsonSchema") ""))) "{}"))
            ui-schema (.parse js/JSON (or (not-empty (string/trim (or (get @preview-data "uiSchema") ""))) "{}"))
            ]
        [:div.preview
         [:legend nil "Preview"]
         [w/json-form {:schema schema
                       :id :preview
                       :uiSchema ui-schema
                       :fields field
                                    }]
         ])

      )))
(defmethod p/page :admin-ct-edit-page
  [r]
  [:div.container-fluid
   [p/admin-header]
   [:div.admin
    [:div.row
     [:div.col-md-6
      [admin-ct-edit r]]
     [:div.col-md-6
      [admin-ct-preview r]]]]]
  )

(defmethod p/page :admin-db-edit-page
  [r]
  [:div.container-fluid
   [p/admin-header]
   [:div
    [:div.row
     [:div.col-md-6
      [admin-db-edit r]]
     [:div.col-md-6
      [action-bar r]]]]]
  )
