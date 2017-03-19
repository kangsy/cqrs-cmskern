(ns cmskern.ui.commands
  (:require
   [taoensso.timbre :as log]

   [conqueress.failure :refer [failure? fail]]
   [conqueress.monads :refer [attempt-all]]

   [cmskern.utils :as u]
   [cmskern.auth :as auth]
   [cmskern.db :as db]
   ))

(defn auth
  "UI-Wrapper for auth command"
  [f]
  (fn [record]
    (let [result (apply f (:args record))]
      (if-not (failure? result)
        {:type :return
         :result {:token (auth/generate-new-token (str (:_id result)))
                   :transact {:db/add [[:user] (u/shadow-user result)]}}}
        result
        )
      )))

(defn change-user-password
  "UI-Wrapper for user change-pw command"
  [f]
  (fn [record]
    (log/debug ::change-user-password :record record)
    (let [result (apply f (:args record))]
      (log/debug ::change-user-password :result result)
      (if-not (failure? result)
        nil
        result
        )
      )))

(defn inject-username
  "UI-Wrapper"
  [f]
  (fn [{:keys [args token] :as record}]
    (log/debug ::inject-username :record record)
    (attempt-all [
                  user (or (auth/get-user-by-token token) (fail :no-user))
                  result (f (assoc (first args) :username (:username user)))]
                 result
      )))

(defn save-user
  "UI-Wrapper for save-user command"
  [f]
  (fn [record]
    (log/debug ::save-user :record record)
    (let [result (apply f (:args record))]
      (log/debug ::save-user :result result)
      (if-not (failure? result)
        nil
        result
        )
      )))

(defn save-ct
  "UI-Wrapper for save-ct command"
  [f]
  (fn [record]
    (log/debug ::save-ct :record record)
    (let [result (apply f (:args record))]
      (log/debug ::save-ct :result result)
      (if-not (failure? result)
        nil
        result
        )
      )))

(defn save-db
  "UI-Wrapper for save-db command"
  [f]
  (fn [record]
    (log/debug ::save-db :record record)
    (let [result (apply f (:args record))]
      (log/debug ::save-db :result result)
      (if-not (failure? result)
        (let [dbs (db/find "databases" {})]
          {:type :broadcast
           :result {:transact {:db/add [[:databases] dbs ]}}})
        result
        )
      )))

(defmulti -handle-command-result (comp :type :result))

(defn handle-command-result
  [{:keys [type result] :as return-data}]
  (-handle-command-result return-data))
