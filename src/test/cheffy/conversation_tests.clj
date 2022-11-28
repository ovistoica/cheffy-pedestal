(ns cheffy.conversation-tests
  (:require [cheffy.test-system :refer [api-service]]
            [cheffy.utils :as u]
            [clojure.test :refer :all]
            [io.pedestal.test :as pt]))

(defonce conversation-id (atom nil))

(deftest conversation-tests
  (testing "List conversations"
    (let [{:keys [status]} (-> (pt/response-for
                                 (api-service)
                                 :get "/conversations"
                                 :headers {"Authorization" "auth|5fbf7db6271d5e0076903601"})
                               (update :body u/transit-read))]
      (is (= 200 status))))
  (testing "Create messages"
    (testing "Without conversation-id"
      (let [{:keys [status body]}
            (-> (pt/response-for
                  (api-service)
                  :post "/conversations"
                  :headers {"Authorization" "auth|5fbf7db6271d5e0076903601"
                            "Content-Type" "application/transit+json"}
                  :body (u/transit-write {:to "mike@mailinator.com"
                                          :message-body "test message"}))
                (update :body u/transit-read))]
        (reset! conversation-id (:conversation-id body))
        (is (= 201 status))))


    (testing "With conversation-id"
      (let [{:keys [status]}
            (-> (pt/response-for
                  (api-service)
                  :post (str "/conversations/" @conversation-id)
                  :headers {"Authorization" "auth|5fbf7db6271d5e0076903601"
                            "Content-Type" "application/transit+json"}
                  :body (u/transit-write {:to "mike@mailinator.com"
                                          :message-body "test message"}))
                (update :body u/transit-read))]
        (is (= 201 status))))

    (testing "List conversation messages"
      (let [{:keys [status body]} (-> (pt/response-for
                                        (api-service)
                                        :get (str "/conversations/" @conversation-id)
                                        :headers {"Authorization" "auth|5fbf7db6271d5e0076903601"
                                                  "Content-Type" "application/transit+json"})
                                      (update :body u/transit-read))]
        (is (= 200 status))
        (is (every? #(and (uuid? (:message/message-id %))
                          (string? (:message/body %)))
                    body))))

    (testing "Clear conversation messages"
      (let [{:keys [status]} (pt/response-for
                               (api-service)
                               :delete (str "/conversations/" @conversation-id)
                               :headers {"Authorization" "auth|5fbf7db6271d5e0076903601"
                                         "Content-Type" "application/transit+json"})]
        (is (= 204 status))))))
