(ns cheffy.test-system
  (:require [clojure.test :refer :all]
            [com.stuartsierra.component.repl :as cr]
            [io.pedestal.http :as http]))

(defn api-service
  []
  (-> cr/system
      :api-server
      :service
      ::http/service-fn))
