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

(defn search-for-mail [mail]
  ;; This function requires schema extentions
  (search-ldap (str "(&(objectClass=inetOrgPerson)(mail="
                    mail
                    "))")))

(defn search-for-github [ghuid]
  ;; This function requires schema exentions
  (search-ldap (str "(&(objectClass=inetOrgPerson)(githubUID="
                    ghuid
                    "))"))
  )

(defn get-resource [func] (resource :allowed-methods [:get]
                                 :available-media-types ["application/json"]
                                 :handle-ok func))

(defroutes app
           (ANY "/group/:group" [group] (get-resource (search-for-group group)))
           (ANY "/user/:username" [username] (get-resource (search-for-user username)))
           (ANY "/mail/:mail" [mail] (get-resource (search-for-mail mail)))
           (ANY "/irc/:irchandle" [irchandle] (get-resource (search-for-irchandle irchandle)))
           (ANY "/github/:ghuid" [ghuid] (get-resource (search-for-github ghuid))))

(def handler
  (-> app
      (wrap-params)))

(run-jetty #'handler {:port (config :webserver_port)})