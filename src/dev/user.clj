(ns user
  (:require [cheffy.server :as server]
            [clojure.edn :as edn]
            [com.stuartsierra.component :as component]))

(defonce system-ref (atom nil))

(defn start-dev []
  (let [config (-> "src/config/cheffy/development.edn" slurp edn/read-string)]
    (reset! system-ref
            (-> config
                (server/create-system)
                (component/start))))
  :started)

(defn stop-dev []
  (component/stop @system-ref)
  :stopped)

(defn restart-dev []
  (stop-dev)
  (start-dev)
  :restarted)

(comment

  (:api-server @system-ref)

  (start-dev)

  (restart-dev)

  (stop-dev)

  )
