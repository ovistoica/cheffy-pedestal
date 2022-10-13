(ns cheffy.conversations
  (:require
    [cheffy.interceptors :as interceptors]
    [io.pedestal.http :as http]
    [io.pedestal.http.body-params :as bp]
    [io.pedestal.interceptor :as interceptor]
    [ring.util.response :as rr])
  (:import (java.util Date)))

(def conversation-pattern [:conversation/conversation-id
                           {:conversation/messages
                            [:message/message-id
                             :message/body
                             {:message/owner
                              [:account/account-id
                               :account/display-name]}]}])

(def message-pattern [:message/message-id
                      :message/body
                      :message/created-at
                      {:message/owner [:account/account-id :account/display-name]}])


(def find-conversations-by-account-id-interceptor
  (interceptor/interceptor
    {:name ::find-conversations-by-id-interceptor
     :enter
     (fn [{:keys [request] :as ctx}]
       (let [db (get-in request [:system/database :db])
             account-id (get-in request [:headers "authorization"])
             q-data {:query '[:find (pull ?c pattern)
                              :in $ ?account-id pattern
                              :where
                              [?a :account/account-id ?account-id] [?c :conversation/participants ?a]]
                     :args [db account-id conversation-pattern]}]
         (assoc ctx :q-data q-data)))
     :leave
     (fn [ctx]
       (let [conversations (mapv first (get ctx :q-result))]
         (assoc ctx :response (rr/response conversations))))}))

(def create-message-interceptor
  (interceptor/interceptor
    {:name ::create-message-interceptor
     :enter
     (fn [{:keys [request] :as ctx}]
       (let [path-conv-id (get-in request [:path-params :conversation-id])
             conversation-id (or (when path-conv-id (parse-uuid path-conv-id))
                                 (random-uuid))
             from (get-in request [:headers "authorization"])
             message-id (random-uuid)
             {:keys [to message-body]} (get-in request [:transit-params])]
         (assoc ctx :tx-data [{:conversation/conversation-id conversation-id
                               :conversation/participants (mapv #(vector :account/account-id %) [to from])
                               :conversation/messages (str message-id)}
                              {:db/id (str message-id)
                               :message/message-id message-id
                               :message/owner [:account/account-id from]
                               :message/body message-body
                               :message/read-by [[:account/account-id to]]
                               :message/created-at (Date.)}])))
     :leave (fn [ctx]
              (let [conversation-id (-> ctx :tx-data (first) :conversation/conversation-id)]
                (assoc ctx :response (rr/created
                                       (str "/conversations/" conversation-id)
                                       {:conversation-id conversation-id}))))}))

(def find-messages-by-conversation-id-interceptor
  (interceptor/interceptor
    {:name ::find-messages-by-conversation-id-interceptor
     :enter
     (fn [{:keys [request] :as ctx}]
       (let [db (get-in request [:system/database :db])
             conv-id (parse-uuid (get-in request [:path-params :conversation-id]))]
         (assoc ctx :q-data {:query '[:find (pull ?m pattern)
                                      :in $ ?conversation-id pattern
                                      :where
                                      [?e :conversation/conversation-id ?conversation-id]
                                      [?e :conversation/messages ?m]]
                             :args [db conv-id message-pattern]})))
     :leave
     (fn [ctx]
       (let [result (->> ctx :q-result (mapv first))]
         (assoc ctx :response (rr/response result))))}))

(def list-conversations
  [http/transit-body
   interceptors/db-interceptor
   find-conversations-by-account-id-interceptor
   interceptors/query-interceptor])


(def create-conversation
  [(bp/body-params)
   http/transit-body
   create-message-interceptor
   interceptors/transact-interceptor])

(def list-messages
  [http/transit-body
   interceptors/db-interceptor
   find-messages-by-conversation-id-interceptor
   interceptors/query-interceptor])
