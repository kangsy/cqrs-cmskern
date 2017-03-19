(ns cmskern.routes
  (:require [bidi.bidi :as bidi]))

(def cmskern-routes
  ["/"
   {"" :index-page
    "index.html" :index-page
    "login" :login-page
    "admin" {"" :admin-page
             "/" {"" :admin-page
                  "users" {"" :admin-users-page
                           "/" {"" :admin-users-page
                                ":new" (bidi.bidi/tag :admin-user-edit-page :admin-user-new)
                                [:uid] {"" (bidi.bidi/tag :admin-user-edit-page :admin-user-edit)
                                        "/" {"" (bidi.bidi/tag :admin-user-edit-page :admin-user-edit)
                                             }
                                        }}
                           }
                  "dbs" {"" :admin-dbs-page
                         "/" {"" :admin-dbs-page
                              ":new" (bidi.bidi/tag :admin-db-edit-page :admin-db-new)
                              [:dbid] {"" (bidi.bidi/tag :admin-db-edit-page :admin-db-edit)
                                       "/" {"" (bidi.bidi/tag :admin-db-edit-page :admin-db-edit)
                                            ":new" (bidi.bidi/tag :admin-ct-edit-page :admin-ct-new)
                                            [:ctid] (bidi.bidi/tag :admin-ct-edit-page :admin-ct-edit)
                                            }
                                       }}
                         }}}
    [:dbid] {"" :db-index-page
             "/" {"" :db-index-page
                  [:ctid] {"" :ct-index-page
                           "/" {
                                ":new" (bidi.bidi/tag :content-edit-page :content-new)
                                [:cid] (bidi.bidi/tag :content-edit-page :content-edit)
                                }
                           }}}
    true :four-o-four}])


(def make-path (partial bidi/path-for cmskern-routes))
