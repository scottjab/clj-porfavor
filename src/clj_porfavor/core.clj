(ns clj-porfavor.core
  (:gen-class))

(ns clj-porfavor.core
  (:require [clj-ldap.client :as ldap]
            [liberator.core :refer [resource defresource]]
            [ring.adapter.jetty :refer [run-jetty]]
            [ring.middleware.params :refer [wrap-params]]
            [compojure.core :refer [defroutes ANY]]
            [clojure.data.json :as json]
            ))


(def ldap-connection (ldap/connect {:host "ldap.server.com"
                                    :port 636
                                    :ssl? true}))

(defn base [] "dc=domain,dc=com")

(defn search-for-group [group]
  (ldap/search ldap-connection (base) {:filter (str "(&(|(objectClass=posixGroup)(objectClass=groupOfNames))(cn="
                                                    group
                                                    "))")}))

(defn search-for-user [username]
  (ldap/search ldap-connection (base) {:filter (str "(&(objectClass=inetOrgPerson)(uid=" username "))")})
  )

(defroutes app
           (ANY "/group/:group" [group] (resource :allowed-methods [:get]
                                                  :available-media-types ["application/json"]
                                                  :handle-ok (json/write-str (search-for-group group))))
           (ANY "/user/:username" [username] (resource :allowed-methods [:get]
                                                       :available-media-types ["application/json"]
                                                       :handle-ok (json/write-str (search-for-user username)))))

(def handler
  (-> app
      (wrap-params)))

(run-jetty #'handler {:port 3001})