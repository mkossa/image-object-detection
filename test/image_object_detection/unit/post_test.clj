(ns image-object-detection.unit.post-test
  (:require [clojure.test :refer [deftest is]]
            [image-object-detection.post :as sut]))

(deftest insert-image-sql-test
  (is (= {:insert-into [:image] 
          :values [{:label "test label" :image_location "test_image_location"}]
          :returning [:*]}
         (#'sut/insert-image-sql {:label "test label" :image_location "test_image_location"}))))

(deftest insert-obj-detected-sql-test
  (is (= {:insert-into [:image_object_detection]
          :values [{:object-detected "test-object"
                    :confidence 99.9999
                    :image_id 1}
                   {:object-detected "test-object2"
                    :confidence 100
                    :image_id 1}]}
         (#'sut/insert-obj-detected-sql 1
                                        [{:object "test-object"
                                          :confidence 99.9999}
                                         {:object "test-object2"
                                          :confidence 100}]))))
