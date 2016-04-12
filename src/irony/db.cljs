(ns irony.db
  (:require
    [faker.name]
    [faker.internet]
    [datascript.core :as d]))

(def user-schema
  {:user/gid {:db/index true}})

(def schema (merge user-schema))

(def db (d/create-conn schema))

(defn load-data []
  (let [fake-users
        (for [id (range 15)]
          {:user/gid      id
           :user/name     (faker.name/one-name)
           :user/username (faker.name/first-name)
           :user/email    (faker.internet/email)
           :user/website  (faker.internet/domain-name)})]
    (d/transact! db fake-users)))

(load-data)

;(declare db)
;(mount/defstate db
;  :start (let [conn (d/create-conn schema)]
;           (d/transact!
;             conn
;             [{:db/id -1
;               :name  "Maksim"
;               :age   45
;               :aka   ["Maks Otto von Stirlitz", "Jack Ryan"]}
;              {:db/id 0
;               :name  "Jacobs"
;               :age   32
;               :aka   ["Maks Otto von Stirlitz", "Jack Ryan"]}])
;           conn))


