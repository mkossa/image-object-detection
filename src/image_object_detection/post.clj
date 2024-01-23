(ns image-object-detection.post
  (:require [clojure.java.io :as io]
            [image-object-detection.db :as db]
            [image-object-detection.errors :as err]
            [image-object-detection.imagga :as imagga]
            [next.jdbc :as jdbc]))

(defn- insert-image-sql
  "Return sql for inserting an image object into the db."
  [image]
  {:insert-into [:image]
   :values [image]
   :returning [:*]})

(defn- insert-obj-detected-sql
  "Return sql for inserting the objects detected in an image into the db."
  [image_id objects-detected]
  {:insert-into [:image_object_detection]
   :values (mapv #(hash-map :image_id image_id
                            :confidence (:confidence %)
                            :object-detected (:object %))
                 objects-detected)})

(defn- save-to-db!
  "Using a transaction, insert an image into the image table.
   If objects were detected, insert them into the image_object_detection table."
  [image objects-detected]
  (jdbc/with-transaction [tx db/datasource]
    (let [{id :id :as image-metadata} (db/next-execute-one! tx (insert-image-sql image))]
      (when (seq objects-detected)
        (db/next-execute! tx (insert-obj-detected-sql id objects-detected)))
      image-metadata)))    

(defn- validate-request!
  "Validate that either an `image_url` or `image_file` was provided.
   Throw an error if both or neither are provided."
  [{image-url :image_url
    image-file :image_file :as _req_body}]
  (cond
    (and image-url image-file)
    (throw (ex-info err/url-and-path-provided {:status-code 400}))
    ,
    (not (or image-url image-file))
    (throw (ex-info err/neither-url-or-path-provided {:status-code 400}))
    ,
    (and image-file (not (.exists (io/file image-file))))
    (throw (ex-info (format err/file-does-not-exist image-file) {:status-code 400}))
    ,
    :else
    nil))

(defn image!
  "Provided an `image_url` or `image_file`, store metadata about the image in a db.
   Optionally a `label` can be provided, or a random one will be generated.
   Optionally a `detect_objects` parameter can be provided to perform object detection
   on the image using the Imagga API. Object detection results will be stored in the db."
  [{image-url :image_url
    image-file :image_file
    label :label
    object-detection? :detect_objects
    :or {object-detection? false
         label (str "label-" (subs (str (java.util.UUID/randomUUID)) 0 8))}
    :as req_body}]
  (validate-request! req_body)
  (let [objects-detected (when object-detection? (imagga/object-detection! image-url image-file))]
    (merge (save-to-db! {:label label :image_location (or image-url image-file)} objects-detected)
           (when objects-detected {:objects_detected objects-detected}))))
