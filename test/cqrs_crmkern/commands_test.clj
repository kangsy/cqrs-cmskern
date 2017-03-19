(ns cqrs-crmkern.commands-test
  (:require [clj-http.client :as client]
            [clojure.test :refer :all]
            [clojure.test.check :as tc]

            [slingshot.slingshot :refer [throw+ try+]]
            [gniazdo.core :as ws]

            [clojure.spec :as spec]
            [clojure.spec.gen :as sgen]
            [clojure.spec.test :as stest]

            [environ.core :refer [env]]))


(def host (str "http://localhost:" (env :port)))

(defn- post
  [data]
  (:body (client/post (str host "/c") {:form-params data
                                       :content-type :json
                                       :coerce :always
                                       :as :json})))


(deftest test-basic-http-command
  (testing "Parameter destructuring and command mapping"

    (let [result (post {:cmd "auth"
                        :eid 1234
                        :args [{:id "kang" :password "1234"}]})]
      (is (map? result))
      (is (= "auth" (:cmd result)))
      (is (= [{:id "kang" :password "1234"}] (:args result)))
      (is (contains? result :eid))))


  #_(testing "basic args error"

     (try+
      (post {:cmd "auth"
             :args {:id 111 :password "1234"}})
      (catch [:status 400] {:keys [body]}
        (println "body " body)
        (is (contains? body :error))))))



(deftest test-domain-result
  (testing "Parameter destructuring and command mapping"
    (let [
          res (atom nil)
          socket (ws/connect "ws://ws"
                             {:on-receive #(reset! res %)})
          result (post {:cmd "auth"
                        :eid 1234
                        :args [{:id "kang" :password "1234"}]})]
      (println @res)
      (is (map? @res))
      (is (= "auth" (:cmd @res)))
      (is (= [{:id "kang" :password "1234"}] (:args @res)))
      (is (contains? @res :eid)))))
