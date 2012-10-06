(ns friend-form-login.handler
  (:use compojure.core)
  (:require [compojure.handler :as handler]
            [compojure.route :as route]
            [cemerick.friend :as friend]
            (cemerick.friend [workflows :as workflows]
                             [credentials :as creds])))

(def users {"admin" {:username "admin"
                    :password (creds/hash-bcrypt "password")
                    :roles #{::admin}}
            "dave" {:username "dave"
                    :password (creds/hash-bcrypt "password")
                    :roles #{::user}}})

(derive ::admin ::user)

(defroutes app-routes
  (GET "/" [] "Hello World")
  (GET "/authorized" request
       (friend/authorize #{::user} "This page can only be seen by authenticated users."))
  (GET "/admin" request
       (friend/authorize #{::admin} "This page can only be seen by administrators."))
  (GET "/login" [] (ring.util.response/file-response "login.html" {:root "resources"}))
  (friend/logout (ANY "/logout" request (ring.util.response/redirect "/")))
  (route/not-found "Not Found"))

(def app
  (handler/site
   (friend/authenticate app-routes
   			{:credential-fn (partial creds/bcrypt-credential-fn users)
                         :workflows [(workflows/interactive-form)]})))
