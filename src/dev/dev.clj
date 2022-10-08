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

(defonce system-ref (atom nil))

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
  (:database cr/system)

  (restart-dev)

  (stop-dev)

  )
(ns dev)
