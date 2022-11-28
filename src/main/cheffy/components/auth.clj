(ns cheffy.components.auth
  (:require [cognitect.aws.client.api :as aws]
            [com.stuartsierra.component :as component])
  (:import (java.nio.charset StandardCharsets)
           (java.util Base64)
           (javax.crypto Mac)
           (javax.crypto.spec SecretKeySpec)))

(defn calculate-secret-hash
  [{:keys [client-id client-secret username]}]
  (try
    (let [hmac-sha256-algorithm "HmacSHA256"
          signing-key (SecretKeySpec. (.getBytes client-secret StandardCharsets/UTF_8) hmac-sha256-algorithm)
          mac (doto (Mac/getInstance hmac-sha256-algorithm)
                (.init signing-key)
                (.update (.getBytes username)))
          raw-mac (.doFinal mac (.getBytes client-id StandardCharsets/UTF_8))]
      (.encodeToString (Base64/getEncoder) raw-mac))
    (catch Exception e
      (throw (ex-info "Error while calculating secret hash"
               {:message (ex-message e)}
               e)))))

(comment
  (calculate-secret-hash {:client-id "client-id"
                          :client-secret "client-secret"
                          :username "username"}))

(defrecord Auth [config cognito-id]
  component/Lifecycle

  (start [component]
    (println ";; Starting Auth")
    (assoc component
      :cognito-idp (aws/client {:api :cognito-idp})))

  (stop [component]
    (println ";; Stopping Auth")
    (assoc component
      :cognito-idp nil)))

(defn config
  [config]
  (map->Auth {:config config}))

