(ns image-object-detection.unit.imagga-test
  (:require [clojure.test :refer [deftest is]]
            [image-object-detection.imagga :as sut]))

(def imagga-resp {:body
                  "{\"result\":{\"tags\":[{\"confidence\":100,\"tag\":{\"en\":\"test-object\"}},
                                          {\"confidence\":64.8014373779297,\"tag\":{\"en\":\"test-object2\"}},
                                          {\"confidence\":63.3033409118652,\"tag\":{\"en\":\"test-object3\"}}]},
                    \"status\":{\"text\":\"\",\"type\":\"success\"}}"})

(deftest raise-obj-found-test
  (is (= {:tag "test-object"}
         (#'sut/raise-obj-found {:tag {:en "test-object"}}))))

(deftest parse-tags-test
  (is (= [{:confidence 100, :object "test-object"}
          {:confidence 64.8014373779297, :object "test-object2"}
          {:confidence 63.3033409118652, :object "test-object3"}]
         (#'sut/parse-response imagga-resp))))
