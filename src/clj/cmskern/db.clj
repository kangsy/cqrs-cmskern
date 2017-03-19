(ns cmskern.db
  (:require
   [mount.core :refer [defstate]]
   [taoensso.timbre :as log]
   [monger.core :as mg]
   [monger.conversion :as mconv]
   [monger.collection :as mc]
   [monger.query :as mq]
   [monger.operators :refer :all]
   [monger.gridfs :as gfs]
   [monger.result :as mr]
   ))

;; TODO
(def dbname "cmskern")


(def gridfs (atom {}))
(def dbs (atom {}))

(defstate conn
  :start (do (reset! dbs {})
             (reset! gridfs {})
             (mg/connect))
  :stop (do (reset! dbs nil)
            (reset! gridfs nil)
            (mg/disconnect conn)
            ))

(defn get-or-create-db
  [dbid]
  (let [dbc (get @dbs dbid)
        c (when-not dbc (mg/get-db conn dbid))]
    (if-not dbc
      (do (swap! dbs assoc dbid c)
          c)
      dbc)
    ))

(defn get-or-create-gridfs
  [dbid]
  (let [dbc (get @gridfs dbid)
        c (when-not dbc (mg/get-gridfs conn dbid))]
    (if-not dbc
      (do (swap! gridfs assoc dbid c)
          c)
      dbc)
    ))

(defmacro with-db [dbid body]
  `(let [db# (get-or-create-db ~dbid)]
     (when db#
       (do 
         (-> db#
             ~body)))))

(defn gridfs-get
  ""
  [dbid q]
  (log/debug ::gridfs-get dbid q)
  (let [ fs (get-or-create-gridfs dbid)
        res (gfs/find-one fs q)
        ]
    (log/debug ::gridfs-get :result (doall (gfs/files-as-maps fs)) fs res)
    res)
  )

(defn gridfs-query
  "Returns DBCursor. Use .toArray or .count"
  [dbid {:keys [find sort limit skip] :as qmap}]
  (-> (get-or-create-gridfs dbid)
      (.getFileList (mconv/to-db-object find) (mconv/to-db-object sort))
      (.skip skip)
      (.limit limit)
      )
  )

(defn gridfs-query-count
  "der vollständigkeitshalber"
  [dbid qmap]
  (let [cursor (gridfs-query dbid qmap)]
    (.count cursor)
    )
  )
(defn gridfs-query-files
  ""
  [dbid qmap]
  (let [cursor (gridfs-query dbid qmap)]
    (map #(mconv/from-db-object % true) (.toArray cursor))
    )
  )

(defn gridfs-query-total-files
  "Returns DBCursor. Use .toArray or .count"
  [dbid qmap]
  (let [cursor (gridfs-query dbid qmap)]
    {:count (.count cursor)
     :result (map #(mconv/from-db-object % true) (.toArray cursor))}
    ))

(defn gridfs-store-file
  ""
  [dbid f]
  (gfs/store-file (gfs/make-input-file (get-or-create-gridfs dbid) (:tempfile f))
                  (gfs/filename (:filename f))
                  (gfs/content-type (:content-type f))))

(defn gridfs-remove
  ""
  [dbid q]
  (gfs/remove (get-or-create-gridfs dbid) q))

(defn execute-query
  ""
  [dbid col query]
  ;;TODO
  [{:_id 123 :name "db-1"}{:_id 223 :name "db-2"}])

(defn count
  ""
  ([entity q]
   (get dbname entity q))
  ([dbid entity q]
   (log/debug ::count entity  q)
   (with-db
     dbid
     (mc/count entity q))))

(defn get
  ""
  ([entity q]
   (get dbname entity q))
  ([dbid entity q]
   (log/debug ::get entity  q)
   (with-db
     dbid
     (mc/find-one-as-map entity q))))

(defn query
  ""
  ([entity {:keys [fields find sort limit skip] :as qmap}]
   (query dbname entity qmap))
  ([dbid entity {:keys [fields find sort limit skip] :as qmap} ]
   (log/debug ::query dbid entity qmap)
   (with-db
     dbid
     (mq/with-collection
       entity
       (mq/find find)
       (mq/fields fields)
       (mq/sort sort)
       (mq/limit limit)
       (mq/skip skip)
       ))))

(defn find
  ""
  ([entity q]
   (find dbname entity q))
  ([dbid entity q]
   (log/debug ::find dbid entity q)
   (with-db
     dbid
     (mc/find-maps entity q))))

(defn insert
  ""
  ([entity q]
   (insert dbname entity q))
  ([dbid entity q]
   (log/debug ::insert dbid entity q)
   (mr/acknowledged?
    (with-db
      dbid
      (mc/insert entity q)))))

(defn upsert
  ""
  ([entity q change]
   (upsert dbname entity q change))
  ([dbid entity q change]
   (log/debug ::upsert dbid entity q change)
   (mr/acknowledged?
    (with-db
      dbid
      (mc/update entity q change {:upsert true :multi false})))
     ))

(defn save-and-return
  ""
  ([entity q]
   (save-and-return dbname entity q))
  ([dbid entity q]
   (log/debug ::save-and-return dbid entity q)
   (with-db
     dbid
     (mc/save-and-return entity q)))
     )

(defn modify-and-return
  ""
  ([entity q change]
   (modify-and-return dbname entity q change))
  ([dbid entity q change]
   (log/debug ::modify-and-return dbid entity q)
   (with-db
     dbid
     (mc/find-and-modify entity q change {:return-new true})))
     )

(defn update
  ""
  ([entity q change]
   (update dbname entity q change {:multi false}))
  ([dbid entity q change]
   (update dbid entity q change {:multi false}))
  ([dbid entity q change opts]
   (log/debug ::update dbid entity q change opts)
      (mr/acknowledged?
       (with-db
         dbid
         (mc/update entity q change opts)))
      ))

(defn delete
  ""
  ([entity q]
   (delete dbname entity q))
  ([dbid entity q]
   (log/debug ::delete dbid entity q)
   (mr/acknowledged?
    (with-db
      dbid
      (mc/remove entity q))))
     )

(defn get-new-id
  [dbid col]
  (let [ret
        (modify-and-return
         dbid "ids" {:_id col} {"$inc" {:value 1}}
         )]
    (or ret (save-and-return dbid "ids" {:_id col :value 1}))))
