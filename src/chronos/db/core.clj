(ns chronos.db
  (:use [datomic.api :only [q db] :as d]
        [clojure.pprint]
        [chronos.db.helpers]))

(def calendar-schema
  [(create-attribute :calendar/name :db.type/string)
   (create-attribute :calendar/level :db.type/ref :db.cardinality/many true)

   (create-attribute :calendar.level/attribute :db.type/ref :db.cardinality/one true)
   (create-attribute :calendar.level/name :db.type/string)
   (create-attribute :calendar.level/index :db.type/long)
   (create-attribute :calendar.level/maxVal :db.type/long)
   (create-attribute :calendar.level/maxValFn :db.type/fn)

   (create-partition :db.part/calendars)])


(def time-schema
  [(create-attribute :time/calendar :db.type/ref)
   (create-partition :db.part/times)])

(def time-window-schema
  [(create-attribute :timeWindow/start :db.type/ref)
   (create-attribute :timeWindow/end :db.type/ref)])

(def event-schema
  [(create-attribute :event/time :db.type/ref :db.cardinality/one true)
   (create-attribute :event/timeWindow :db.type/ref :db.cardinality/one true)])

(def period-schema
  [;; ONE OF THESE
   (create-attribute :period/startEvent :db.type/ref :db.cardinality/one true)
   (create-attribute :period/startTime :db.type/ref :db.cardinality/one true)
   (create-attribute :period/startTimeWindow :db.type/ref :db.cardinality/one true)
   ;; AND ONE OF THESE
   (create-attribute :period/endEvent :db.type/ref :db.cardinality/one true)
   (create-attribute :period/endTime :db.type/ref :db.cardinality/one true)
   (create-attribute :period/endTimeWindow :db.type/ref :db.cardinality/one true)])

(def item-schema
  [(create-attribute :item/name :db.type/string)
   (create-attribute :item/description :db.type/string)
   (create-attribute :item/region :db.type/string)
   (create-attribute :item/theme :db.type/string)

   (create-partition :db.part/items)])

(def gregorian-calendar-schema
  [(create-attribute :calendar.gregorian/year :db.type/long)
   (create-attribute :calendar.gregorian/month :db.type/long)
   (create-attribute :calendar.gregorian/day :db.type/long)
   (create-attribute :calendar.gregorian/hour :db.type/long)
   (create-attribute :calendar.gregorian/minute :db.type/long)
   (create-attribute :calendar.gregorian/second :db.type/long)])

(defn-db days-in-month [year month]
  (cond
    (= month 1) 31
    (= month 2) (if (= 0 (mod year 4)) 29 28)
    (= month 3) 30
    (= month 4) 30
    (= month 5) 31
    (= month 6) 30
    (= month 7) 31
    (= month 8) 31
    (= month 9) 30
    (= month 10) 31
    (= month 11) 30
    (= month 12) 31))

(def gregorian-calendar
  [{:db/id (d/tempid :db.part/calendars 1)
    :db/ident :calendar/gregorian
    :calendar/name "Gregorian"}

   {:db/id (d/tempid :db.part/calendars)
    :calendar.level/attribute :calendar.gregorian/year
    :calendar.level/name "Year"
    :calendar.level/index 0
    :calendar/_level (d/tempid :db.part/calendars 1)}
   {:db/id (d/tempid :db.part/calendars)
    :calendar.level/attribute :calendar.gregorian/month
    :calendar.level/name "Month"
    :calendar.level/index 1
    :calendar.level/maxVal 12
    :calendar/_level (d/tempid :db.part/calendars 1)}
   {:db/id (d/tempid :db.part/calendars)
    :calendar.level/attribute :calendar.gregorian/day
    :calendar.level/name "Day"
    :calendar.level/index 2
    :calendar.level/maxValFn days-in-month
    :calendar/_level (d/tempid :db.part/calendars 1)}
   {:db/id (d/tempid :db.part/calendars)
    :calendar.level/attribute :calendar.gregorian/hour
    :calendar.level/name "Hour"
    :calendar.level/index 3
    :calendar.level/maxVal 24
    :calendar/_level (d/tempid :db.part/calendars 1)}
   {:db/id (d/tempid :db.part/calendars)
    :calendar.level/attribute :calendar.gregorian/minute
    :calendar.level/name "Minute"
    :calendar.level/index 4
    :calendar.level/maxVal 60
    :calendar/_level (d/tempid :db.part/calendars 1)}
   {:db/id (d/tempid :db.part/calendars)
    :calendar.level/attribute :calendar.gregorian/second
    :calendar.level/name "Second"
    :calendar.level/index 5
    :calendar.level/maxVal 60
    :calendar/_level (d/tempid :db.part/calendars 1)}])

(def mars-landing
  [{:db/id (d/tempid :db.part/times 1)
    :time/calendar :calendar/gregorian
    :calendar.gregorian/year 2012
    :calendar.gregorian/month 8
    :calendar.gregorian/day 6
    :calendar.gregorian/hour 5
    :calendar.gregorian/minute 17
    :calendar.gregorian/second 57}

   {:db/id (d/tempid :db.part/items)
    :event/time (d/tempid :db.part/times 1)
    :item/name "MSL Landing"
    :item/description "Mars Rover \"Curiosity\" touched down"
    :item/region "Mars"
    :item/theme "Space"}])

(def harold-ii
  [ ;; Start of reign (TIME)
   {:db/id (d/tempid :db.part/times 1)
    :time/calendar :calendar/gregorian
    :calendar.gregorian/year 1066
    :calendar.gregorian/month 1
    :calendar.gregorian/day 6}

   ;; End of reign, and battle (TIME)
   {:db/id (d/tempid :db.part/times 2)
    :time/calendar :calendar/gregorian
    :calendar.gregorian/year 1066
    :calendar.gregorian/month 10
    :calendar.gregorian/day 14}

   ;; Battle of hastings (EVENT)
   {:db/id (d/tempid :db.part/items 3)
    :event/time (d/tempid :db.part/times 2)
    :item/name "Battle of Hastings"
    :item/description "Occurred during Norman conquest of England."
    :item/region "England"
    :item/theme "Normans"}

   ;; Reign (PERIOD)
   {:db/id (d/tempid :db.part/items)
    :period/startTime (d/tempid :db.part/times 1)
    :period/endEvent (d/tempid :db.part/items 3)
    :item/name "Reign of King Harold II"
    :item/region "England"
    :item/theme "English Monarchs"}])


(do ;; (Re)Create and init database
  (def uri "datomic:mem://chronos")

  (try (d/delete-database uri) (catch RuntimeException e))
  (d/create-database uri)

  (def conn (d/connect uri))

  (def t (transact conn calendar-schema))
  (def t (transact conn time-schema))
  (def t (transact conn event-schema))
  (def t (transact conn period-schema))
  (def t (transact conn item-schema))

  (def t (transact conn gregorian-calendar-schema))
  (def t (transact conn gregorian-calendar))

  (def t (transact conn mars-landing))
  (def t (transact conn harold-ii)))
