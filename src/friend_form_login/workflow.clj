(ns friend-form-login.workflow
  (:require [cemerick.friend :as friend]
            [cemerick.friend.util :as util]))

(defn merge-auth-meta [identity]
  (vary-meta identity
             {:type ::friend/auth
              ::friend/workflow :email-login
              ::friend/redirect-on-auth? true}))

(defn email-workflow
  [& {:keys [login-uri] :as config}]
  (fn [{:keys [uri request-method] :as request}]
    (when (and (= uri (util/gets :login-uri config (::friend/auth-config request)))
               (= :post request-method))
      (merge-auth-meta {:identity "identity"}))))
