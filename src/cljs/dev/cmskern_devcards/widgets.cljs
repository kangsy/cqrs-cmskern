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


(def sstr " {
    \"title\": \"Sections\",
    \"type\": \"array\",
    \"items\":{
        \"anyOf\": [
            {
                \"type\":\"object\",
                \"title\": \"Raw\",
                \"properties\": {
                    \"type\": {
                        \"type\": \"string\",
                        \"enum\": [\"Raw\"]
                    },
                    \"body\": {
                        \"title\": \"Body\",
                        \"type\":\"string\"
                    }
                }
            },
            {
                \"type\":\"object\",
                \"title\": \"Body\",
                \"properties\": {
                    \"type\": {
                        \"type\": \"string\",
                        \"enum\": [\"Body\"]
                    },
                    \"body\": {
                        \"title\": \"Body\",
                        \"type\":\"string\"
                    }
                }
            },
            {
                \"type\":\"object\",
                \"title\": \"Query\",
                \"properties\": {
                    \"type\": {
                        \"type\": \"string\",
                        \"enum\": [\"Query\"]
                    },
                    \"title\":{
                        \"title\":\"Section Titel\",
                        \"type\":\"string\"
                    },
                    \"description\":{
                        \"title\":\"Section Description\",
                        \"type\":\"string\"
                    },
                    \"teaser_type\":{
                        \"title\":\"Teasertyp\",
                        \"type\":\"string\",
                        \"enum\":[\"image\", \"Bühne\", \"Imagebox1\", \"Imagebox2\", \"Vertikal\", \"Gallery\", \"Horizontal\", \"2er-Block\", \"Logo\", \"News\"]
                    },
                    \"query\": {
                        \"title\": \"Query\",
                        \"type\":\"string\"
                    }
                }
            },
            {
                \"type\":\"object\",
                \"title\": \"Manual\",
                \"properties\": {
                    \"type\": {
                        \"type\": \"string\",
                        \"enum\": [\"Manual\"]
                    },
                    \"title\":{
                        \"title\":\"Section Titel\",
                        \"type\":\"string\"
                    },
                    \"description\":{
                        \"title\":\"Section Description\",
                        \"type\":\"string\"
                    },
                    \"teaser_type\":{
                        \"title\":\"Teasertyp\",
                        \"type\":\"string\",
                        \"enum\":[\"image\", \"Bühne\", \"Imagebox1\", \"Imagebox2\", \"Vertikal\", \"Gallery\", \"Horizontal\", \"2er-Block\", \"Logo\", \"News\"]
                    },
                    \"articles\":{
                        \"title\":\"Articles\",
                        \"type\":\"array\",
                        \"items\":{
                            \"title\":\"Article\",
                            \"type\":\"object\",
                            \"properties\":{
                                \"idref\":{
                                    \"title\":\"Artikel\",
                                    \"type\":\"integer\",
                                    \"description\":\"References an internal URL to a selected article.\"
                                },
                                \"catchline\":{
                                    \"title\":\"Spitzmarke\",
                                    \"type\":\"string\"
                                },
                                \"title\":{
                                    \"title\":\"Headline\",
                                    \"type\":\"string\"
                                },
                                \"teaser\":{
                                    \"title\":\"Teaser\",
                                    \"type\":\"string\"
                                },
                                \"imageref\":{
                                    \"title\":\"Image ID\",
                                    \"description\":\"References an internal URL to a selected image.\",
                                    \"type\":\"integer\"
                                }
                            }
                        }
                    }
                }
            }
        ]
    }
}
")

(defcard Schema-Field
  (let [
        schema (js->clj (.parse js/JSON sstr))
        ui-schema {"ui:options" {"foldable" true}}
        ;formdata [{"type" "Manual", "title" "aaa", "description" "", "teaser_type" "image", "articles" [{"idref" "22" "catchline" "", "title" "", "teaser" "", "imageref" ""}] }]
        formdata (clj->js (w/remove-nils [{:type "Manual",
                                           :title nil,
                                           :description nil,
                                           :teaser_type "image",
                                           :articles
                                           [{:idref 22
                                             :catchline nil,
                                             :title nil,
                                             :teaser nil,
                                             :imageref nil}]}]))
        on-validate (fn [formData errors]
                      (.log js/console ::on-validate formData errors)
                      errors)
        on-submit (fn [arg] (log/debug "submitting arg") (log/debug ::formdata (:formData (js->clj arg :keywordize-keys true)))
                    (.log js/console ::onsubmit arg))]
    (dc/reagent
     [:div nil
      [:h2 "Schema-Field with any-of"]
      [:div nil [:pre nil (with-out-str (cljs.pprint/pprint schema))]]
      [w/json-form {:schema (clj->js schema)
                    :uiSchema (clj->js ui-schema)
                    :formData formdata
                    :onSubmit on-submit
                    :liveValidate true
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
