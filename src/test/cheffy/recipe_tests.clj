(ns cheffy.recipe-tests
  (:require [cheffy.utils :as u]
            [clojure.test :refer :all]
            [com.stuartsierra.component.repl :as cr]
            [io.pedestal.http :as http]
            [io.pedestal.test :as pt]))

(defn api-service
  []
  (-> cr/system
      :api-server
      :service
      ::http/service-fn))

(defonce recipe-id (atom nil))
(defonce step-id (atom nil))

(deftest recipe-tests
  (let [new-recipe {:name "name"
                    :public true
                    :prep-time 30
                    :img "https://github.com/clojure.png"}]

    (testing "list recipes"
      (testing "with auth -- public and drafts"
        (let [{:keys [status body]}
              (-> (pt/response-for
                    (api-service)
                    :get "/recipes"
                    :headers {"Authorization" "auth|5fbf7db6271d5e0076903601"})
                  (update :body u/transit-read))]
          (is (= 200 status))
          (is (vector? (:public body)))
          (is (vector? (:drafts body)))))

      (testing "without auth -- public"
        (let [{:keys [status body]}
              (-> (pt/response-for
                    (api-service)
                    :get "/recipes")
                  (update :body u/transit-read))]
          (is (= 200 status))
          (is (vector? (:public body)))
          (is (nil? (:drafts body))))))

    (testing "create-recipe"
      (let [{:keys [status body]}
            (-> (pt/response-for
                  (api-service)
                  :post "/recipes"
                  :headers {"Authorization" "auth|5fbf7db6271d5e0076903601"
                            "Content-Type" "application/transit+json"}
                  :body (u/transit-write new-recipe))
                (update :body u/transit-read))]
        (reset! recipe-id (:recipe-id body))
        (is (= 201 status))))

    (testing "retrieve-recipe"
      (let [{:keys [status body]}
            (-> (pt/response-for
                  (api-service)
                  :get (str "/recipes/" @recipe-id)
                  :headers {"Authorization" "auth|5fbf7db6271d5e0076903601"
                            "Content-Type" "application/transit+json"})
                (update :body u/transit-read))]
        (is (= 200 status))
        (is (= body (assoc #:recipe{:display-name "name"
                                    :favorite-count 0
                                    :image-url "https://github.com/clojure.png"
                                    :owner #:account{:account-id "auth|5fbf7db6271d5e0076903601"
                                                     :display-name "Auth"}
                                    :prep-time 30
                                    :public? true}
                      :recipe/recipe-id @recipe-id)))))


    (testing "update-recipe"
      (let [{:keys [status]}
            (pt/response-for
              (api-service)
              :put (str "/recipes/" @recipe-id)
              :headers {"Authorization" "auth|5fbf7db6271d5e0076903601"
                        "Content-Type" "application/transit+json"}
              :body (u/transit-write {:name "updated name"
                                      :public true
                                      :prep-time 30
                                      :img "https://github.com/clojure.png"}))]
        (is (= 204 status))))

    (testing "create-step"
      (let [{:keys [status body]}
            (-> (pt/response-for
                  (api-service)
                  :post "/steps"
                  :headers {"Authorization" "auth|5fbf7db6271d5e0076903601"
                            "Content-Type" "application/transit+json"}
                  :body (u/transit-write {:recipe-id @recipe-id
                                          :description "new step"
                                          :sort-order 1}))
                (update :body u/transit-read))
            ]
        (reset! step-id (:step-id body))
        (is (= 201 status))))

    (testing "delete-recipe"
      (let [{:keys [status]}
            (pt/response-for
              (api-service)
              :delete (str "/recipes/" @recipe-id)
              :headers {"Authorization" "auth|5fbf7db6271d5e0076903601"
                        "Content-Type" "application/transit+json"})]
        (is (= 204 status))))))
