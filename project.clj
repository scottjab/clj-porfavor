(defproject clj-porfavor "0.1.0-SNAPSHOT"
  :description "A simple JSON Rest interface to read only ldap."
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [liberator "0.11.0"]
                 [org.clojars.pntblnk/clj-ldap "0.0.7"]
                 [org.clojure/clojure "1.4.0"]
                 [compojure "1.1.3"]
                 [ring/ring-core "1.2.1"]
                 [ring/ring-jetty-adapter "1.1.0"]]
  :main ^:skip-aot clj-porfavor.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
