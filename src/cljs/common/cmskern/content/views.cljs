(ns cmskern.content.views
(:require-macros [cmskern.ui.core :refer [handler-fn]])
(:require [re-frame.core :as rf]
          [clojure.string :as string]
          [taoensso.timbre :as log]
          [reagent.core :as r]

          [cmskern.content.events]
          [cmskern.content.subs]

          [cmskern.pages :as p]
          [cmskern.routes :as route]
          [cmskern.functions :as f :refer [tr]]

          [cmskern.views.inputs :as vi]
          [cmskern.views.misc :as misc]

          [cmskern.views.widgets :as w]
          ))


(defn content-action-bar
  [{{:keys [cid ctid dbid]} :route-params} uuid]
  (let [
        form-data (rf/subscribe [:current-content])
        temp-data (rf/subscribe [:current-content-copy])
        modal-open? (r/atom false)
        modal-content (r/atom nil)
        on-close (handler-fn (.preventDefault event) (reset! modal-open? false))
        update-modal-content (fn [res] (reset! modal-content res))
        changes (rf/subscribe [:content/changes])
        open-modal (fn [v]
                     (handler-fn (.preventDefault event)
                                 (.stopPropagation event)
                                 (reset! modal-content nil)
                                 (reset! modal-open? true)
                                 (rf/dispatch [:fetch-version dbid v update-modal-content])
                                 ))
        on-incure (handler-fn (.preventDefault event)
                              (log/debug ::on-incure (:data @modal-content))
                              (rf/dispatch [:incure-data (:data @modal-content)]))
        publish (handler-fn (.preventDefault event)
                            (rf/dispatch [:content/publish dbid ctid cid]))
        delete (handler-fn (.preventDefault event)
                            (rf/dispatch [:content/delete dbid ctid cid]))
        un-publish (handler-fn (.preventDefault event)
                           (rf/dispatch [:content/un-publish dbid ctid cid]))
        ]
    (fn
      [{{:keys [cid ctid dbid]} :route-params}]
      [:div.content-action-bar
       [misc/status-panel]
       [:ul.list-group
        [:li.list-group-item (tr ["Erstellt am "]) (when (:_created @form-data) (f/epoch->str (:_created @form-data) "HH:mm:ss - dd.MM.y"))]
        [:li.list-group-item (tr ["von "]) (:_creator @form-data)]
        [:li.list-group-item (tr ["zuletzt geändert am "]) (when (:_modified @form-data) (f/epoch->str (:_modified @form-data) "HH:mm:ss - dd.MM.y"))]
        [:li.list-group-item (tr ["von "]) (:_modifier @form-data)]
        [:li.list-group-item nil [:h5 (tr ["Publication-Status "])
                                  (if (> (:_status @form-data) 50)
                                    [:span.label.label-default (tr ["Live"])]
                                    [:span.label.label-info (tr ["Draft"]) ]
                                    )]]
        ]
       [:div.actions.btn-toolbar
        [:a.btn.btn-primary {:on-click publish :class (when (or (nil? (:_status @form-data)) (>= (:_status @form-data) 200)) "disabled")} (tr ["publish"])]
        [:a.btn.btn-warning {:on-click un-publish :class (when (< (:_status @form-data) 200) "disabled")} (tr ["un-publish"])]
        [:a.btn.btn-danger {:on-click delete :class (when (nil? (:_status @form-data)) "disabled")} (tr ["delete"])]
        ]
       (when @modal-open?
         [misc/modal-box [:legend nil (tr ["Version"])]
          (if @modal-content
            [:div [:pre (with-out-str (cljs.pprint/pprint (:data @modal-content)))]
             [:a.btn.btn-primary {:on-click on-incure} (tr ["Version übernehmen"])]
             ]
            misc/spinner)
          :on-close on-close
          ])
       [:h4 (tr ["History"])]
       [:ul.history
        (map-indexed
         (fn [idx v]
           ^{:key idx} [:li [:div.ts (when (:ts v) (f/epoch->str (:ts v) "HH:mm:ss - dd.MM.y"))]
                        [:div nil (:subject v) " - " (:by v)]
                        (when (-> v :data :version)
                          [:a {:on-click (open-modal (-> v :data :version)) } (tr [(str "Version " (-> v :data :version))])]

                          )
                        ]
           ) (reverse (:history @form-data)))

        ]
       [:h4 (tr ["Live Changes"])]
       [:div [:pre (with-out-str (cljs.pprint/pprint @changes))]]
       [:div [:pre (with-out-str (cljs.pprint/pprint (:data @form-data)))]]
       ])))

(defn on-validate-incure
  "Diese fn nutzt das live-validate-Feature um die Form-Daten der JsonSchema-React-Compo
  in ein Clojure-Atom zu schreiben."
  [formData errors]
  (let [formdata (js->clj formData :keywordize-keys true)]
    (rf/dispatch [:db/add [:current-content :data] formdata])
    ;; wenn mit nil initialisiert, sorgt dies dafür dass die struktur entsteht.
    ;; sorge dafür, dass dies nicht als "change" erkannt wird
    ;; todo - es sollte auch keine fehler angezeigt werden.
    ;
     (.log js/console ::formdata formdata (not-empty (w/remove-nils formdata))   errors)

    (when-not (not-empty (w/remove-nils formdata))
      (rf/dispatch [:db/add [:current-content-copy :data] formdata])
      )
    )
  errors
  )

(defn content-edit
  [{{:keys [cid ctid dbid]} :route-params} uuid]
  (let [on-submit (fn [arg]
                    (.log js/console ::on-submit arg)
                    (rf/dispatch [:submit-content dbid ctid cid arg uuid]))
        json-schema (rf/subscribe [:content/json-schema])
        ui-schema (rf/subscribe [:content/ui-schema])
        content-type (rf/subscribe [:current-ct])
        content-data (rf/subscribe [:current-content-data])
        dbs (rf/subscribe [:db [:databases]])
        content-changed? (rf/subscribe [:content/changed?])
        fields {}
        ]
    (fn []
      (let []
        [:div.content-edit.well
         [:legend (str (:displayName @content-type) " editieren ")
          [:small (if @content-changed?
                    [:span.label.label-warning (tr ["Changed"]) ]
                    [:span.label.label-default (tr ["Un-Changed"])]
                    )]]
         (if (and @json-schema @ui-schema)
           [w/json-form {:schema @json-schema
                         :onSubmit on-submit
                         :uiSchema @ui-schema
                         :formData @content-data
                         :liveValidate true
                         :validate on-validate-incure
                         }]
           misc/spinner)
         ]
        ))))

(defmethod p/page :content-edit-page
  [r]
  (let [
        uuid (random-uuid)
        ]
    [:div.container-fluid
     [p/header]
     [:div
      [:div.row
       [:div.col-md-8
        [content-edit r uuid]]
       [:div.col-md-4
        [content-action-bar r uuid]]]]])
  )
