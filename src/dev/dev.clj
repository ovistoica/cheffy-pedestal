(ns dev
  (:require [cheffy.recipes :as recipes]
            [cheffy.server :as server]
            [clojure.edn :as edn]
            [cognitect.transit :as transit]
            [com.stuartsierra.component.repl :as cr]
            [datomic.client.api :as d]
            [io.pedestal.http :as http]
            [io.pedestal.test :as pt])
  (:import (java.io ByteArrayInputStream ByteArrayOutputStream)))

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

(defn db
  []
  (d/db (-> cr/system :database :conn)))

(defn api-service
  []
  (-> cr/system :api-server :service ::http/service-fn))

(defn transit-write [obj]
  (let [out (ByteArrayOutputStream.)
        writer (transit/writer out :json)]
    (transit/write writer obj)
    (.toString out)))

(defn transit-read [txt]
  (let [in (ByteArrayInputStream. (.getBytes txt))
        reader (transit/reader in :json)]
    (transit/read reader)))


(comment
  (:database cr/system)
  (:api-server cr/system)

  (start-dev)

  (stop-dev)

  (restart-dev)

  )

;; Datomic playground
(comment

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
  (-> (pt/response-for
        (api-service)
        :get "/recipes"
        :headers {"Authorization" "auth|5fbf7db6271d5e0076903601"})
      :body
      (transit-read)
      :public)


  (pt/response-for
    (api-service)
    :post "/recipes"
    :headers {"Authorization" "auth|5fbf7db6271d5e0076903601"
              "Content-Type" "application/transit+json"}
    :body (transit-write {:name "name"
                          :public true
                          :prep-time 30
                          :img "https://github.com/clojure.png"}))

  )


