(ns friend-form-login.workflow-facts
  (:use midje.sweet)
  (:require [friend-form-login.workflow :as lw]
            [cemerick.friend :as friend]
            [ring.mock.request :as ring-mock]))

;; Auth Map, as expected by Friend on a successful authentication:
(def authmap
  (vary-meta (merge
              ;; Dummy identity
              {:identity "identity"})
             ;; Meta-data
             {:type ::friend/auth
              ::friend/workflow :email-login
              ::friend/redirect-on-auth? true}))

(def config
  {:login-uri "/login"})

;; Mimicking how Friend attaches the configuration to the request
;; before passing it to the workflow when running authentication:
(def post-login-request
  (assoc 
      (ring-mock/request :post "/login")
    ::friend/auth-config config))

;; (println config)
;; (println post-login-request)
;; (println (meta ((lw/email-workflow) post-login-request)))

(fact
 "Workflows return a function."
 (clojure.test/function? (lw/email-workflow)) => true)

(fact
 "Helper function merge-auth-meta returns an authmap"
 (meta (lw/merge-auth-meta {})) => (meta authmap))

(fact
 "On POST, the email-workflow returns an proper authmap."
 ((lw/email-workflow) post-login-request) => authmap)

(fact
 "On POST, the email-workflow function returns an authmap with the proper metadata."
 (meta ((lw/email-workflow) post-login-request)) => (meta authmap))

(fact
 "If we don't have the login url, we return nil."
 ((lw/email-workflow) (ring-mock/request :post "/not-login")) => nil)

(fact
 "The email-workflow function returns an authmap with the proper metadata."
 (meta ((lw/email-workflow) post-login-request)) => (meta authmap))
