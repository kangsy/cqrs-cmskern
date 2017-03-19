(ns cmskern.login.views
  (:require-macros [cmskern.ui.core :refer [handler-fn]])
  (:require [re-frame.core :as rf]
            [reagent.core :as r]
            [taoensso.timbre :as log]

            [cmskern.views.inputs :as vi]
            [cmskern.login.events]
            [cmskern.login.subs]
            [cmskern.pages :as p]
            ))

(defn login-panel
  ""
  []
  (let [login-val (r/atom {:email nil :password nil})
        status-tooltip (r/atom "")
        uuid (r/atom (random-uuid))
        errors (rf/subscribe [:results @uuid :errors])
        submit (handler-fn (.preventDefault event)
                           (rf/dispatch [:login/submit-login @uuid @login-val]))]
    (fn []
      (log/debug ::login-panel @login-val)
      [:div.login
       [:div {:class "pure-form pure-form-stacked"}
        [:form {:class "pure-form pure-form-stacked" :on-submit submit}
         [:fieldset  
        [vi/input-control
         :model            (:id @login-val)
         :input-type       :text
         :status           nil
         :status-icon?     true
         :status-tooltip   @status-tooltip
         :width            "300px"
         :placeholder      "E-Mail"
         :label      "E-Mail"
         :on-change        #(swap! login-val assoc :email %)
         :change-on-blur?  false
         :disabled?        false]
        [vi/input-control
         :model            (:id @login-val)
         :input-type       :password
         :status           nil
         :status-icon?     true
         :status-tooltip   @status-tooltip
         :width            "300px"
         :placeholder      "Passwort"
         :label      "Passwort"
         :on-change        #(swap! login-val assoc :password %)
         :change-on-blur?  false
         :disabled?        false]

        [:div {:class "pure-controls"}
         [:button.pure-button.pure-button-primary {:type :submit} "Submit"]]
        [:div.help-block nil @uuid]
        [:div.help-block nil (:form @errors)]
        ]]]])))

(defmethod p/page :login-page
  []
  [:div.login-page.pure-g
    [:div.pure-u-1.pure-u-md-1-4
    [login-panel]]])
