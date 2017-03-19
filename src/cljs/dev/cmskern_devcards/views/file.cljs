(ns cmskern-devcards.views.file
  (:require
   [re-frame.core :as rf]
   [reagent.core :as r]
   [taoensso.timbre :as log]
   [ajax.core :as ajax :refer [GET POST PUT DELETE]]
   [sablono.core :as sab :include-macros true]
   [devcards.core :as dc :include-macros true]

   [cljs.test :as t :include-macros true :refer-macros [testing is]]

   [cmskern.views.file :as f])
  (:require-macros
   [devcards.core :refer [defcard deftest]]))


(rf/reg-event-db
 ::upload-file
 rf/debug
 (fn [db [_ dbid uuid file ]]
   (.log js/console ::upload-file dbid file)
   (when file
     (let [upload (doto
                      (js/FormData.)
                    (.append "upload" file)
                    )]
       (log/debug ::upload-file-cljs :inner dbid upload file)
       (.log js/console ::upload-file :inner dbid upload file)
       (POST (str "/_asset/" dbid )
             {:body upload
              :headers {:authorization (str "Bearer " (:token db))}
              :response-format :raw
              :keywords? true
              :handler #(rf/dispatch [::on-success uuid %1])
                                        ;:error-handler handle-response-error
              })
       (assoc db :waiting? true))
     )
   db
   ))
(defcard File-Input
  (let [dbid "test"
        uuid (random-uuid)
        on-change (fn [e] (.preventDefault e)
                    (.log js/console "change event" e)
                    (rf/dispatch [::upload-file dbid uuid (-> e .-target .-files (aget 0))]))]
    (dc/reagent
     [:form {:method "POST"}
      [:input {:type "file" :onChange on-change}]])))


(defcard File-Upload
  (let [dbid "test"]
    (dc/reagent
     [f/file-upload :dbid dbid])))
