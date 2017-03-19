(ns cmskern.new-content.views
  (:require [re-frame.core :as rf]

            [cmskern.pages :as p]
))


(defn new-content-page
  []
  (let [on-submit (fn [arg]
                    (.log js/console ::on-submit arg))
        ct (rf/subscribe [:current-ct])
        schema (rf/subscribe [:current-schema])
        react-jsonschema-form (aget js/window "deps" "react-jsonschema-form" "default")

        ui-schema {}
        fields {}
        ]
    (fn []
      [:div.new-content
       [:div.row
        [:div.col-md-2]
        [:div.col-md-10
         (if (and @ct @schema)
           [:> react-jsonschema-form {:schema (clj->js @schema)
                                      :onSubmit on-submit
                                      }]
           [:div.waiting "waiting"])]]]))
  )

(defmethod p/page :new-content-page
  []
  [:div.container-fluid
   [p/header]
   [new-content-page]]
  )

