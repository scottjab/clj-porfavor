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

(def config (json/read-json (slurp "./porfavor-config.json")))


(def ldap-connection (ldap/connect {:host (config :server)
                                    :port 636
                                    :ssl? true}))

(def base (config :base))

(defn search-ldap [filter]
  (json/write-str (ldap/search ldap-connection base {
                                                      :filter filter
                                                      }))
  )

(defn search-for-group [group]
  (search-ldap (str "(&(|(objectClass=posixGroup)(objectClass=groupOfNames))(cn="
                    group
                    "))")))

(defn search-for-user [username]
  (search-ldap (str "(&(objectClass=inetOrgPerson)(uid=" username "))"))
  )

(defn search-for-irchandle [irchandle]
  ;; This method requires schema extentions
  (search-ldap (str "(&(objectClass=inetOrgPerson)(ircHandle="
                    irchandle
                    "))")))

(defroutes app
           (ANY "/group/:group" [group] (resource :allowed-methods [:get]
                                                  :available-media-types ["application/json"]
                                                  :handle-ok (search-for-group group)))
           (ANY "/user/:username" [username] (resource :allowed-methods [:get]
                                                       :available-media-types ["application/json"]
                                                       :handle-ok (search-for-user username)))
           (ANY "/irc/:irchandle" [irchandle] (resource :allowed-methods [:get]
                                                        :available-media-types ["application/json"]
                                                        :handle-ok (search-for-irchandle irchandle))))

(def handler
  (-> app
      (wrap-params)))

(run-jetty #'handler {:port 3001})