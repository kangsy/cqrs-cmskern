(ns cqrs-crmkern.consumer-test
  (:require [mount.core :refer [defstate]]
            [clojure.test :refer :all]
            [clojure.test.check :as tc]

            [clojure.core.async.impl.protocols :as p]
            [slingshot.slingshot :refer [throw+ try+]]
            [gniazdo.core :as ws]

            [taoensso.timbre :as log]

            [clojure.spec :as spec]
            [clojure.spec.gen :as sgen]
            [clojure.spec.test :as stest]
            [franzy.serialization.nippy.deserializers :as nippy-deserializers]

            [environ.core :refer [env]]
            [clj-http.client :as client]

            [clojure.core.async :as a]
            [kinsky.async       :as async]

            [cqrs-crmkern.consumer :as cc :refer [events-ch events-pub]]
            [cqrs-crmkern.producer :as cp])

  (:import (java.util Date)
           (franzy.serialization.nippy.deserializers NippyDeserializer)))




(comment

 (def eid "123")
 (def rch (a/promise-chan))
;(def events-sub (a/sub events-pub eid rch))

 (defn start-returner [in]
   (a/go-loop [v (a/<! in)]
     (when v
       (log/info "returning " v)
       (dp/send-event! v)
       (recur (a/<! in))))
   in)

 (defn make-test-consumer
   ""
   [process-fn topic groupid]
   (let [
         value-deserializer (nippy-deserializers/nippy-deserializer)
         [out ctl] (async/consumer {:bootstrap.servers "localhost:9092"
                                    :group.id         groupid}
                                   value-deserializer
                                   value-deserializer)

         mult (a/mult out)
         copy (a/chan)
         int-chan (a/chan)
         _ (a/tap mult copy)
         _ (a/tap mult int-chan)]


     (a/put! ctl {:op :subscribe :topic topic})
     (a/go-loop []
       (when-let [record (a/<! int-chan)]
         (log/debug ::make-test-consumer record)
         (when (= (:type record) :record)
           (log/debug ::make-test-consumer "incoming test-event" record)
           (process-fn record))
         (recur)))
     [copy ctl]))
    ;;(a/put! ctl {:op :partitions-for :topic topic})
    ;;(a/put! ctl {:op :commit})


 (defstate ^{:on-reload :noop} test-consumer-2
   :start (make-test-consumer #(when true (log/debug "test-consumer 2: " %)) "test" "test-consumer-2")
   :stop (a/put! (second test-consumer-2) {:op :stop}))


 (defstate ^{:on-reload :noop} test-consumer
   :start (make-test-consumer #(when true (log/debug "test-consumer: " %)) "test" "domain-consumer")
   :stop (when test-consumer (a/put! (second test-consumer) {:op :stop})))

;; testen, ob kafka events multiplexed werden
 (defstate ^{:on-reload :noop} test-pub
   :start (a/pub (first test-consumer) (comp :parent :data :value)))



 (deftest test-pub-sub
   (testing ""
     (let [pub (a/>!! returner {:parent eid})
           res (try
                 (a/alt!!
                   rch
                   ([v] (assoc v :status :ok))
                   (a/timeout 3000)
                   ([v] :time-out))
                 (catch Exception e (log/error "Exception return loop " e))
                 (finally
                   (a/close! rch)
                   (a/unsub events-pub eid rch)))]
       (is (= :ok res)))))



 (def value-deserializer (nippy-deserializers/nippy-deserializer))



 (defstate handler
   :start (make-consumer-handler)
   :stop (a/close! handler)))


(comment

  "Ich gehe erst einmal vom Modell aus, wo der Frontend-Server nur einen
Pub-basierten Rückkanal hat."
  (deftest test-events-ch
    (testing ""
      (let [
            rm (cp/send!
                cp/producer
                "events"
                {:a 1})
            res (a/alt!!
                  (first cc/events-ch)
                  ([v] :ok)
                  (a/timeout 2000)
                  ([v] :timeout))]

        (is (= :ok res))))))


(deftest test-pub-ch
  (testing ""
    (let [pch (a/promise-chan)
          _ (a/sub cc/events-pub "123" pch)
          data {:data {:parent "123"
                       :ts (java.util.Date.)
                       :x 1}}

;; das simuliert das senden eines biz-services, welches ein Result als event liefert
          rm (cp/send!
              cp/producer
              "events"
              data)]

      (is (= data
             (:value (try (a/alt!!
                            pch
                            ([v] v)
                            (a/timeout 2000)
                            ([v] :timeout))
                          (finally
                            (a/close! pch)
                            (a/unsub cc/events-pub "123" pch)))))))))
(deftest test-query-ch
  (testing ""
    (let [
          rm (cp/send!
              cp/producer
              "query"
              {
               :eid "123"
               :db "ea2"
               :col "orders"
               :query {}})
          res (a/alt!!
                (first cc/query-ch)
                ([v] :ok)
                (a/timeout 2000)
                ([v] :timeout))]

      (println res)
      (is (= :ok res)))))
