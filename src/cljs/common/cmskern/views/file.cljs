(ns cmskern.views.file
  (:require-macros [cmskern.ui.core :refer [handler-fn]])
  (:require
   [re-frame.core :as rf]
   [reagent.core :as r]
   [taoensso.timbre :as log]
   [accountant.core :as accountant]

   [cmskern.views.utils :refer [deref-or-value]]
   [cmskern.views.misc :as misc]
   [cmskern.functions :as f :refer [tr]]
   ))


(defn file-upload
  [& {:keys [dbid status status-icon? status-tooltip disabled? class on-done]
    :or {on-done (fn [p] (log/debug ::file-upload :change p))
         }}]
  (log/debug ::file-upload dbid)
  (let [
        uuid (random-uuid)
        internal-model (r/atom [])
        file-type "image/png"
        ;dropzone (aget js/window "deps" "react-dropzone")
        on-drop (fn [f] (.log js/console ::on-drop f)
                  (swap! internal-model #(apply conj % (js->clj f)))
                  #_(doseq [file f]
                    (.log js/console ::doseq file )
                    (rf/dispatch [:gfs-upload-file dbid [uuid (.-name file)] file ]))
                  )
        waiting? (r/atom false)
        done? (rf/subscribe [:result-status uuid])
        on-submit (handler-fn (reset! waiting? true)
                              (doseq [file @internal-model]
                                (.log js/console ::doseq file )
                                (rf/dispatch [:gfs-upload-file dbid [uuid (.-name file)] file ]))
                              )
        -on-done (handler-fn (reset! waiting? false)
                            (rf/dispatch [:cleanup [:results uuid]])
                            (reset! internal-model [])
                            (on-done)
                            )

        ]
    (fn
      [& {:keys [dbid status status-icon? status-tooltip disabled? class]}]
      (let [
            disabled?        (deref-or-value disabled?)
            ]
        [:div.file-upload
         {:class (str "inner "          ;; form-group
                      (case status
                        :success "has-success "
                        :warning "has-warning "
                        :error "has-error "
                        "")
                      (when (and status status-icon?) "has-feedback"))} 
                                        ;(misc/preview-fn dbid file-type @internal-model)
         (comment
           [:> dropzone {:className "dropzone" :onDrop on-drop}
            [:div nil (tr ["Bild-Datei hier hinein droppen, um neu hochzuladen."])]
            [:div nil (tr ["Oder klicken für Auswahl"])]
            ])
         (when (not-empty @internal-model)
           [:div nil
            (doall
             (map-indexed (fn [idx f]
                            (.log js/console ::f f)
                            ^{:key idx}
                            [:div.preview
                             [:img {:class "preview" :src (.-preview f)}]
                             [:br]
                             [:button.btn.btn-danger.btn-xs {:on-click (handler-fn (swap! internal-model f/vec-remove idx)) :class (when @waiting? "disabled")} "Delete"]
                             [misc/status-panel [uuid (.-name f)]]
                             ]
                            ) @internal-model))

            (when-not @waiting?
              [:button.btn.btn-primary {:on-click on-submit} (tr ["Hochladen"])])
            (when (and @waiting? (not @done?))
              [:div
               [:h4 nil (tr ["Uploading..."]) (.-length @internal-model)]
               misc/spinner
               ])
            (when @done?
                  [:button.btn.btn-primary {:on-click -on-done} (tr ["Erledigt"])])
            ]
           )
         (when status-tooltip
           [:div.help-block status-tooltip]
           )
         ]))))

