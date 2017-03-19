(ns cmskern.views.core
  (:require [re-frame.core :as rf]
            [reagent.core :as r]

            [cmskern.functions :as f]
            [cmskern.subs]
            [cmskern.routes :as route]
            [cmskern.pages :as p]

            [cmskern.login.views]
            [cmskern.index.views]
            [cmskern.admin.views]
            [cmskern.content.views]

            [cmskern.views.misc :as misc])
  (:require-macros [cmskern.ui.core :refer [handler-fn]])
  )


(defmethod p/page :four-o-four
  []
  [:div.index
   ":four-o-fourNDEX"])


(defn layout []
  (let [ready?  (rf/subscribe [:initialised?])
        r (rf/subscribe [:route])
        token (rf/subscribe [:db [:token]])
        ]
    (fn []
      (if-not @ready?         ;; do we have good data?
        misc/spinner   ;; tell them we are working on it
        (if @token
          (p/render-page @r)
          (p/render-page {:handler :login-page})
          )
        )
      )))

