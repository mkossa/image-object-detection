(ns image-object-detection.integration.post-test
  (:require [clj-http.client :as http]
            [clojure.test :refer [deftest is testing]]
            [image-object-detection.errors :as err]
            [image-object-detection.post :as sut]))

(def objects-detected
  {:body "{\"result\":{\"tags\":[{\"confidence\":100,\"tag\":{\"en\":\"object1\"}},{\"confidence\":64.8014373779297,\"tag\":{\"en\":\"object2\"}}]}}"})

(deftest image!-test
  (testing "Validation test that either image_url or image_file was provided"
    (is (thrown-with-msg? Exception (re-pattern err/neither-url-or-path-provided)
                          (sut/image! {:label "test-label"
                                       :detect_objects true}))))
  (testing "Validation test that only one of (image_url, image_file) was provided"
    (is (thrown-with-msg? Exception (re-pattern err/url-and-path-provided)
                          (sut/image! {:label "test-label"
                                       :detect_objects true
                                       :image_url "https://image.jpg"
                                       :image_file "C:\\path\\to\\image.jpg"}))))
  (testing "Validation test that provided image_file exists"
    (is (thrown-with-msg? Exception (re-pattern (format err/file-does-not-exist
                                                        "C:\\\\invalid\\\\path\\\\image.jpg"))
                          (sut/image! {:label "test-label"
                                       :detect_objects true
                                       :image_file "C:\\invalid\\path\\image.jpg"}))))
  (testing "Image URL provided with detect_objects true and a label"
    (with-redefs [http/get (constantly objects-detected)
                  sut/save-to-db! (constantly {:id 1
                                               :label "test-label"
                                               :image_location "https://image.jpg"
                                               :upload_time "2024-01-22 20:11:36.793938"})]
      (is (= {:id 1
              :label "test-label"
              :image_location "https://image.jpg"
              :upload_time "2024-01-22 20:11:36.793938"
              :objects_detected [{:confidence 100, :object "object1"}
                                 {:confidence 64.8014373779297, :object "object2"}]}
             (sut/image! {:label "test-label"
                          :detect_objects true
                          :image_url "https://image.jpg"})))))
  (testing "Image File provided with detect_objects true and a label"
    (with-redefs [http/post (constantly objects-detected)
                  sut/save-to-db! (constantly {:id 1
                                               :label "test-label"
                                               :image_location "C:\\path\\to\\image.jpg"
                                               :upload_time "2024-01-22 20:11:36.793938"})
                  sut/validate-request! (constantly nil)]
      (is (= {:id 1
              :label "test-label"
              :image_location "C:\\path\\to\\image.jpg"
              :upload_time "2024-01-22 20:11:36.793938"
              :objects_detected [{:confidence 100, :object "object1"}
                                 {:confidence 64.8014373779297, :object "object2"}]}
             (sut/image! {:label "test-label"
                          :detect_objects true
                          :image_file "C:\\path\\to\\image.jpg"})))))
  (testing "Image URL provided, but Imagga responds with an error status code"
    (with-redefs [http/get (constantly (fn [& _] (throw (ex-info "Imagga Failure Message"
                                                                 {:status 400
                                                                  :body "{\"status\": {\"text\": \"Imagga Failure Message\"}}"}))))]
      (is (thrown? Exception (sut/image! {:image_url "https://image.jpg"
                                          :detect_objects true})))))
  (testing "Minimum inputs provided to show label and detect_objects are optional"
    (with-redefs [sut/save-to-db! (constantly {:id 1
                                               :label "label-12345678"
                                               :image_location "C:\\path\\to\\image.jpg"
                                               :upload_time "2024-01-22 20:11:36.793938"})
                  sut/validate-request! (constantly nil)]
      (is (= {:id 1
              :label "label-12345678"
              :image_location "C:\\path\\to\\image.jpg"
              :upload_time "2024-01-22 20:11:36.793938"}
             (sut/image! {:image_file "C:\\path\\to\\image.jpg"}))))))
