(ns dev
  (:require [cheffy.recipes :as recipes]
            [cheffy.server :as server]
            [cheffy.utils :as u]
            [clojure.edn :as edn]
            [com.stuartsierra.component.repl :as cr]
            [datomic.client.api :as d]
            [io.pedestal.http :as http]
            [io.pedestal.test :as pt]))

(defn system
  [_]
  (-> "src/config/cheffy/development.edn"
    (slurp)
    (edn/read-string)
    (server/create-system)))

(cr/set-init system)

(defn start-dev []
  (cr/start))

(defn stop-dev []
  (cr/stop))

(defn restart-dev []
  (cr/reset))

(defn datomic-conn
  []
  (-> cr/system :database :conn))

(defn db
  []
  (d/db (datomic-conn)))

(defn api-service
  []
  (-> cr/system :api-server :service ::http/service-fn))

(defn db-connection []
  (-> cr/system :database :conn))


(comment (:database cr/system)
  (:api-server cr/system)

  (start-dev)

  (stop-dev)

  (restart-dev)

  )


;; Datomic playground
(comment

  (require '[cognitect.aws.client.api :as aws])

  (def cognito-idp (aws/client {:api :cognito-idp}))

  (aws/doc cognito-idp :SignUp)

  (aws/validate-requests cognito-idp true)

  (aws/invoke cognito-idp
    {:op :SignUp
     :request {:ClientId "42rpelce3vkk4fp69ahmqgcj1g"
               :Username "ovidiu.stoica1094+aws@gmail.com"
               :Password "Pa$$w0rd"
               :SecretHash (calculate-secret-hash {:client-id "client-id"
                                                   :client-secret "client-secret"
                                                   :username "username"})}})


  ; Get messages read by account
  (let [account-id "auth|5fbf7db6271d5e0076903601"
        conversation-id #uuid"8d4ab926-d5cc-483d-9af0-19627ed468eb"]
    (->> (d/q '[:find ?m
                :in $ ?account-id ?conversation-id
                :where
                [?a :account/account-id ?account-id]
                [?e :conversation/conversation-id ?conversation-id]
                [?e :conversation/messages ?m]
                [?c :conversation/messages ?m]
                (not [?m :message/read-by ?a])]
           (db) account-id conversation-id)
      (map first)))

  (d/transact (db-connection)
    {:tx-data [[:db/add 83562883711094
                :message/read-by [:account/account-id "auth|5fbf7db6271d5e0076903601"]]]})

  (d/q '[:find ?e ?id
         :where [?e :account/account-id ?id]]
    (db))

  (d/q '[:find ?e ?v
         :where
         [?e :account/account-id ?v]] (db))

  (d/pull (db) {:eid [:account/account-id "mike@mailinator.com"]
                :selector '[*]})

  (d/pull (db) {:eid [:account/account-id "mike@mailinator.com"]
                :selector '[:account/account-id
                            :account/display-name
                            {:account/favorite-recipes
                             [:recipe/recipe-id
                              :recipe/display-name]}]})

  (def conversation-pattern [:conversation/conversation-id
                             {:conversation/messages
                              [:message/message-id
                               :message/body
                               {:message/owner
                                [:account/account-id
                                 :account/display-name]}]}])

  (d/q '[:find (pull ?c pattern)
         :in $ ?account-id pattern
         :where
         [?a :account/account-id ?account-id]
         [?c :conversation/participants ?a]]
    (db) "auth|5fbf7db6271d5e0076903601" conversation-pattern)

  (let [conversation-id (random-uuid)
        message-id (random-uuid)
        from "auth|5fbf7db6271d5e0076903601"
        to "mike@mailinator.com"
        message-body "message body"]
    (d/transact (datomic-conn)
      {:tx-data [{:conversation/conversation-id conversation-id
                  :conversation/participants (mapv #(vector :account/account-id %) [to from])
                  :conversation/messages (str message-id)}
                 {:db/id (str message-id)
                  :message/message-id message-id
                  :message/owner [:account/account-id from]
                  :message/body message-body
                  :message/read-by [[:account/account-id to]]
                  :message/created-at (java.util.Date.)}
                 ]}))


  (let [conv-id #uuid"8908e487-24fd-49c5-9011-2370acda533d"
        message-pattern [:message/message-id
                         :message/body
                         :message/created-at
                         {:message/owner [:account/account-id :account/display-name]}]]
    (d/q '[:find (pull ?m pattern)
           :in $ ?conversation-id pattern
           :where
           [?e :conversation/conversation-id ?conversation-id]
           [?e :conversation/messages ?m]]
      (db) conv-id message-pattern))

  (let [account-id "auth|5fbf7db6271d5e0076903601"
        recipe-pattern
        [:recipe/recipe-id
         :recipe/prep-time
         :recipe/display-name
         :recipe/image-url
         :recipe/public?
         :account/_favorite-recipes
         {:recipe/owner
          [:account/account-id
           :account/display-name]}
         {:recipe/steps [:step/step-id
                         :step/description
                         :step/sort-order]}
         {:recipe/ingredients [:ingredient/ingredient-id
                               :ingredient/display-name
                               :ingredient/amount
                               :ingredient/measure
                               :ingredient/sort-order]}]]
    (mapv recipes/query-result->recipe (d/q '[:find (pull ?e pattern)
                                              :in $ ?account-id pattern
                                              :where
                                              [?owner :account/account-id ?account-id]
                                              [?e :recipe/public? false]]
                                         (db) account-id recipe-pattern))))



;; Pedestal playground
(comment
  (pt/response-for
    (api-service)
    :get "/recipes"
    :headers {"Authorization" "auth|5fbf7db6271d5e0076903601"})

  (def recipe "a1995316-80ea-4a98-939d-7c6295e4bb46")

  (pt/response-for
    (api-service)
    :get (str "/recipes/" recipe)
    :headers {"Authorization" "mike@mailinator.com"})


  (pt/response-for
    (api-service)
    :post "/recipes"
    :headers {"Authorization" "auth|5fbf7db6271d5e0076903601"
              "Content-Type" "application/transit+json"}
    :body (u/transit-write {:name "name"
                            :public true
                            :prep-time 30
                            :img "https://github.com/clojure.png"}))

  (let [c #uuid"8908e487-24fd-49c5-9011-2370acda533d"]
    (pt/response-for
      (api-service)
      :get (str "/conversations/" c)
      :headers {"Authorization" "auth|5fbf7db6271d5e0076903601"
                "Content-Type" "application/transit+json"}))

  )


