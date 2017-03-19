(ns cmskern.ui.core
  (:import org.bson.types.ObjectId)
  (:require
   [bidi.ring :as bidi ]
   [taoensso.timbre :as log]
   [ring.middleware.keyword-params :refer [wrap-keyword-params]]
   [ring.middleware.params :refer [wrap-params]]
   [ring.middleware.multipart-params :refer [wrap-multipart-params]]
   [ring.middleware.anti-forgery :refer [wrap-anti-forgery]]
   [ring.middleware.session :refer [wrap-session]]
   [ring.util.response :refer [file-response resource-response
                               status content-type]]
   [clojure.java.io :as io]
   [conqueress.failure :refer [fail failure?]]

   [conqueress.monads :refer [attempt-all]]
   [conqueress.core :as cqrs]

   [cmskern.auth :as auth]
   [cmskern.db :as db]
   [cmskern.ui.websockets :as ws]
   [cmskern.ui.middleware :as mw]
   [cmskern.ui.commands :as cmd]
   [cmskern.ui.binary :as bin]
   ))


(defmacro handler-fn
  ([& body]
   `(fn [~'event] ~@body nil)))  ;; force return nil

(defn http-query-handler
  [ctx]
  (attempt-all
   [data (cqrs/query-handler ctx)
    _ (log/debug ::http-query-handler ctx)
    ]
   {:status 200 :body data}
   #(do (log/error (.errors %)) {:status 400
                                 :body {:error {:form (.errors %)}
      }})))

(defn http-command-handler
  [ctx]
  (attempt-all
   [data (cmd/handle-command-result (cqrs/command-handler (:params ctx)))]
   {:status 200 :body data}
   #(do
      (log/error (.errors %))
      {:status 400
       :body {:error {:form (.errors %)}}}
      )))

(defn img-formats
  [f]
  (case f
    :_orig :orig
    :default [391 nil 391 :fix]
    :clogo [150 :fix 150 nil]
    :mini [50 :fix 50 :fix]
    :preview-thumb [80 :fix 80 :fix]
    :orig
    ))

(defn binary-handler
  ""
  [{:keys [route-params]}]
  (let [{:keys [dbid format ext ref]} route-params
        attr (img-formats (keyword format))
        asset (bin/get-binary dbid ref attr)]
    (content-type
     {:body (second asset)}
     (first asset)
     )))

(defn wrap-image
  ""
  [hndl]
  (fn [req]
    (let [{:keys [dbid format ext ref]} (:route-params req)
          img (db/get dbid "content" {:_id (Integer/parseInt ref)})
          assetref (-> img :data :asset :idref)
          _ (log/debug ::wrap-image img assetref)
          ]
      (hndl (assoc-in req [:route-params :ref] assetref))
      )))

(defn wrap-default-mw
  [hndl]
  (-> hndl
      mw/request-logging
      wrap-keyword-params
      wrap-multipart-params
      wrap-params
      mw/wrap-formats-params
      mw/wrap-formats-response
      ))

(defn debugging-mw
  [hndl]
  (fn [req]
    (log/debug ::debug :request)
    (clojure.pprint/pprint req)
    (hndl req)))

(defn wrap-ws
  [h]
  (-> h
      ;debugging-mw
      wrap-keyword-params
      wrap-params
      wrap-anti-forgery
      wrap-session
      ))
(defn devcards-handler
  [req]
  (resource-response "devcards.html" {:root "public"})
  )
(defn index-handler
  [req]
  (resource-response "index.html" {:root "public"})
  )

(defn asset-handler
  [{:keys [route-params params body headers] :as req}]
  (attempt-all [
                dbid (or (:dbid route-params) (fail :no-db))
                f (or (:upload params) (fail :no-file))
                uid (or (auth/check-token (get headers "authorization")) (fail :token-invalid))
                user (or (db/get "users" {:_id (ObjectId. uid)}) (fail :no-user))
                result (db/gridfs-store-file dbid f)
                ]
               result
               #(log/error ::asset-handler :failure %))
  )
(defn cache-imgs
  [handler]
  (fn [{:keys [route-params] :as req}]
    (let [{:keys [dbid format ext ref]} route-params
          target-dir (io/file (clojure.core/format "%s/%s/%s" "resources/public/img-cache" dbid format))
          target-file (io/file (clojure.core/format "%s/%s.%s" (.getAbsoluteFile target-dir) ref ext))
          bin (handler req)]
      ;(log/debug ::target-file target-file)
      ;(log/debug ::target-bin bin)

      (.mkdirs target-dir)
      (io/copy (:body bin) target-file)
      (content-type
       {:body target-file}
       "images/png"
       )
      )))

(defn res-route [ws-get-fn ws-post-fn]
  ["/" [
        ["_devcards" (wrap-default-mw devcards-handler)]
        ["" (wrap-default-mw index-handler)]
        ["js" (bidi/resources {:prefix "public/js"})]
        ["css" (bidi/resources {:prefix "public/css"})]
        ["fonts" (bidi/resources {:prefix "public/fonts"})]
        ["cljs" (bidi/resources {:prefix "public/cljs"})]
        ["_asset/" {[:dbid] {:post {"" (wrap-default-mw asset-handler)}}}]
        ["c" (wrap-default-mw http-command-handler)]
        ["q" (wrap-default-mw http-query-handler)]
        ["bin" (bidi/->ResourcesMaybe {:prefix "public/img-cache"})]
        ["img" (bidi/->ResourcesMaybe {:prefix "public/img-cache"})]
        ["bin/" {[:dbid "/" :format "/" :ref "." :ext] (cache-imgs binary-handler)}]
        ["img/" {[:dbid "/" :format "/" :ref "." :ext] (cache-imgs (wrap-image binary-handler))}]
        ["chsk" {:get {"" (wrap-ws ws-get-fn)}
                 :post {"" (wrap-ws ws-post-fn)}
                 }]
        [true (wrap-default-mw index-handler)]
        ]
   ])

(defn app-handler [{:keys [ajax-get-or-ws-handshake-fn ajax-post-fn]}]
  (bidi/make-handler (res-route ajax-get-or-ws-handshake-fn ajax-post-fn)))

(defn app [ws-connection]
  (app-handler ws-connection)
  )
