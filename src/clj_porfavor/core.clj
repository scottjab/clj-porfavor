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

(ns clj-porfavor.core
  (:use [clojure.tools.logging :only (info error)]))

(def config (json/read-json (slurp "./porfavor-config.json")))


(def ldap-connection (ldap/connect {:host (config :server)
                                    :port (config :port)
                                    :ssl? (config :ssl)}))

(def base (config :base))

(defn search [filter]
  (try
    (info (format "Search %s"
                  filter))
    (ldap/search ldap-connection base {:filter filter})
    (catch Exception ex
      (error ex "Something bad happened."))))


(defn search-ldap [filter]
  (json/write-str (search filter)))

(defn search-for-group [group]
  (search-ldap (format "(&(|(objectClass=posixGroup)(objectClass=groupOfNames))(cn=%s))"
                       group)))

(defn search-for-user [username]
  (search-ldap (format "(&(objectClass=inetOrgPerson)(uid=%s))"
                       username))
  )

(defn search-for-irchandle [irchandle]
  ;; This method requires schema extentions
  (search-ldap (format "(&(objectClass=inetOrgPerson)(ircHandle=%s))"
                       irchandle)))

(defn search-for-mail [mail]
  (search-ldap (format "(&(objectClass=inetOrgPerson)(mail=%s))"
                       mail)))

(defn search-for-github [ghuid]
  ;; This function requires schema exentions
  (search-ldap (format "(&(objectClass=inetOrgPerson)(githubUID=%s))"
                       ghuid)))

(defn groups-for-user [username]
  (search-ldap (format "(|(memberUid=%s)(member=%s))"
                       username
                       (get (first (search (format "(&(objectClass=inetOrgPerson)(uid=%s))"
                                                   username))) :dn))))


  (defn whois [name]
  (search-ldap (format "(|(uid=%s)(ircHandle=%s)(gecos=%s)(githubUid=%s))"
                       name
                       name
                       name
                       name)))

(defn uid [uid]
  (search-ldap (format "(uidNumber=%s)"
                       uid)))

(defn gid [gid]
  (search-ldap (format "(gidNumber=%s"
                       gid)))

(defn get-resource [func] (resource :allowed-methods [:get]
                                    :available-media-types ["application/json"]
                                    :handle-ok func))

(defroutes app
           (ANY "/group/:group" [group] (get-resource (search-for-group group)))
           (ANY "/user/:username" [username] (get-resource (search-for-user username)))
           (ANY "/mail/:mail" [mail] (get-resource (search-for-mail mail)))
           (ANY "/irc/:irchandle" [irchandle] (get-resource (search-for-irchandle irchandle)))
           (ANY "/github/:ghuid" [ghuid] (get-resource (search-for-github ghuid)))
           (ANY "/userGroups/:username" [username] (get-resource (groups-for-user username)))
           (ANY "/whois/:name" [name] (get-resource (whois name)))
           (ANY "/uid/:uid" [uid] (get-resource (uid uid)))
           (ANY "/gid/:gid" [gid] (get-resource (gid gid))))

(def handler
  (-> app
      (wrap-params)))

(run-jetty #'handler {:port (config :webserver_port)})