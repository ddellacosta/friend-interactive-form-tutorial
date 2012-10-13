(ns friend-form-login.workflow-facts
  (:use midje.sweet)
  (:require [friend-form-login.workflow :as lw]
            [cemerick.friend :as friend]
            [ring.mock.request :as ring-mock]))

(def meta-data {:type ::friend/auth
                ::friend/workflow :email-login
                ::friend/redirect-on-auth? true})

(def creds {:identity "identity"
            :roles #{::user}})

;; Auth Map, as expected by Friend on a successful authentication:
(def authmap
  (with-meta creds meta-data))

(def config
  {:login-uri "/login"})

;; Mimicking how Friend attaches the configuration to the request
;; before passing it to the workflow when running authentication:
(def post-login-request
  (assoc 
      (ring-mock/request :post "/login")
    ::friend/auth-config config))

(defn credential-fn [] creds)

(defn email-workflow-fn [request]
  ((lw/email-workflow :credential-fn credential-fn) request))

;; (println config)
;; (println post-login-request)
;; (println (meta ((lw/email-workflow) post-login-request)))

(fact
 "Workflows return a function."
 (clojure.test/function? (lw/email-workflow)) => true)

(fact
 "Helper function make-auth returns an authmap"
 (lw/make-auth creds) => authmap)

(fact
 "make-auth sets correct meta-data"
 (meta (lw/make-auth {})) => meta-data)

(fact
 "On POST, the email-workflow returns an proper authmap."
 (email-workflow-fn post-login-request) => authmap)

(fact
 "On POST, the email-workflow function returns an authmap with the proper metadata."
 (meta (email-workflow-fn post-login-request)) => (meta authmap))

(fact
 "If we don't have the login url, we return nil."
 (email-workflow-fn (ring-mock/request :post "/not-login")) => nil)

(fact
 "The email-workflow function returns an authmap with the proper metadata."
 (meta (email-workflow-fn post-login-request)) => (meta authmap))

(fact
 "It sets a role in the identity"
 ((email-workflow-fn post-login-request) :roles) => #{::user})

;; (fact
;;  "It uses the credential-fn for verifying the credentials"
;;  ((meta ((lw/email-workflow credential-fn) post-login-request)) :roles) => #{::user})