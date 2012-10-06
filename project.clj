(defproject friend-form-login "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [compojure "1.1.3"]
                 [com.cemerick/friend "0.1.2"]
                 [ring "1.1.3"]]
  :plugins [[lein-ring "0.7.5"]]
  :ring {:handler friend-form-login.handler/app}
  :profiles
  {:dev {:dependencies [[ring-mock "0.1.3"]]}})
