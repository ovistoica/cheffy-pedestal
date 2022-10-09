(ns dev
  (:require [cheffy.server :as server]
            [clojure.edn :as edn]
            [com.stuartsierra.component.repl :as cr]))

(defn system
  [_]
  (-> "src/config/cheffy/development.edn"
      (slurp)
      (edn/read-string)
      (server/create-system)))

(cr/set-init system)

(defn start-dev []
  (cr/start))

(defn stop-dev []
  (cr/stop))

(defn restart-dev []
  (cr/reset))

(comment



  (:database cr/system)
  (:api-server cr/system)

  (start-dev)
  (stop-dev)
  (restart-dev)
  )
