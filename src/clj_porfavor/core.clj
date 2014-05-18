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
  (:use [clojure.tools.logging :only (info error)]
        [clj-porfavor.ldapfilter :only (anyGroup person l= l& l! l|)]))

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
  (search-ldap (l& anyGroup (l= "cn" group))))

(defn search-for-user [username]
  (search-ldap (l& person (l= "uid" username))))

(defn search-for-irchandle [irchandle]
  ;; This method requires schema extentions
  (search-ldap (l& person (l= "ircHandle" irchandle))))

(defn search-for-mail [mail]
  (search-ldap (l& person (l= "mail" mail))))

(defn search-for-github [ghuid]
  ;; This function requires schema exentions
  (search-ldap (l& person (l= "githubUID" ghuid))))

(defn groups-for-user [username]
  (search-ldap (l| (l= "memberUid" username)
                   (l= "member" (get (first
                                       (search
                                         (l& person (l= "uid" username)))) :dn)))))


(defn whois [name]
  (search-ldap (l| (l= "uid" name)
                   (l= "ircHandle" name)
                   (l= "gecos" name)
                   (l= "githubUID" name))))

(defn uid [uid]
  (search-ldap (l= "uidNumber" uid)))

(defn gid [gid]
  (search-ldap (l= "gidNumber" gid)))

(defn search-for-any [attribute value]
  (search-ldap (l= attribute value)))

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
           (ANY "/gid/:gid" [gid] (get-resource (gid gid)))
           (ANY "/any/:attribute/:value" [attribute value] (get-resource (search-for-any attribute value))))

(def handler
  (-> app
      (wrap-params)))

(run-jetty #'handler {:port (config :webserver_port)})