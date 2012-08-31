(ns chronos.db.helpers
  (:use [datomic.api :only [q db] :as d]
        [clojure.pprint])
  (:require [cljs.closure :as cljsc]))

(defn create-attribute
  ([ident type] (create-attribute ident type :db.cardinality/one))
  ([ident type cardinality] (create-attribute ident type cardinality false))
  ([ident type cardinality is-component]
     {:db/id (d/tempid :db.part/db)
      :db/ident ident
      :db/valueType type
      :db/cardinality cardinality
      :db/isComponent is-component
      :db.install/_attribute :db.part/db}))

(defn create-partition [ident]
  {:db/id (d/tempid :db.part/db),
    :db/ident ident,
    :db.install/_partition :db.part/db})

;; A macro to associate returned values with their variable names in the query.
;; Works just like (datomic.api/q ...) except datalog should not be quoted.
(defmacro query [datalog & sources]
  (let [find (first datalog)
        vars (take-while #(not (keyword? %)) (drop 1 datalog))
        rest (drop (+ 1 (count vars)) datalog)]
    `(try (map (fn [matches#] (apply assoc {}
                                    (apply concat
                                           (map (fn [val# var#] [var# val#])
                                                matches#
                                                '~(map #(keyword (clojure.string/replace (str %) "?" "")) vars)))))
               (q '[~find ~@vars ~@rest] ~@sources))
          (catch Exception e# (pprint e#)))))


;; NOTE: transact fails if you give it an enormous seq. Maybe.
(defn transact [conn tx-data]
  (try
    @(d/transact conn tx-data)
    (catch Exception e (pprint e))
    (catch Error e (pprint e))))

(defn entity-map
  ([entity] (entity-map entity 3))
  ([entity max-depth] (reduce (fn [m k]
                                (if (> max-depth 0)
                                  (cond
                                    (instance? datomic.query.EntityMap (k entity))
                                    (assoc m k (entity-map (k entity) (dec max-depth)))
                                    (set? (k entity))
                                    (assoc m k (set (map #(entity-map % (dec max-depth)) (k entity))))
                                    :else
                                    (assoc m k (k entity))))) {} (keys entity))))

(defn pprint-entity [entity]
  (pprint (entity-map entity)))

(defmacro defn-db-and-js [name [& args] & body]
  `(do (def ~(symbol (str name "-db")) (d/function '{:lang "clojure"
                                :params ~args
                                :code (do ~@body)}))
       (def ~(symbol (str name "-js"))
         (cljsc/build (list ~''defn '~name '[~@args] '(do ~@body)) {:output-dir "tmp" :output-to nil :optimizations :simple :pretty-print false}))))
