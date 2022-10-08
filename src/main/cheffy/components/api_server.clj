(ns cheffy.components.api-server
  (:require [cheffy.routes :as routes]
            [com.stuartsierra.component :as component]
            [io.pedestal.http :as http]))

(defrecord ApiServer [service-map service]

  component/Lifecycle

  (start [component]
    (println ";; Stating API Server")
    (let [service (-> service-map
                      (assoc ::http/routes routes/routes)
                      (http/create-server)
                      (http/start))]
      (assoc component :service service)))

  (stop [component]
    (println ";; Stopping API server")
    (when service
      (http/stop service))
    (assoc component :service nil)))

(defn service
  [service-map]
  (map->ApiServer {:service-map service-map}))
