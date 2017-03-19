(ns cmskern.ui.middleware
  (:require
   [taoensso.timbre :as log]
   [ring.middleware.format-params :refer [wrap-restful-params]]
   [ring.middleware.format-response :refer [wrap-restful-response]]

   [cmskern.utils :as u]
))

(defn wrap-exception [f]
  (fn [request]
    (try (f request)
         (catch Exception e
           (log/debug "Catched Exception. Request" request e (type e))
           {:status 404
            :body {:success false
                   :errors {:form (.getMessage e)}
                   :status {}
                   }}))))

#_(defn keywordize-middleware [handler]
  (fn [req]
    (handler
     (update-in req [:query-params] keywordize-map))))

(defn wrap-cors-response [handler]
  (fn [req]
    (let [response (handler req)]
      (-> response
          (assoc-in [:headers "X-Frame-Options"] "SAMEORIGIN")
          (assoc-in [:headers "Access-Control-Allow-Origin"] "*")
          )
      ))
  )

(defn test-constant-response [handler]
  (fn [req]
    {:status 400, :body "{\"success\":false,\"status\":null,\"errors\":\"a\"}"})
  )

(defn response-debugger [handler]
  (fn [req]
    (let [response (handler req)]
      (log/debug "resp debugger" response)
      response
      )
    )
  )
(defn wrap-formats-params [handler]
  (wrap-restful-params handler {:decoder u/decode-transit
                                :formats [:transit-json :transit-msgpack :json-kw]}))

(defn wrap-formats-response [handler]
  (wrap-restful-response handler {:format-options {:transit-json 
                                                   {:handlers
                                                    {org.joda.time.DateTime u/joda-time-writer
                                                     org.bson.types.ObjectId u/bson-objectid-write-handler}}}
                                  :formats [:transit-json :transit-msgpack :json-kw ]}))


(defn request-logging [handler]
  (fn [req]
    (let [start (System/currentTimeMillis)
          reqstr (format "%s %s %s:%s%s" (:remote-addr req) (:request-method req) (:server-name req) (:server-port req) (:uri req))]
      (log/debug (with-out-str (clojure.pprint/pprint req)))
      (log/info reqstr)
        (try
          (let [response (handler req)
                status (:status response)
                finish (System/currentTimeMillis)
                total (- finish start)]

            (clojure.pprint/pprint response)
            (log/info (format "Finished %s %sms - %s" status total reqstr))
            response)
          (catch Throwable t
            (let [finish (System/currentTimeMillis)
                  total (- finish start)]

              (log/error "Finished with exception in ms" total)
              )
            (throw t)))
      )))
