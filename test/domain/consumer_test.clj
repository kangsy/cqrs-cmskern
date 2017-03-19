(ns domain.consumer-test
  (:require [clojure.test :refer :all]
            [environ.core :refer [env]]
            [clojure.core.async :refer [put! chan <! >! go-loop close!] :as a]

            [domain.consumer :as dc]
            [domain.producer :as dp]
            [cqrs-crmkern.failure :refer [failure?]]))



#_(deftest test-consumer-command-handler
   (testing "basics"
     (let [result (consumer-command-handler {:value {:eid 123 :cmd "auth" :args [{:id "kang" :password "1234"}]}})]
       (is (= {:parent 123 :result "token-1234"} (select-keys result [:parent :result]))))))



(deftest test-commands-ch
  (testing "Basic incoming commands-channel"
    (let [
          ;; simulating a producer, which creates a command-event
          rm (dp/send!
              dp/producer
              "commands"
              {:a 1})
          res (a/alt!!

                ;; this is our incoming channel for commands
                (first dc/commands-ch)
                ([v] v)
                (a/timeout 2000)
                ([v] :timeout))]

      (is (= {:a 1} (:value res))))))
