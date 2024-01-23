(ns image-object-detection.unit.get-test
  (:require [clojure.test :refer [deftest is]]
            [image-object-detection.get :as sut]))

(deftest get-image-ids-with-object-match-sql-test
  (is (= {:select [:image.id],
          :from [:image],
          :join [[:image_object_detection :iod] [:= :image.id :iod.image_id]],
          :where [:in :iod.object_detected ["dog" "cat"]]}
         (#'sut/get-image-ids-with-object-match-sql ["dog" "cat"]))))

(deftest get-image-by-ids-sql-test
  (is (= {:where [:in :image.id [1 2]],
          :select
          [:image.*
           [[[:coalesce
              [:filter
               [:json_agg
                [:json_build_object
                 "confidence"
                 :iod.confidence
                 "object_detected"
                 :iod.object_detected]]
               {:where [:not= :iod.confidence nil]}]
              nil]]
            :objects_detected]],
          :from [:image],
          :left-join
          [[:image_object_detection :iod] [:= :image.id :iod.image_id]],
          :group-by [:image.id]}
         (#'sut/get-image-by-ids-sql [1 2]))))

(deftest clean-str-test
  (is (= ["dog" "cat"]
         (#'sut/clean-str "  \"dog  \",\"cat\"  "))))
