(ns user
  (:require [dev :as dev]))


(defn reset
  []
  (dev/restart-dev))
