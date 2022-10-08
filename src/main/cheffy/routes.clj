(ns cheffy.routes
  (:require [io.pedestal.http.route :as route]))

(defn list-recipes
  [_]
  {:status 200
   :body   "List recipes"})

(defn upsert-recipe!
  [_]
  {:status 200
   :body   "Upsert recipe"})
(def routes
  (route/expand-routes
    #{{:app-name :cheffy :schema :http :host "cheffy.ovistoica.com"}
      ["/recipes" :get list-recipes :route-name :list-recipes]
      ["/recipes" :post upsert-recipe! :route-name :create-recipe!]
      ["/recipes/:recipe-id" :put upsert-recipe! :route-name :update-recipe!]}))
