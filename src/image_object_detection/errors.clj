(ns image-object-detection.errors)

(def invalid-image-id-type
  "Please provide an integer for the image id.")
(def image-id-not-found
  "Image id not found: %s")
(def file-does-not-exist
  "File does not exist: %s")
(def neither-url-or-path-provided
  "No `image_url` or `image_file` provided. Please provide one or the other.")
(def url-and-path-provided
  "Both `image_url` and `image_file` provided. Please provide one or the other.")
(def downstream-imagga-err
  "Downstream Imagga error: %s")
