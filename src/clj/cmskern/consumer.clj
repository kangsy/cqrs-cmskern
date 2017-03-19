(ns cmskern.consumer
  (:require [mount.core :refer [defstate]]
            [clojure.core.async :refer [put! chan <! >! go-loop close!] :as a]
            [taoensso.timbre :as log]
            [clj-uuid :as uuid]

            [franzy.serialization.nippy.deserializers :as nippy-deserializers]

            [kinsky.client      :as client]
            [kinsky.async       :as async]

            ;; todo refactor failure out of http-end-ns
            [conqueress.failure :refer [fail]]
            [cmskern.producer :as dp]
            )
  )


(defn create-kafka-channel
  ""
  [topic]
  (let [
        value-deserializer (nippy-deserializers/nippy-deserializer)
        ;; TODO config
        [out ctl] (async/consumer {:bootstrap.servers "localhost:9092"
                                   :group.id         "crmkern-consumer"}
                                  :keyword
                                  value-deserializer)]

    (a/put! ctl {:op :subscribe :topic topic})
    [out ctl]
    ))

(defstate commands-ch
  :start (let [pipe-trans (fn [ci xf]
                            (let [co (chan 1 xf)]
                              (a/pipe ci co)
                              co))
               [raw ctl] (create-kafka-channel "commands")
               in (pipe-trans raw (filter #(= (:type %) :record)))
               ]
           [in ctl]
           )
  :stop (do (a/put! (second commands-ch) {:op :stop})
            (a/close! (first commands-ch))))

