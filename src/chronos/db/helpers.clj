(ns chronos.db.helpers
  (:use [datomic.api :only [q db] :as d]
        [clojure.pprint]))

(defn create-attribute
  ([ident type] (create-attribute ident type :db.cardinality/one))
  ([ident type cardinality] (create-attribute ident type cardinality false))
  ([ident type cardinality is-component]
     {:db/id (d/tempid :db.part/db)
      :db/valueType type
      :db/cardinality cardinality
      :db/isComponent is-component
      :db.install/_attribute :db.part/db}))
