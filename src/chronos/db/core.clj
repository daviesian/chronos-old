(ns chronos.db
  (:use [datomic.api :only [q db] :as d]
        [clojure.pprint]
        [chronos.db.helpers]))



(def chronos-schema
  [(create-attribute :calendar/name :db.type/string)
   (create-attribute :calendar/levels :db.type/ref :db.cardinality/many true)

   (create-attribute :calendar.level/name :db.type/string)
   (create-attribute :calendar.level/)
   ])
