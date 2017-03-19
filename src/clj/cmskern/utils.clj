(ns cmskern.utils
  (:import org.bson.types.ObjectId)
  (:import [org.joda.time DateTime ReadableInstant])
  (:require
   [taoensso.timbre :as log]
   [clj-time.core :as time]
   [clj-time.coerce :as timeco]
   [clj-time.format :as timef]
   [schema.core :as s]
   [cognitect.transit :as transit]

   ))

(s/defn timeformat [f :- s/Str t :- s/Any]
  (timef/unparse (timef/formatter f) t)
  )

(s/defn tformat [t :- s/Any f :- s/Str]
  (timef/unparse (timef/formatter f) t)
  )

(s/defn str->time [tstr :- s/Str f :- s/Str]
  (timef/parse (timef/formatter f) tstr)
  )

(s/defn epoch->string :- s/Str
  [tint :- s/Any f :- s/Str]
  (timeformat f (timeco/from-long (* 1000 tint)))
  )

(defn datum->time [d]
  (timeco/from-long (* 1000 (:dateInt d))))

(defn ->basic-date-string [d]
  (timef/unparse (:basic-date timef/formatters) d))

(defn uuid [] (str (java.util.UUID/randomUUID)))
(defn long-now [] (timeco/to-long (time/now)))


(defn shadow-user
  [emp]
  (dissoc emp :password :passwordHash)
  )

(def joda-time-writer
  (transit/write-handler
   (constantly "m")
   (fn [v] (-> ^ReadableInstant v .getMillis))
   (fn [v] (-> ^ReadableInstant v .getMillis .toString))))

(def bson-objectid-write-handler (transit/write-handler "bson-objectid" (fn [o] (str o))))

(def bson-objectid-read-handler (transit/read-handler (fn [r] (ObjectId. r))))

(defn encode-transit [message]
  (let [out (java.io.ByteArrayOutputStream. 4096)
        writer (transit/writer out :json-verbose {:handlers {org.bson.types.ObjectId bson-objectid-write-handler
                                                             org.joda.time.DateTime joda-time-writer}})
        _ (transit/write writer message)
        out-str (.toString out "UTF-8")]
    out-str
    ))

(defn decode-transit [message]
  (let [in message
        reader (transit/reader in :json-verbose
                               {:handlers {"bson-objectid" bson-objectid-read-handler}})]
    (transit/read reader)))
