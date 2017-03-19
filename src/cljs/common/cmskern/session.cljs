(ns cmskern.session
  (:require
   [re-frame.core :as rf]
   [goog.storage.mechanism.HTML5LocalStorage]
   [taoensso.timbre :as log])
  )

(def storage (goog.storage.mechanism.HTML5LocalStorage.))


(rf/reg-fx
 :localstorage
 (fn [[action args]]
   (log/debug ::localstorage action args)
   (case action
     :set (.set storage (first args) (second args))
     :remove (.remove storage args)
     nil)
   ))

