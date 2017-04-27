(ns cmskern.views.widgets
  (:require-macros [cmskern.ui.core :refer [handler-fn]]
                   [taoensso.timbre :as log])
  (:require [re-frame.core :as rf]
            [reagent.core :as r]
            [cognitect.transit :as t]
            [cljs-time.format :as timef]
            [day8.re-frame.http-fx] 

            [cmskern.views.pager :as pager]
            [cmskern.views.misc :as misc]
            [cmskern.views.file :as vf]
            [cmskern.functions :as f :refer [tr]]
            [cmskern.events]
            [cmskern.subs]
            ))

(def  react-jsonschema-form (aget js/window "deps" "react-jsonschema-form" "default"))
(def  dropzone (aget js/window "deps" "react-dropzone"))
(def  schema-field (aget js/window "deps" "schema-field" "default"))
(def  schema-utils (aget js/window "deps" "schema-utils"))

(defn select-binary-ref
  ""
  [{:keys [dbid q] :as params}
   & {:keys [col-labels cols cols on-select show-fns preview limit on-delete]
      :or {col-labels ["Datei" "Datum" "Größe" "Ersteller"]
           cols [[:filename] [:uploadDate] [:length] [:metadata :uploadUser]]
           show-fns {:uploadDate (fn [v] (str (f/epoch->str (.getTime v) "dd.MM.y - HH:mm:ss") " Uhr"))}
           limit 20
           preview (fn [val] (misc/preview-fn dbid (:contentType val) val))
           on-select (fn [cont]
                       (log/debug ::select-content-ref :on-select cont))
           on-delete (fn [asset cb] (rf/dispatch [:gfs-delete nil {:db dbid :id (:_id asset)} cb]))
           }}]
  (log/debug ::select-content-ref :init params)
  (let [uuid (random-uuid)
        txid (random-uuid)
        page (r/atom 1)
        mk-query (fn [p]
                   {:find q
                    :limit limit
                    :skip (* limit (- p 1))
                    :sort (array-map :uploadDate -1)
                    })
        query-fn (fn []
                   (rf/dispatch
                    [:gfs-query-callout
                     uuid
                     {:db dbid :query (mk-query @page)}]))
        on-page-change (fn [p] (reset! page p)
                         (query-fn))
        results (rf/subscribe [:db [:results uuid]])
        selected-content (r/atom nil)
        on-incure (handler-fn (.stopPropagation event)
                             (.preventDefault event)
                             (on-select @selected-content))

        on-delete-click (handler-fn (.stopPropagation event)
                                    (.preventDefault event)
                                    (on-delete @selected-content
                                               (fn [res] (query-fn))))

        select-content (fn [cont] (handler-fn
                                   (if (= @selected-content cont)
                                     (reset! selected-content nil)
                                     (reset! selected-content cont))))
        show (fn [f v]
               (let [val (get-in v f)]
                 ((or (get-in show-fns f) str) (f/get-value-str val))))
        upload-new-file (handler-fn (rf/dispatch [:gfs-upload-file dbid (-> event .-target .-files (aget 0)) txid]))
        _ (query-fn)
        ]
    (fn
      [{:keys [dbid q] :as params}]
      (log/debug ::select-binary-ref :render params)
      [:div nil
       [vf/file-upload :dbid dbid :on-done #(query-fn)]
       [pager/pagination {:start @page :limit limit :total (:count @results) :on-change #(on-page-change %)}]
       [:div.results
        [:table.table.table-condensed.table-striped.table-hover
         [:thead
          [:tr
           (doall
            (map-indexed
             (fn [idx col] ^{:key idx}
               [:th nil col]) col-labels))
           (when preview [:th (tr ["Preview"])])
           [:th (tr ["Action"])]
           ]]
         [:tbody
          (if-not (:result @results)
            [:tr
             [:td {:col-span (+ 2 (count cols))} misc/spinner]]
            (doall
             (map-indexed
              (fn [idx content] ^{:key idx}
                [:tr {:on-click (select-content content)}
                 (doall
                  (map-indexed
                   (fn [cidx field] ^{:key cidx}
                     [:td nil [:div nil [:div (show field content )]
                               (when (and (= cidx 0) (= @selected-content content))
                                 [:button.btn.btn-xs.btn-danger {
                                                                 :on-click on-delete-click} (tr ["Delete"])]
                                 )
                               ]
                      ]) cols))
                 (when preview [:td.preview (preview content)])
                 [:td nil
                  [:button.btn.btn-xs {:class (if (= @selected-content content) "btn-primary" "btn-default")
                                       :disabled (when-not (= @selected-content content) :disabled)
                                       :on-click on-incure} (tr ["Übernehmen"])] [:br]
                  ]
                 ]
                ) (:result @results)))
            )]]]])))

(defn select-content-ref
  ""
  [{:keys [dbid col q] :as params} & {:keys [col-labels cols cols on-select show-fns preview limit]
                                      :or {col-labels [:_id :title]
                                           cols [[:_id] [:data :title]]
                                           show-fns {}
                                           limit 30
                                           on-select (fn [cont]
                                                       (log/debug ::select-content-ref :on-select cont))}}]
  (log/debug ::select-content-ref :init params)
  (let [uuid (random-uuid)
        page (r/atom 1)
        selected-content (r/atom nil)
        results (rf/subscribe [:db [:results uuid]])
        mk-query (fn [p]
                   {:find q
                    :limit limit
                    :skip (* limit (- p 1))
                    :fields []
                    :sort (array-map :_modified -1)
                    })
        query-fn (fn []
                   (rf/dispatch
                    [:db-query-callout
                     uuid
                     {:db dbid :col col :query (mk-query @page)}]))
        on-page-change (fn [p] (reset! page p)
                         (query-fn))
        on-incure (handler-fn (.stopPropagation event)
                              (.preventDefault event)
                              (on-select @selected-content)
                              )
        select-content (fn [cont] (handler-fn
                                   (if (= @selected-content cont)
                                     (reset! selected-content nil)
                                     (reset! selected-content cont))))
        show (fn [f v] ((or (get-in show-fns f) str) (get-in v f)))
        _ (query-fn)
        ]
    (fn
      [{:keys [dbid col q] :as params}]
      (log/debug ::select-content-ref :render params)
      [:div nil
       [pager/pagination {:start @page :total (:count @results) :limit limit :on-change #(on-page-change %)}]
       [:div.results
        [:table.table.table-condensed.table-striped.table-hover
         [:thead
          [:tr
           (doall
            (map-indexed
             (fn [idx col] ^{:key idx}
               [:th nil col]) col-labels))
           (when preview [:th (tr ["Preview"])])
           [:th (tr ["Action"])]
           ]]
         [:tbody
          (if-not (:result @results)
            [:tr
             [:td {:col-span (+ 2 (count cols))} misc/spinner]]
            (doall
             (map-indexed
              (fn [idx content] ^{:key idx}
                [:tr {:on-click (select-content content)}
                 (doall
                  (map-indexed
                   (fn [cidx field] ^{:key cidx}
                     [:td nil [:div nil [:div (show field content )]
                               ]
                      ]) cols))
                 (when preview [:td.preview (preview content)])
                 [:td nil
                  [:button.btn.btn-xs {:class (if (= @selected-content content) "btn-primary" "btn-default")
                                       :disabled (when-not (= @selected-content content) :disabled)
                                       :on-click on-incure} (tr ["Übernehmen"])] ]
                 ]
                ) (:result @results)))
            )]]]])))

(defn field-mapper
  ""
  [cont]
  {:idref (f/get-value-str (:_id cont))
   :title (-> cont :data :title)
   :catchline (-> cont :data :catchline)
   :teaser (get-in cont [:data :teaser :text])
   :imgref (or (get-in cont [:data :teaser :image_idref]) (:image_idref (first (get-in cont [:data :image]))))
   })

(defn callout
  "Callout-Component-Factory-Function"
  [elem {:keys [dbid] :as params}]
  (fn
    [{:keys [formData schema uiSchema formContext registry name] :as args}]
    (let [data (r/atom (js->clj formData :keywordize-keys true))
          modal-open? (r/atom false)
          self (r/current-component)
          sync-data (fn [d]
                      (-> self .-props (.onChange (clj->js d)))
                      )
          on-select (fn [v]
                      (reset! data (-> v
                                       field-mapper
                                       (select-keys (keys (:properties (js->clj schema :keywordize-keys true))))))
                      (sync-data @data)
                      )
          on-click (handler-fn
                    (.preventDefault event)
                    (.stopPropagation event)
                    (swap! modal-open? not)
                    )
          ]
      (fn
        [{:keys [formData schema uiSchema formContext registry name] :as args}]
        (let [ui-schema (js->clj uiSchema :keywordize-keys true)
              ui-params (get ui-schema :callout/params) 
              query (:query ui-params)
              preview-type (:preview ui-params)]
              ;;(.log js/console ::callout args @data ui-params)

          (when schema
            [:fieldset
             [:legend (or (aget schema "title") name)]
             [:div.preview (misc/preview-fn (:dbid query) preview-type @data)]
             [:span.input-group-btn
              [:button.btn.btn-primary.pull-right {:type "button" :on-click on-click} (tr ["Callout"])]
              ]
             (when @modal-open?
               [misc/modal-box [:legend nil (tr ["Callout"])]
                [elem (merge params query {:preview misc/preview-fn}) :on-select on-select]
                :on-close on-click])
             (doall (map (fn [[k v]]
                           (let [vtype (get v "type")
                                 inp (keyword (or (get-in ui-schema (map keyword [k "ui:widget"])) "input"))
                                        ;_ (log/debug ::input k inp (get ui-schema (keyword k)) (get @data (keyword k)) (get v "type"))
                                 ]
                             ^{:key k} [:div.form-group nil
                                        [:label.control-label (or (get v "title") k)]
                                        [inp {:class "form-control"
                                              :type (if (= vtype "integer") "number" "text")
                                              :value (if (nil? (get @data (keyword k))) "" (get @data (keyword k)))
                                              :on-change (handler-fn
                                                          (swap! data assoc (keyword k) (if (= vtype "integer") (js/parseInt (-> event .-target .-value)) (-> event .-target .-value))
                                                                 )
                                                          (sync-data @data)
                                                          )}
                                         ]
                                        ])
                           ) (js->clj (aget schema "properties"))))
             ]))))))

(defn resolve-schema
  ""
  [schemas form-data]
  ;; schemas cljs
  ;; form-data cljs
  ;; must return js
  ;(log/debug ::resolve-schema schemas)
  ;(log/debug ::resolve-schema :formdata form-data)
  (when form-data
    (let [t (get form-data "type")
          s (some #(when (= t (-> % :properties :type :enum first)) %) schemas)]
      ;(log/debug ::resolve-schema t :s s)
      ;; we return a empty schema, if we can't find a sub-schema
      (clj->js (or s {}))
      )))

(defn custom-schema-field
  [{:keys [formData schema uiSchema formContext onChange registry name] :as args}]
  ;(.log js/console ::custom-schema-field :schema (js->clj schema))
  ;(.log js/console ::custom-schema-field :formdata (js->clj formData))
  ;(log/debug ::custom-schema-field :uiSchema (js->clj uiSchema))
  (let [
        selected-schema (r/atom nil) ;; this should be js
        folded? (r/atom false)
        dropdown? (r/atom false)
        toggle-dropdown (fn [e] (swap! dropdown? not)
                          (.preventDefault e)
                          (.stopPropagation e))
        toggle-fold (fn [e] 
                      (.preventDefault e)
                      (.stopPropagation e)
                      (swap! folded? not))
        ui-schema (js->clj uiSchema :keywordize-keys false)
        fold? (get-in ui-schema ["ui:options" "foldable"])
        wrap-if-object (if fold?
                         (fn [v]
                           [:div.fold {:class (when @folded? "folded")}
                            [:span.fold-ctrl {:on-click toggle-fold} [:span.glyphicon  {:class (if @folded? "glyphicon-plus" "glyphicon-minus")}]]
                            v
                            ])
                         identity
                         )
        ]
    (fn
      [{:keys [formData schema uiSchema formContext registry name] :as args}]
      ;(.log js/console ::custom-schema-field :schema :inner (js->clj schema))
      ;(.log js/console ::custom-schema-field :schema :js-inner schema )
      (let [
            any-ofs (js->clj (-> schema .-anyOf) :keywordize-keys true)
            on-click (fn [e] (let [val (js/parseInt (-> e .-target .-value))
                                   s (get any-ofs val)
                                   ]
                               (log/debug ::val val (get any-ofs val) any-ofs)

                               ;; wenn wir explizit einen Typ auswählen, initialisieren wir formData neu
                               (onChange (clj->js {:type (-> s :properties :type :enum first)}))
                               (reset! selected-schema s)
                               (toggle-dropdown e)
                               ))
            ]
        [:div
         (when any-ofs
           [:div.dropdown {:class (when @dropdown? "open")}
            [:button.btn.btn-default.dropdown-toggle {:data-toggle "dropdown" :type "button" :aria-has-popup true :aria-expanded false :on-click toggle-dropdown}
             "Select Type" [:span.caret]]
            [:ul.dropdown-menu.list-group {:style {:padding 0 :margin 0 :border 0}}
             (doall (map-indexed (fn [idx option]
                                   [:button.list-group-item {:key idx 
                                                             :on-click on-click
                                                             :type "button"
                                                             :value idx
                                                             } (or (-> option
                                                                       :properties :type :enum first) (str "Opton-" idx))]) any-ofs))
             ]]
           )
         (wrap-if-object
          (if any-ofs
            (if formData
              [:> schema-field
               (assoc args :schema (resolve-schema any-ofs (f/remove-nils (js->clj formData)))
                      :formData (clj->js (f/remove-nils (js->clj formData)))
                      )
               ]
              (when @selected-schema
                [:> schema-field
                 (assoc args :schema @selected-schema
                        :formData (clj->js (f/remove-nils (js->clj formData)))
)
                 ]
                ))
            [:> schema-field
             (assoc args :schema schema
                    :formData (clj->js (f/remove-nils (js->clj formData)))
)
             ]))
         ]
        ))))


(defn content-callout
  [& args]
  (r/reactify-component (apply callout args)))

(def default-fields
  {"select-binary-ref"
   (content-callout select-binary-ref {:dbid "test" ;; todo
                                       :q {}
                                       })
   "select-content-ref"
   (content-callout select-content-ref {:dbid "test" ;; todo
                                        :col "content"
                                        :q {}
                                        })
   "SchemaField" (r/reactify-component custom-schema-field)
   })


(defn json-form
  [args]
  (let [
        content-changed? (rf/subscribe [:content/changed?])
        on-unload (fn [e]
                    (.preventDefault e)
                    (when @content-changed?
                      (set! (.-returnValue e) "Sure?")
                      "Sure?"
                      )
                    )
        ]
    (r/create-class
     {:component-did-mount
      (fn []
        (.addEventListener js/window "beforeunload"
                           on-unload
                           ))

      :component-will-unmount
      (fn []
        ;;(.removeEventListener js/window "beforeunload" on-unload)
        )

      :display-name "json-form"
      :reagent-render
      (fn [args]
        (log/debug ::json-form :args args)
        [:> react-jsonschema-form (merge args {:fields default-fields
                                               :schema (clj->js (:schema args))
                                               :formData (clj->js (f/remove-nils (:formData args)))})]
        )
      })))

