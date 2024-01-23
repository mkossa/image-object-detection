(ns image-object-detection.db 
  (:require [clojure.data.json :as json]
            [clojure.walk :refer [postwalk]]
            [honey.sql :as sql]
            [next.jdbc :as jdbc]
            [next.jdbc.result-set :as rs]))

(defonce db-config (load-file "resources/db-config.edn"))
(defonce datasource (jdbc/get-datasource db-config))

(defn- PGobject->clj
  "Convert PGobject types to clj types."
  [x]
  (if (= org.postgresql.util.PGobject (type x))
    (json/read-str (.getValue x))
    x))

(def ^:private PGobjects->clj-objects (partial postwalk PGobject->clj))

(defn next-execute!
  "Format and execute a query and convert any PGobject types to clj types.
   Multi-arity to support transactions."
  ([query]
   (PGobjects->clj-objects (jdbc/execute! datasource
                                          (sql/format query)
                                          {:builder-fn rs/as-unqualified-lower-maps})))
  ([tx query]
   (PGobjects->clj-objects (jdbc/execute! tx
                                          (sql/format query 
                                          {:builder-fn rs/as-unqualified-lower-maps})))))

(defn next-execute-one!
  "Format and execute a query. Take the first value returned. Convert any
   PGobject types to clj types. Multi-arity to support transactions."
  ([query]
   (PGobjects->clj-objects (jdbc/execute-one! datasource
                                              (sql/format query)
                                              {:builder-fn rs/as-unqualified-lower-maps}))) 
  ([tx query] 
   (PGobjects->clj-objects (jdbc/execute-one! tx
                                              (sql/format query)
                                              {:builder-fn rs/as-unqualified-lower-maps}))))
