(ns cmskern.content
  (:require 
   [taoensso.timbre :as log]
   [clj-time.coerce :as timeco]
   [clj-time.core :as time]

   [conqueress.monads :refer [attempt-all]]
   [conqueress.failure :refer [fail]]

   [cmskern.db :as db]
   ))

(defn save-content
  ""
  [{:keys [dbid ctid cid data username]}]
  (log/debug ::save-content dbid ctid cid data username)
  (let [now (timeco/to-long (time/now))]
    (if cid
      ;; create version
      ;; update meta
      (let [vid (db/get-new-id dbid "versions")
            old (db/get dbid "content" {:_id cid})
            history {:subject :version-created
                     :by username
                     :ts now
                     :data {:version (:value vid)}}
            ]
        
        (db/insert dbid "versions" {:_id (:value vid)
                                    :_idref cid
                                    :_type ctid
                                    :_modifier username
                                    :_modified now
                                    :data (:data old)
                                    })
        (db/modify-and-return dbid "content" {:_id cid} {"$set" {:_modifier username
                                                                 :_modified now
                                                                 :_type ctid
                                                                 :data data}
                                                         "$push" {:history history}})
        )
      (let [id (db/get-new-id dbid "content")
            created {:_id (:value id)
                     :_type ctid
                     :_status 50
                     :_creator username
                     :_created now
                     :_modifier username
                     :_modified now
                     :data data
                     :history [{:subject :created :by username}]
                     }]
        (db/save-and-return dbid "content" created)
        )
      ))
  )

(defn publish-content
  ""
  [{:keys [dbid ctid cid data username]}]
  (log/debug ::publish-content dbid ctid cid data username)
  (let [now (timeco/to-long (time/now))]
    (if cid
      (let [
            history {:subject :publish
                     :by username
                     :ts now
                     }
            ]
        (db/modify-and-return dbid
                              "content"
                              {:_id cid}
                              {"$set" {:_modifier username
                                       :_modified now
                                       :_type ctid
                                       :_status 200
                                       }
                               "$push" {:history history}})
        )
      (fail :no-content-id)
      ))
  )

(defn unpublish-content
  ""
  [{:keys [dbid ctid cid data username]}]
  (log/debug ::unpublish-content dbid ctid cid data username)
  (let [now (timeco/to-long (time/now))]
    (if cid
      (let [
            history {:subject :un-publish
                     :by username
                     :ts now
                     }
            ]
        (db/modify-and-return dbid
                              "content"
                              {:_id cid}
                              {"$set" {:_modifier username
                                       :_modified now
                                       :_type ctid
                                       :_status 100
                                       }
                               "$push" {:history history}})
        )
      (fail :no-content-id)
      ))
  )

(defn delete-content
  ""
  [{:keys [dbid ctid cid data username]}]
  (log/debug ::unpublish-content dbid ctid cid data username)
  (let [now (timeco/to-long (time/now))]
    (if cid
      (let [
            history {:subject :delete
                     :by username
                     :ts now
                     }
            ]
        (db/modify-and-return dbid
                              "content"
                              {:_id cid}
                              {"$set" {:_modifier username
                                       :_modified now
                                       :_type ctid
                                       :_status 20
                                       }
                               "$push" {:history history}})
        )
      (fail :no-content-id)
      ))
  )
