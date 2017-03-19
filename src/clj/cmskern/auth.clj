(ns cmskern.auth
  (:import org.bson.types.ObjectId)
  (:require [taoensso.timbre :as log]
            [clj-jwt.core  :as jwt]
            [clj-jwt.key   :as key ]
            [clj-time.core :as time]
            [taoensso.tufte :as tufte :refer (defnp p profiled profile)]
            [crypto.password.bcrypt :as pw]
            [clojure.string :as string]

            [conqueress.monads :refer [attempt-all]]
            [conqueress.failure :refer [fail]]

            [cmskern.db :as db]))



(def +INVALIDED+ "INVALIDED_")
(def rsa-prv-key (key/private-key "cmskern.rsa"))
(def rsa-public-key (key/public-key "cmskern-pub.key"))

(defn generate-jwt [claim]
  (p ::generate-jwt
     (-> claim jwt/jwt (jwt/sign :RS256 rsa-prv-key) jwt/to-str)
     ))
(defn generate-new-token [id]
  (p ::generate-new-token
     (generate-jwt
      {:id id
       :exp (time/plus (time/now) (time/days 1))
       :iat (time/now)})
     ))

(defn get-user-by-token
  [tok-string]
  (let [token (try (jwt/str->jwt tok-string)
                   (catch Exception e 
                     (log/error ::get-user-by-token (.getMessage e))
                     nil
                     ))]
    (when token
      (log/debug ::get-user-by-token (:claims token))
      (db/get "users" {:_id (ObjectId. (:id (:claims token)))})
           )
    ))

;; ;;;;;;;;;;
(defn verify-token
  "returns bool"
  [tok-string]
  (p ::verify-token
     (let [token (try (jwt/str->jwt tok-string)
                      (catch Exception e 
                        (log/error "check-token error" (.getMessage e) tok-string)
                        nil
                        ))]
       (when token
         (and (jwt/verify token :RS256 rsa-public-key)
              token
              ))
       )))
;; ;;;;;;;

(defn check-token
  "returns id in claim or nil"
  [tok-string]
  (p ::check-token
     (when tok-string
       (when-let [token (verify-token (string/replace tok-string #"Bearer " ""))]
         (:id (:claims token))))
     ))

(defn auth-by-id-and-password
  "returns user or failure"
  [email passwd]
  (p ::auth-by-id-and-password
     (attempt-all
      [_ (or (and email passwd) (fail ::args-not-valid))
       user (or (db/get "users" {:email email}) (fail ::user-not-found))
       ok? (or (try (pw/check passwd (:passwordHash user))
                    (catch Exception e
                      (log/error ::auth-by-id-and-password :exception (.getMessage e))
                      (fail :pw-mismatch)
                      )) )
       ]
      user
      )
     )
  )
