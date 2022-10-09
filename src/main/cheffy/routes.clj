(ns cheffy.routes
  (:require [cheffy.recipes :as recipes]
            [io.pedestal.http.route :as route]))

(def routes
  (route/expand-routes
    #{["/recipes" :get recipes/list-recipes :route-name :list-recipes]
      ["/recipes" :post recipes/create-recipe :route-name :create-recipe!]
      ["/recipes/:recipe-id" :put recipes/upsert-recipe! :route-name :update-recipe!]}))
