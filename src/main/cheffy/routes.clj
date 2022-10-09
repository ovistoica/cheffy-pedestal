(ns cheffy.routes
  (:require [cheffy.interceptors :as interceptors]
            [io.pedestal.http.route :as route]
            [io.pedestal.http :as http]))

(defn list-recipes
  [_]
  {:status 200
   :body "List recipes"})

(defn upsert-recipe!
  [_]
  {:status 200
   :body "Upsert recipe"})
(def routes
  (route/expand-routes
    #{ ["/recipes" :get [interceptors/db-interceptor list-recipes] :route-name :list-recipes]
      ["/recipes" :post upsert-recipe! :route-name :create-recipe!]
      ["/recipes/:recipe-id" :put upsert-recipe! :route-name :update-recipe!]}))
