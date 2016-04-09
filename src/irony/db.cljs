(ns irony.db
  (:require
    [mount.core :as mount :include-macros true]
    [datascript.core :as d]))

(def schema
  {:aka {:db/cardinality :db.cardinality/many}})

(declare db)
(mount/defstate db
  :start (let [conn (d/create-conn schema)]
           (d/transact!
             conn
             [{:db/id -1
               :name  "Maksim"
               :age   45
               :aka   ["Maks Otto von Stirlitz", "Jack Ryan"]}
              {:db/id 0
               :name  "Jacobs"
               :age   32
               :aka   ["Maks Otto von Stirlitz", "Jack Ryan"]}])
           conn))
