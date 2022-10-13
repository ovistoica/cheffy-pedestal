(ns cheffy.recipe-tests
  (:require [cheffy.utils :as u]
            [clojure.test :refer :all]
            [cheffy.test-system :refer [api-service]]
            [io.pedestal.test :as pt]))


(defonce recipe-id (atom nil))
(defonce step-id (atom nil))
(defonce ingredient-id (atom nil))

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
                (update :body u/transit-read))]
        (reset! step-id (:step-id body))
        (is (= 201 status))))


    (testing "update-step"
      (let [{:keys [status]}
            (pt/response-for
              (api-service)
              :put (str "/steps/" @step-id)
              :headers {"Authorization" "auth|5fbf7db6271d5e0076903601"
                        "Content-Type" "application/transit+json"}
              :body (u/transit-write {:recipe-id @recipe-id
                                      :description "new step updated"
                                      :sort-order 1}))]
        (is (= 204 status))))

    (testing "delete-step"
      (let [{:keys [status]}
            (pt/response-for
              (api-service)
              :delete (str "/steps/" @step-id)
              :headers {"Authorization" "auth|5fbf7db6271d5e0076903601"
                        "Content-Type" "application/transit+json"})]
        (is (= 204 status))
        (reset! step-id nil)))

    (testing "create-ingredient"
      (let [{:keys [status body]}
            (-> (pt/response-for
                  (api-service)
                  :post "/ingredients"
                  :headers {"Authorization" "auth|5fbf7db6271d5e0076903601"
                            "Content-Type" "application/transit+json"}
                  :body (u/transit-write {:recipe-id @recipe-id
                                          :amount 4
                                          :measure "cup"
                                          :display-name "sugar"
                                          :sort-order 1}))
                (update :body u/transit-read))]
        (reset! ingredient-id (:ingredient-id body))
        (is (= 201 status))))


    (testing "update-ingredient"
      (let [{:keys [status]}
            (pt/response-for
              (api-service)
              :put (str "/ingredients/" @ingredient-id)
              :headers {"Authorization" "auth|5fbf7db6271d5e0076903601"
                        "Content-Type" "application/transit+json"}
              :body (u/transit-write {:recipe-id @recipe-id
                                      :amount 5
                                      :measure "cup"
                                      :display-name "sugar updated"
                                      :sort-order 1}))]
        (is (= 204 status))))

    (testing "delete-ingredient"
      (let [{:keys [status]}
            (pt/response-for
              (api-service)
              :delete (str "/ingredients/" @ingredient-id)
              :headers {"Authorization" "auth|5fbf7db6271d5e0076903601"
                        "Content-Type" "application/transit+json"})]
        (is (= 204 status))
        (reset! ingredient-id nil)))

    (testing "delete-recipe"
      (let [{:keys [status]}
            (pt/response-for
              (api-service)
              :delete (str "/recipes/" @recipe-id)
              :headers {"Authorization" "auth|5fbf7db6271d5e0076903601"
                        "Content-Type" "application/transit+json"})]
        (is (= 204 status))
        (reset! recipe-id nil)))))
