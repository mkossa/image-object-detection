(ns image-object-detection.unit.db-test
  (:require [clojure.test :refer [deftest is]]
            [image-object-detection.db :as sut]))

(deftest PGobject->clj-test
  (let [pgobject (org.postgresql.util.PGobject.)]
    (.setType pgobject "json")
    (.setValue pgobject "{\"key\": \"value\"}") 
    (is (= {"key" "value"}
           (#'sut/PGobject->clj pgobject)))))
