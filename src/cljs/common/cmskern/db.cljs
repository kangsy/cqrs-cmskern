(ns cmskern.db
  (:require [cmskern.config :as config]
            [ajax.core :as ajax :refer [GET POST PUT DELETE]]
            ))


(defn path-for-type
  ""
  [type]
  (condp = type
    :dbs [:data :databases])
  )

(defn get-data
  ""
  [type query & opts]
  (let [options (apply hash-map opts)
        on-success (:on-success options)]
    (GET
     config/query-endpoint
     {:params {:db "db1" :col "todo" :query {}}
      :handler       #(on-success %)
      }))     ;; <2> further dispatch !!
  )
