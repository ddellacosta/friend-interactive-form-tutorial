(ns friend-form-login.workflow
  (:require [cemerick.friend :as friend]
            [cemerick.friend.util :as util]))

(defn make-auth [identity]
  (with-meta identity
    {:type ::friend/auth
     ::friend/workflow :email-login
     ::friend/redirect-on-auth? true}))

(defn email-workflow
  [& {:keys [credential-fn login-uri] :as config}]
  (fn [{:keys [uri request-method] :as request}]
    (when (and (= uri (util/gets :login-uri config
                                 (::friend/auth-config request)))
               (= :post request-method))
      (if-let [creds (credential-fn)]
        (make-auth creds)))))
