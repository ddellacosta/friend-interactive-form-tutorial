(ns friend-form-login.workflow-facts
  (:use midje.sweet)
  (:require [friend-form-login.workflow :as lw]
            [cemerick.friend :as friend]
            [ring.mock.request :as ring-mock]
            [postal.core :as postal]))

(def postal-config
  (with-meta
    {:from "ddellacosta@gmail.com"
     :to "dave@dubitable.com"
     :subject "YEAH!"
     :body "test"}
    {:host "smtp.gmail.com"
     :user "ddellacosta@gmail.com"
     :pass ""
     :ssl :yes}))

(background
 (postal/send-message postal-config) =>
 (fn [& anything] {:error :SUCCESS, :code 0, :message "messages sent"}))

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

(def request-with-cred-fn
  (update-in post-login-request [::friend/auth-config]
             (partial merge {:credential-fn credential-fn})))

(defn email-workflow-fn [request]
  ((lw/email-workflow :credential-fn credential-fn) request))

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

(fact
 "It picks up the credential-fn from the request as well."
 (((lw/email-workflow) request-with-cred-fn) :roles) => #{::user})

;; Gotta add credentials to be tested.
(future-fact
 "It uses the credential-fn for verifying the credentials"
 (((lw/email-workflow) request-with-cred-fn) :roles) => #{::user})

;; How do I mock how functions properly in Midje again?
(def mock-email-sent nil)

(fact
 "It sends an email on a successful authentication."
 (((lw/email-workflow) request-with-cred-fn) :roles) => #{::user}
 (provided
  (postal/send-message postal-config) => 1 :times 1))

(future-fact
 "It sets the email 'from' map from configuration data")

(future-fact
 "It sets the email 'to' map from user credentials")

(future-fact
 "It generates a token to be sent with the email."
 token => true)

;; Generating token,
;; Devise's method:
;;   SecureRandom.base64(15).tr('+/=lIO0', 'pqrsxyz')
