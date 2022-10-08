(ns user
  (:require [io.pedestal.http :as http]
            [io.pedestal.http.route :as route]))

(defonce system-ref (atom nil))


(defn list-recipes
  [request]
  {:status 200
   :body   "List recipes"})

(defn upsert-recipe!
  [request]
  {:status 200
   :body   "Upsert recipe"})

(def table-routes
  (route/expand-routes
    #{{:app-name :cheffy :schema :http :host "cheffy.ovistoica.com"}
      ["/recipes" :get list-recipes :route-name :list-recipes]
      ["/recipes" :post upsert-recipe! :route-name :create-recipe!]
      ["/recipes/:recipe-id" :put upsert-recipe! :route-name :update-recipe!]}))

(defn start-dev []
  (reset! system-ref
          (-> {::http/routes table-routes
               ::http/router :prefix-tree
               ::http/type   :jetty
               ::http/join?  false
               ::http/port   3000}
              (http/create-server)
              (http/start)))
  :started)

(defn stop-dev []
  (http/stop @system-ref)
  :stopped)

(defn restart-dev []
  (stop-dev)
  (start-dev)
  :restarted)

(comment

  (start-dev)

  (restart-dev)

  (stop-dev)

  )
