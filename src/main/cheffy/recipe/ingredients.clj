(ns cheffy.recipe.ingredients
  (:require
    [cheffy.interceptors :as interceptors]
    [io.pedestal.http :as http]
    [io.pedestal.http.body-params :as bp]
    [io.pedestal.interceptor :as interceptor]
    [ring.util.response :as rr]))


(def ingredient-interceptor
  (interceptor/interceptor
    {:name ::ingredient-interceptor
     :enter
     (fn [{:keys [request] :as ctx}]
       (let [{:keys [display-name recipe-id sort-order amount measure]}
             (:transit-params request)
             path-ingredient-id (get-in request [:path-params :ingredient-id])
             ingredient-id (or (when path-ingredient-id
                                 (parse-uuid path-ingredient-id))
                               (random-uuid))]
         (assoc ctx :tx-data [{:recipe/recipe-id recipe-id
                               :recipe/ingredients [{:ingredient/ingredient-id ingredient-id
                                                     :ingredient/amount amount
                                                     :ingredient/display-name display-name
                                                     :ingredient/measure measure
                                                     :ingredient/sort-order sort-order}]}])))
     :leave
     (fn [{:keys [tx-data request] :as ctx}]
       (if (get-in request [:path-params :ingredient-id])
         (assoc ctx :response (rr/status 204))
         (let [ingredient-id (-> tx-data
                                 (first)
                                 :recipe/ingredients
                                 (first)
                                 :ingredient/ingredient-id)
               recipe-id (-> tx-data (first) :recipe/recipe-id)]
           (assoc ctx :response (rr/created (str "/recipes/" recipe-id)
                                            {:ingredient-id ingredient-id})))))}))

(def retract-ingredient-interceptor
  (interceptor/interceptor
    {:name ::retract-ingredient
     :enter
     (fn [ctx]
       (let [ingredient-id (get-in ctx [:request :path-params :ingredient-id])]
         (assoc ctx :tx-data [[:db/retractEntity [:ingredient/ingredient-id ingredient-id]]])))
     :leave
     (fn [ctx]
       (assoc ctx :response (rr/status 204)))}))

(def upsert-ingredient
  [(bp/body-params)
   http/transit-body
   ingredient-interceptor
   interceptors/transact-interceptor])

(def delete-ingredient
  [retract-ingredient-interceptor
   interceptors/transact-interceptor])
