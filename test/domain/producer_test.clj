(ns domain.producer-test
  (:require
   [clojure.test :refer :all]

   [franzy.serialization.nippy.serializers :as nippy-serializers]
   [kinsky.client      :as client]

   [domain.producer :as dp]
   )
  (:import (org.apache.kafka.common.serialization Deserializer ByteArrayDeserializer Serializer ByteArraySerializer)
           (org.apache.kafka.clients.producer MockProducer )))

(def value-serializer (nippy-serializers/nippy-serializer))

(deftest test-send-mock
  (let [mock (MockProducer. true value-serializer value-serializer)
        ]
    (testing "send! function with mock"
      (let [_ (.clear mock)
            _ (dp/send! (client/producer->driver mock) "test" {:a 1})
            hist (.history mock)
            ]
        (is (= {:a 1} (.value (first hist))) ))
      )))

(deftest test-send-prod
  (let [
        ]
    (testing "send! function returns record-metadata"
      (let [
            rm (dp/send!
                dp/producer
                "test"
                {:a 1})
            ]
        (is (= "test" (.topic (.get rm))) ))
      )))
