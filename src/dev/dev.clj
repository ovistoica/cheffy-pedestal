(ns dev
  (:require [cheffy.server :as server]
            [clojure.edn :as edn]
            [io.pedestal.http :as http]
            [com.stuartsierra.component.repl :as cr]
            [datomic.client.api :as d]
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

(comment
  (:database cr/system)
  (:api-server cr/system)

  (start-dev)

  (stop-dev)

  (restart-dev)

  )

;; Datomic playground
(comment

  (def db (d/db (-> cr/system :database :conn)))

  (d/q '[:find ?e ?v ?display-name
         :in $ ?account-id
         :where
         [?e :recipe/recipe-id ?v]
         [?e :recipe/display-name ?display-name]
         [?e :recipe/owner ?account-id]]
       db 87960930222184)

  (d/q '[:find ?e ?id
         :where [?e :account/account-id ?id]]
       db)

  (d/q '[:find ?e ?v
         :where
         [?e :account/account-id ?v]] db)

  (d/pull db {:eid [:account/account-id "mike@mailinator.com"]
              :selector '[*]})

  (d/pull db {:eid [:account/account-id "mike@mailinator.com"]
              :selector '[:account/account-id
                          :account/display-name
                          {:account/favorite-recipes
                           [:recipe/recipe-id
                            :recipe/display-name]}]})

  )


;; Pedestal playground
(comment
  (pt/response-for
    (-> cr/system :api-server :service ::http/service-fn)
    :get
    "/recipes")

  )
