(ns cheffy.recipes
  (:require [cheffy.interceptors :as interceptors]
            [datomic.client.api :as d]
            [io.pedestal.http :as http]
            [io.pedestal.http.body-params :as bp]
            [ring.util.response :as rr]))

(defn query-result->recipe
  [[{:account/keys [_favorite-recipes] :as recipe}]]
  (-> recipe
      (assoc :recipe/favorite-count (count _favorite-recipes))
      (dissoc :account/_favorite-recipes)))

(defn list-recipes-response
  [request]
  (let [db (get-in request [:system/database :db])
        account-id (get-in request [:headers "authorization"])
        recipe-pattern
        [:recipe/recipe-id
         :recipe/prep-time
         :recipe/display-name
         :recipe/image-url
         :recipe/public?
         :account/_favorite-recipes {:recipe/owner
                                     [:account/account-id
                                      :account/display-name]}
         {:recipe/steps [:step/step-id
                         :step/description
                         :step/sort-order]}
         {:recipe/ingredients [:ingredient/ingredient-id
                               :ingredient/display-name
                               :ingredient/amount
                               :ingredient/measure
                               :ingredient/sort-order]}]
        public-recipes (->> (d/q '[:find (pull ?e pattern)
                                   :in $ pattern
                                   :where [?e :recipe/public? true]]
                                 db recipe-pattern)
                            (mapv query-result->recipe))]
    (cond-> {:public public-recipes}
            account-id (assoc :drafts
                              (mapv query-result->recipe
                                    (d/q '[:find (pull ?e pattern)
                                           :in $ ?account-id pattern
                                           :where
                                           [?owner :account/account-id ?account-id]
                                           [?e :recipe/owner ?owner]
                                           [?e :recipe/public? false]]
                                         db account-id recipe-pattern)))
            true (rr/response))))

(def list-recipes
  [interceptors/db-interceptor http/transit-body list-recipes-response])

(defn create-recipe-response
  [request]
  (let [account-id (get-in request [:headers "authorization"])
        recipe-id (random-uuid)
        {:keys [name prep-time public img]} (get-in request [:transit-params])
        conn (get-in request [:system/database :conn])]
    (d/transact conn {:tx-data [{:recipe/display-name name
                                 :recipe/recipe-id recipe-id
                                 :recipe/public? public
                                 :recipe/prep-time prep-time
                                 :recipe/image-url img
                                 :recipe/owner [:account/account-id account-id]}]})
    (rr/created (str "/recipes/" recipe-id) {:recipe-id recipe-id})))

(def create-recipe
  [(bp/body-params)
   http/transit-body
   create-recipe-response])

(defn upsert-recipes-response!
  [_]
  {:status 200
   :body "upsert recipes"})

(def upsert-recipe!
  [interceptors/db-interceptor upsert-recipes-response!])
