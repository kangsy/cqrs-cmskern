(ns cmskern.config)

(def debug? true)
(def query-endpoint "/q")
(def websocket-uri (str "ws://" (.-location.host js/window) "/ws"))
(def default-timeout 2000)
(def default-user-schema
  {:schema {:id "user"
            :title "User"
            :type "object"
            :required ["email"],
            :properties {
                         :username {:type  "string"
                                :title "Username"}
                         :email {:type "string"
                                :title "E-Mail"}
                         :roles {:type "array"
                                 :title "Roles"
                                 :items {
                                         :type "string"
                                         :enum ["admin" "crmadmin"]
                                         }
                                 :uniqueItems true
                                 }
                         :databases {:type "array"
                                     :items {
                                             :type "string"
                                             }
                                     }
                         }
            }
   :ui-schema {
               :roles {
                       "ui:widget" "checkboxes"
                       "ui:options" {"inline" true}
                       }
               }
})

(def default-db-schema
  {:schema {:id "database"
            :title "Database"
    :type "object"
    :required ["name", "dbid", "gridfs"],
    :properties {
                 :name {:type  "string"
                        :title "Name"}
                 :dbid {:type "string"
                        :title "DBID"}
                 :gridfs {:type "string"
                          :title "GridFs-Collection"}
              }}
   :ui-schema {}
   :fields {}})

(def default-ct-schema
  {:schema {
            :title "Content-Type"
            :type "object"
            :required ["name"]
            :properties {
                         :name {:type "string"}
                         :displayName {:type "string"}
                         :jsonSchema {:type "string"}
                         :uiSchema {:type "string"}
                         :description {:type "string"}
                         }}
   :ui-schema {
               :uiSchema
               {
                "ui:widget" "textarea"
                "classNames" "large" 
                
                }
               :jsonSchema
               {
                "ui:widget" "textarea"
                "classNames" "large" 
                }
               }})
