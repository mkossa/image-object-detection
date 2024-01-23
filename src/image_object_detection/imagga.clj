(ns image-object-detection.imagga
  (:require [clj-http.client :as http]
            [clojure.data.json :as json]
            [clojure.java.io :as io]
            [clojure.set :as set]
            [image-object-detection.errors :as err]))

(defonce config-data (load-file "resources/imagga-config.edn"))
(defonce api-key (:api-key config-data))
(defonce api-secret (:api-secret config-data))

(def base-url "https://api.imagga.com/v2/tags")

(defn- raise-obj-found
  "Raise the object detected to the tag key.
   Ex. {:tag {:en \"obj\"}} -> {:tag \"obj\"}"
  [m]
  (update m :tag (fn [x] (:en x))))

(defn- parse-response
  "Parse the stringified json body response of an Imagga object detection
   request and alter the format of the data. Example:
   {:body {\"result\":{\"tags\":[{\"confidence\":100,\"tag\":{\"en\":\"obj\"}}]}}}
   ->
   [{:confidence 100, :object \"obj\"}]"
  [{body :body :as _resp}]
  (let [tags (-> body
                 (json/read-str :key-fn keyword)
                 :result
                 :tags)]
    (mapv #(set/rename-keys (raise-obj-found %) {:tag :object})
          tags)))

(defn- obj-detection-by-img-url!
  "Given a URL, request object detection from Imagga"
  [url]
  (-> (str base-url "?image_url=" url)
      (http/get {:basic-auth [api-key api-secret]})
      parse-response))

(defn- obj-detection-by-img-upload!
  "Use multipart/form-data encoding to pass file contents to Imagga
   for determining objects detected in the image."
  [image-path]
  (parse-response (http/post base-url {:multipart [{:name "image"
                                                    :content (io/file image-path)}]
                                       :basic-auth [api-key api-secret]})))

(defn object-detection!
  "Determine method to call Imagga to retrieve object detection results.
   Handle errors from Imagga by forwarding along status code and error message."
  [image-url image-file-path]
  (try
    (if image-url
      (obj-detection-by-img-url! image-url)
      (obj-detection-by-img-upload! image-file-path))
    (catch Exception e
      (throw (ex-info (format err/downstream-imagga-err
                              (-> (ex-data e)
                                  :body
                                  (json/read-str :key-fn keyword)
                                  :status
                                  :text))
                      {:status-code (:status (ex-data e))})))))
