(defproject friend-form-login "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [compojure "1.1.3"]
                 [com.cemerick/friend "0.1.2"]
                 [ring "1.1.3"]
                 [com.draines/postal "1.9.0"]]
  :plugins [[lein-ring "0.7.5"]
            [lein-midje "2.0.0-SNAPSHOT"]
            [codox "0.6.1"]]
  :ring {:handler friend-form-login.handler/app}
  :profiles
  {:dev {:dependencies [[ring-mock "0.1.3"]
                        [midje "1.5.0-SNAPSHOT"]
                        [com.stuartsierra/lazytest "1.2.3"]]}}
  :repositories {"stuart" "http://stuartsierra.com/maven2"})  ;; For lazytest
