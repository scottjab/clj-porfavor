(ns clj-porfavor.ldapfilter)
;; Some helper methods for building ldap filters


(defn l= [attribute value]
  (format "(%s=%s)"
          attribute value))

(defn l& [& attributes]
  (format "(&%s)"
          (clojure.string/join "" attributes)))

(defn l| [& attributes]
  (format "(|%s)"
          (clojure.string/join "" attributes)))

(defn l! [& attributes]
  (format "(!%s)"
          (clojure.string/join "" attributes)))

(def person (l= "objectClass" "inetOrgPerson"))
(def posixGroup (l= "objectClass" "posixGroup"))
(def groupOfNames (l= "objectClass" "groupOfNames"))
(def anyGroup (l| posixGroup groupOfNames))