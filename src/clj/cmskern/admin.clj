(ns cmskern.admin
  "Admin-Funktionalitäten"
  (:require 
            [taoensso.timbre :as log]
            [crypto.password.bcrypt :as pw]

            [conqueress.monads :refer [attempt-all]]
            [conqueress.failure :refer [fail]]

            [cmskern.db :as db]
            ))


(defn save-database
  ""
  [data]
  (log/debug ::save-database data)
  (let [result
        (db/upsert "databases" {:dbid (:dbid data)} data )]
    (log/debug ::save-database :result result)
    result
    )
  )
(defn save-content-type
  ""
  [dbid data]
  (log/debug ::save-content-type dbid data)
  (let [result
        (db/upsert dbid "contentTypes" {:name (:name data)} data )]
    (log/debug ::save-content-types :result result)
    result
    )
  )

(defn save-user
  ""
  [data]
  (log/debug ::save-user data)
  (let [result
        (db/upsert "users" {:email (:email data)} data )]
    (log/debug ::save-user :result result)
    result
    )
  )

(defn change-user-password
  ""
  [uid pw pw2]
  (log/debug ::change-user-password uid)
  (attempt-all [user (or (db/get "users" {:_id uid}) (fail :no-user))
                passwd (or (and (= pw pw2) (not-empty pw)) (fail :pw-error))
                h (pw/encrypt passwd)
                change {"$set" {:passwordHash h}}
                result (db/update "users" {:email (:email user)} change )
                ]
               result
   )
  )
