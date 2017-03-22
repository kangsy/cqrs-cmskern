(ns cmskern-devcards.widgets
  (:require
   [re-frame.core :as rf]
   [reagent.core :as r]
   [sablono.core :as sab :include-macros true]
   [devcards.core :as dc :include-macros true]

   [cljs.test :as t :include-macros true :refer-macros [testing is]]

   [cmskern.views.widgets :as w])
  (:require-macros
   [taoensso.timbre :as log]
   [devcards.core :refer [defcard deftest]]))

(defcard Schema-Field
  (let [
        schema {
                "title" "Sections",
                "type" "array",
                "items" {"anyOf" [
                                    {"type" "object",
                                     "title" "Auto"
                                     "properties"
                                     {"strs" {"type" "string"}
                                      "type" {"type" "string"
                                              "enum" ["auto"]}}}
                                    {"type" "object",
                                     "title" "Manual"
                                     "properties"
                                     {"ids" {"type" "string"}
                                      "type" {"type" "string"
                                        "enum" ["manual"]}}}
                                    ]
                         }
                }
        ui-schema {"ui:options" {"foldable" true}}
        formdata []
        on-validate (fn [formData errors] (.log js/console ::on-validate formData errors) errors)
        on-submit (fn [arg] (.log js/console ::onsubmit arg))]
    (dc/reagent
     [:div nil
      [:h2 "Schema-Field with any-of"]
      [:p nil [:code nil schema]]
      [w/json-form {:schema (clj->js schema)
                    :uiSchema (clj->js ui-schema)
                    :formData (clj->js formdata)
                    :onSubmit on-submit
                    :validate on-validate
                    }]
      ])))

(defcard Callout
  (let [elem (w/callout
              w/select-content-ref {:dbid "test"
                                    :col "content"
                                    :q {}})
        schema {"type" "object",
                "properties" {
                              "order_value" {
                                             "title" "Order",
                                             "type" "integer"
                                             }}
                }
        ui-schema {}
        formdata {"order_value" nil}
        ]
    (dc/reagent
     [:div nil
      [:h2 "content-callout"]
      [elem {:schema (clj->js schema)
             :uiSchema (clj->js ui-schema)
             :formData (clj->js formdata)
             }]
      ])))

(defcard Json-Form
  (let [schema {"type" "object",
                "properties" {
                              "order_value" {
                                             "title" "Order",
                                             "type" "integer"
                                             }}
                }
        ui-schema {"order_value" {"ui:options" {"foldable" true}}}
        formdata {"order_value" 1}
        on-submit (fn [arg] (.log js/console ::onsubmit arg))]
    (dc/reagent
     [w/json-form {:schema (clj->js schema)
                   :uiSchema (clj->js ui-schema)
                   :formData (clj->js formdata)
                   :onSubmit on-submit
                   }]))
  )

(defcard Select-Binary
  (dc/reagent
   [:div nil
    [w/select-binary-ref {:dbid "test"
                          :q {}}
     :on-delete (fn [asset cb] (rf/dispatch [:gfs-delete :global {:db "test" :id (:_id asset)} cb]))
     ]]))

(defcard Select-Contents
  (dc/reagent
   [:div nil
    [w/select-content-ref {:dbid "test"
                           :col "content"
                           :q {}}
     :preview (fn [v] [:span {:style {:background (get-in v [:data :value])}} "X"])
     ]]))
