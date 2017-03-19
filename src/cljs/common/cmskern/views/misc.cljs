(ns cmskern.views.misc
  (:require-macros [cmskern.ui.core :refer [handler-fn]]
)
  (:require
   [reagent.core :as r]
   [re-frame.core :as rf]
   [cmskern.views.utils :refer [deref-or-value]]
   [cmskern.functions :as f]
   ))


(def spinner
  [:div.spinner
   [:div.bounce1]
   [:div.bounce2]
   [:div.bounce3]
   ])


(defn modal-box
  [head body & {:keys [on-close]}]
  [:div.modal-open
   [:div.modal.fade.in {:style {:display :block} :tabIndex "-1", :role "dialog", :aria-labelledby "myModalLabel", :aria-hidden "true"}
    [:div.modal-backdrop.fade.in {:style {:z-index 0} :on-click on-close}]
    [:div.modal-dialog
     [:div.modal-content
      [:div.modal-header
       [:button {:on-click on-close :type "button", :class "close", :aria-hidden "true"} "×"]
       head
       ]
      [:div.modal-body nil body]]]]])

(defn preview-fn
  [dbid type data]
  (case type
    "color"
    [:span {:style {:background (get-in data [:data :value])}} "X"]

    "image/jpeg"
    [:img {:src (str "/bin/" dbid "/preview-thumb/" (f/get-value-str (:_id data)) ".jpg")}]

    "image/gif"
    [:img {:src (str "/bin/" dbid "/preview-thumb/" (f/get-value-str (:_id data)) ".gif")}]

    "image/png"
    [:img {:src (str "/bin/" dbid "/preview-thumb/" (f/get-value-str (:_id data)) ".png")}]
    "asset"
    [:img {:src (str "/bin/" dbid "/preview-thumb/" (f/get-value-str (:idref data)) ".jpg")}]
    "image"
    [:img {:src (str "/img/" dbid "/preview-thumb/" (f/get-value-str (:idref data)) ".jpg")}]
    nil
    ))

(defn status-panel
  [uuid]
  (let [status (rf/subscribe  [:results (or uuid :global) :status])]
    (r/create-class
     {:display-name "status-panel"
      :component-will-unmount
      (fn []
        (rf/dispatch [:db/delete (vec (flatten [:results (or uuid :global)]))])
        )
      :reagent-render
      (fn []
        [:div.alert {:class (str "alert-" (:status @status))} (:msg @status)])})
    ))
