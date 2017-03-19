(ns cmskern-devcards.forms
  (:require
   [reagent.core :as r]
   [sablono.core :as sab :include-macros true]
   [devcards.core :as dc :include-macros true]

   [cljs.test :as t :include-macros true :refer-macros [testing is]]

   [cmskern.views.forms :as vf])
  (:require-macros
   [devcards.core :refer [defcard deftest]])
  )

(defonce data    {:input-text {:args [:model "xx"
                                      :status nil
                                      :status-icon?     false
                                      :width            "300px"
                                      :on-change        #(swap! data assoc-in [:input-text :data] %)]
                               :data nil
                               }
                  })

(defonce input-text-data (r/atom nil))
(defcard Input-Text
  "*Standard text input-field mit verschiedenen Status*"
  (dc/reagent
     ;[(apply input-text (-> @data :input-text :args))]
   [:form.pure-form.pure-form-aligned
    [:div.pure-control-group
     [vf/input-text
      :model nil
      :placeholder "Normal"
      :status-icon?     true
      :status nil
      :on-change        #(reset! input-text-data %)

      ]]
    [:div.pure-control-group
     [vf/input-text
      :model nil
      :placeholder "Error"
      :status-icon?     true
      :status-tooltip "Oh nein!"
      :status :error
      :on-change        #(reset! input-text-data %)
      ]]
    [:div.pure-control-group
     [vf/input-text
      :model nil
      :placeholder "Success"
      :status-icon?     true
      :status :success
      :status-tooltip "Alles klar"
      :on-change        #(reset! input-text-data %)
      ]]
    [:div.pure-control-group
     [vf/input-text
      :model nil
      :placeholder "Warning"
      :status-icon?     true
      :status-tooltip "Gelb Gelb!"
      :status :warning
      :on-change        #(reset! input-text-data %)
      ]]
    ]
     )
  input-text-data
  {:inspect-data true
   :frame true
   :history true }
  )
(defcard Input-Password
  "*Standard Passwort input-field mit verschiedenen Status*"
  (dc/reagent
     ;[(apply input-text (-> @data :input-text :args))]
   [:form.pure-form.pure-form-aligned
    [:div.pure-control-group
     [vf/input-password
      :model nil
      :placeholder "Passwort"
      :status-icon?     true
      :status nil
      :on-change        #(reset! input-text-data %)

      ]]
    [:div.pure-control-group
     [vf/input-password
      :model nil
      :placeholder "Passwort"
      :status-icon?     true
      :status-tooltip "Oh nein!"
      :status :error
      :on-change        #(reset! input-text-data %)
      ]]
    [:div.pure-control-group
     [vf/input-password
      :model nil
      :placeholder "Passwort"
      :status-icon?     true
      :status :success
      :status-tooltip "Alles klar"
      :on-change        #(reset! input-text-data %)
      ]]
    [:div.pure-control-group
     [vf/input-password
      :model nil
      :placeholder "Passwort"
      :status-icon?     true
      :status-tooltip "Gelb Gelb!"
      :status :warning
      :on-change        #(reset! input-text-data %)
      ]]
    ]
     )
  input-text-data
  {:inspect-data true
   :frame true
   :history true }
  )
(defcard Input-Control
  "*input-field mit Label und wrapper"
  (dc/reagent
   [:form.pure-form.pure-form-stacked
    [vf/input-control
     :label "Normal"
     :model nil
     :placeholder "Normal"
     :status-icon?     true
     :status nil
     :on-change        #(reset! input-text-data %)

     ]
    [vf/input-control
     :label "Error"
     :model nil
     :placeholder "Error"
     :status-icon?     true
     :status-tooltip "Oh nein!"
     :status :error
     :on-change        #(reset! input-text-data %)
     ]
    [vf/input-control
     :label "Success"
     :model nil
     :placeholder "Success"
     :status-icon?     true
     :status :success
     :status-tooltip "Alles klar"
     :on-change        #(reset! input-text-data %)
     ]
    [vf/input-control
     :label "Warning"
     :model nil
     :placeholder "Warning"
     :status-icon?     true
     :status-tooltip "Gelb Gelb!"
     :status :warning
     :on-change        #(reset! input-text-data %)
     ]
    ]
     )
  input-text-data
  {:inspect-data true
   :frame true
   :history true }
  )

(defonce text-area-data (r/atom nil))
(defcard Text-Area
  "*Standard text-area mit verschiedenen Status*"
  (dc/reagent
     ;[(apply input-text (-> @data :input-text :args))]
   [:form.pure-form.pure-form-aligned
    [:div.pure-control-group
     [vf/input-textarea
      :model nil
      :placeholder "Normal"
      :status-icon?     true
      :status nil
      :on-change        #(reset! text-area-data %)

      ]]
    [:div.pure-control-group
     [vf/input-textarea
      :model nil
      :placeholder "Error"
      :status-icon?     true
      :status-tooltip "Oh nein!"
      :status :error
      :on-change        #(reset! text-area-data %)
      ]]
    [:div.pure-control-group
     [vf/input-textarea
      :model nil
      :placeholder "Success"
      :status-icon?     true
      :status :success
      :status-tooltip "Alles klar"
      :on-change        #(reset! text-area-data %)
      ]]
    [:div.pure-control-group
     [vf/input-textarea
      :model nil
      :placeholder "Warning"
      :status-icon?     true
      :status-tooltip "Gelb Gelb!"
      :status :warning
      :on-change        #(reset! text-area-data %)
      ]]
    ]
     )
  text-area-data
  {:inspect-data true
   :frame true
   :history true }
  )
