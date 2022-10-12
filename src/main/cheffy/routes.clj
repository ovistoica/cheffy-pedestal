(ns cheffy.routes
  (:require [cheffy.recipe.steps :as steps]
            [cheffy.recipes :as recipes]
            [io.pedestal.http.route :as route]))

(defn routes
  []
  (route/expand-routes
    #{["/recipes" :get recipes/list-recipes :route-name :list-recipes]
      ["/recipes" :post recipes/create-recipe :route-name :create-recipe]
      ["/recipes/:recipe-id" :get recipes/retrieve-recipe :route-name :retrieve-recipe]
      ["/recipes/:recipe-id" :put recipes/update-recipe :route-name :update-recipe]
      ["/recipes/:recipe-id" :delete recipes/delete-recipe :route-name :delete-recipe]
      ;; steps
      ["/steps" :post steps/create-step :route-name :create-step]}))
