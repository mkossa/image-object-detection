(ns image-object-detection.get
  (:require [clojure.string :as str]
            [image-object-detection.db :as db]
            [image-object-detection.errors :as err]))

;; SELECT 
;;   image.*, 
;;   (
;;     COALESCE(
;;       JSON_AGG(
;;         JSON_BUILD_OBJECT(
;;           'confidence', iod.confidence,
;;           'object_detected', iod.object_detected
;;         )
;;       ) FILTER (
;;         WHERE 
;;           iod.confidence IS NOT NULL
;;       ), 
;;       NULL
;;     )
;;   ) AS objects_detected 
;; FROM 
;;   image 
;;   LEFT JOIN image_object_detection AS iod ON image.id = iod.image_id 
;; GROUP BY 
;;   image.id;
(def ^:private get-images-all
  {:select [:image.*
            [[[:coalesce
               [:filter
                [:json_agg
                 [:json_build_object "confidence" :iod.confidence
                                     "object_detected" :iod.object_detected]]
                {:where [:not= :iod.confidence nil]}]
               nil]] :objects_detected]]
   :from [:image]
   :left-join [[:image_object_detection :iod] [:= :image.id :iod.image_id]]
   :group-by [:image.id]})

(defn- get-image-ids-with-object-match-sql
  "Given a vector of objects, return the sql that queries for a list of image ids
   that had any of the objects detected."
  [objects]
  {:select [:image.id]
   :from [:image]
   :join [[:image_object_detection :iod] [:= :image.id :iod.image_id]]
   :where [:in :iod.object_detected objects]})

(defn- get-image-by-ids-sql
  "Return the sql to query for all images and their metadata,
   filtering based on a provided vector of image ids."
  [ids]
  (merge {:where [:in :image.id ids]}
         get-images-all))

(defn- clean-str
  "Remove all double quotes and whitespace around comma-separated string.
   Return the string as a vector of the comma-separated strings.
   Ex. \"  \"dog  \",\"cat\"  \" -> [\"dog\", \"cat\"]"
  [objects]
  (mapv #(str/trim %) (-> objects
                          (str/replace #"\"" "")
                          (str/split #","))))

(defn- coerce-to-int!
  "Check that a provided string can be parsed as an integer.
   Raise an exception if it cannot."
 [id]
 {:pre [(= (type id) java.lang.String)]
  :post [(= (type %) java.lang.Integer)]}
 (try
   (Integer/parseInt id)
   (catch Exception _
     (throw (ex-info err/invalid-image-id-type {:status-code 400})))))

(defn image-by-id!
  "Retrieve a single image and its metadata based on an id."
  [id]
  (let [image (db/next-execute-one! (get-image-by-ids-sql [(coerce-to-int! id)]))]
    (if (nil? image)
      (throw (ex-info (format err/image-id-not-found id) {:status-code 404}))
      image)))

(defn images!
  "Retrieve all images and their metadata.
   Optionally filter images based on the objects detected in them."
  [{{objects :objects} :query-params :as _req}]
  (if objects
    (let [matching-image-ids
          (db/next-execute! (get-image-ids-with-object-match-sql (clean-str objects)))]
      (if (seq matching-image-ids)
        (db/next-execute! (get-image-by-ids-sql (mapv :id matching-image-ids)))
        []))
    (db/next-execute! get-images-all)))
