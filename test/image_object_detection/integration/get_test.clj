(ns image-object-detection.integration.get-test
  (:require [clojure.test :refer [deftest is testing]]
            [image-object-detection.db :as db]
            [image-object-detection.errors :as err]
            [image-object-detection.get :as sut]))

(def all-images
  [{:id 1
    :label "hedgehog"
    :image_location "https://hedgehog.jpg"
    :upload_time "2024-01-23T02:11:36Z"
    :objects_detected [{:confidence 100 :object_detected "mammal"}]}
   {:id 2
    :label "label-f525de1d"
    :image_location "https://image.jpg"
    :upload_time "2024-01-23T04:23:42Z"}
   {:id 3
    :label "label from insomnia (url)"
    :image_location "C:\\path\\to\\windmill.jpg"
    :upload_time "2024-01-23T06:19:55Z"
    :objects_detected
    [{:confidence 63.3033409118652 :object_detected "generator"}
     {:confidence 61.4765892028809 :object_detected "energy"}]}])

(deftest images!-test
  (testing "Return all images when no object filter is provided"
    (with-redefs [db/next-execute! (constantly all-images)]
      (is (= all-images
             (sut/images! {})))))
  (testing "Return the images that have any of the detected images" 
    (with-redefs [db/next-execute! (fn [x] (if (= x {:select [:image.id],
                                                     :from [:image],
                                                     :join [[:image_object_detection :iod] [:= :image.id :iod.image_id]],
                                                     :where [:in :iod.object_detected ["hedgehog" "generator"]]})
                                             all-images
                                             (mapv #(get all-images %) [0 2])))]
      (is (= (mapv #(get all-images %) [0 2])
             (sut/images! {:query-params {:objects "hedgehog,generator"}})))))
  (testing "Return all images when no object provided matches the filter"
    (with-redefs [db/next-execute! (constantly all-images)]
      (is (= all-images
             (sut/images! {:query-params {:objects "object-that-has-not-been-detected"}}))))))

(deftest images-by-id!-test
  (testing "Return the image based on the provided id"
    (with-redefs [db/next-execute-one! (constantly (first all-images))]
      (is (= (first all-images)
             (sut/image-by-id! "1")))))
  (testing "Raise an error if an invalid image id is provided"
    (with-redefs [db/next-execute-one! (constantly nil)]
      (is (thrown-with-msg? Exception (re-pattern (format err/image-id-not-found
                                                          100))
                            (sut/image-by-id! "100"))))))
