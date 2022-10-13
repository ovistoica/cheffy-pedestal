(ns cheffy.recipe.steps
  (:require
    [cheffy.interceptors :as interceptors]
    [io.pedestal.http :as http]
    [io.pedestal.http.body-params :as bp]
    [io.pedestal.interceptor :as interceptor]
    [ring.util.response :as rr]))


(def step-interceptor
  (interceptor/interceptor
    {:name ::step-interceptor
     :enter
     (fn [{:keys [request] :as ctx}]
       (let [{:keys [recipe-id description sort-order]} (:transit-params request)
             path-step-id (get-in ctx [:request :path-params :step-id])
             step-id (or (when path-step-id (parse-uuid path-step-id)) (random-uuid))]
         (assoc ctx :tx-data [{:recipe/recipe-id recipe-id
                               :recipe/steps [{:step/step-id step-id
                                               :step/description description
                                               :step/sort-order sort-order}]}])))
     :leave
     (fn [ctx]
       (if (get-in ctx [:request :path-params :step-id])    ;; path-step-id?
         (assoc ctx :response (rr/status 204))              ;; update
         (let [recipe-id (-> ctx :tx-data (first) :recipe/recipe-id)
               step-id (-> ctx :tx-data (first) :recipe/steps (first) :step/step-id)]
           (assoc ctx :response (rr/created                 ;;create
                                  (str "/recipes/" recipe-id)
                                  {:step-id step-id})))))}))

(def retract-step-interceptor
  (interceptor/interceptor
    {:name ::retract-step
     :enter
     (fn [ctx]
       (let [step-id (get-in ctx [:request :path-params :step-id])]
         (assoc ctx :tx-data [[:db/retractEntity [:step/step-id step-id]]])))
     :leave
     (fn [ctx]
       (assoc ctx :response (rr/status 204)))}))


(def upsert-step
  [(bp/body-params)
   http/transit-body
   step-interceptor
   interceptors/transact-interceptor])


(def delete-step
  [retract-step-interceptor
   interceptors/transact-interceptor])
