(ns clj-porfavor.ldapfilter)

(defn attribute [attribute value]
  (format "(%s=%s)"
          attribute value))

(defn ldap-and [& attributes]
  (format "(&%s)"
          (clojure.string/join " " attributes)))
