(ns cqrs-crmkern.config-test
  (:require
   [clojure.test :refer :all]
   [cqrs-crmkern.commands.config :refer :all]))




(deftest test-kafka-producer
  (let [
        topic "land-wars-in-asia"
        first-partition 0
        second-partition 1
        third-partition 2


        producer-record-map {:topic     "xx"
                             :partition first-partition
                             :key       :six-fingered-man
                             :value     {:inventions ["The machine" "Goofy hallyway running"]
                                         :six-finger true
                                         :hand       :left
                                         :hobbies    #{"Sword butt hitting" "Being fresh with princes"}}}
        ;;notice we use the factory function for our record this time and refer....whichever you prefer sir/madame
        ;;sending the producer record explicitly
        ;;sending the producer record map explicitly
        record-metadata (send-sync! kafka-producer producer-record-map)]
    (println "producer" kafka-producer)
    (println "Sync send results:" record-metadata)))
    ;;and one last record returned directly into your repl, this time using explicit params and passing a nil options for fun
