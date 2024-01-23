(ns image-object-detection.core
  (:gen-class)
  (:require [clojure.data.json :as json]
            [clojure.tools.logging :as log]
            [clojure.walk :as walk]
            [compojure.core :as compojure]
            [compojure.handler :as handler]
            [compojure.route :as route]
            [image-object-detection.get :as get]
            [image-object-detection.post :as post]
            [ring.adapter.jetty :as jetty]
            [ring.middleware.json :refer [wrap-json-body wrap-json-response]]
            [ring.util.response :refer [response]]))

(compojure/defroutes app-routes
  (compojure/GET "/" [] {:status 200
                         :body "Image Uploader UI WIP"
                         :headers {"Content-Type" "text/plain"}})
  (compojure/GET "/images" req (response (get/images! (walk/keywordize-keys req))))
  (compojure/GET "/images/:id" [id] (response (get/image-by-id! id)))
  (compojure/POST "/images" req (response (post/image! (walk/keywordize-keys (:body req)))))
  (route/not-found {:status 404
                    :body (json/write-str {:success false :message "Route not found"})
                    :headers {"Content-Type" "application/json"}}))

(defn- wrap-exceptions!
  "Wrapper to return correct error status code and a meaningful message as json.
   500 Internal Error for errors not marked with a status-code."
  [handler]
  (fn [request]
    (try
      (handler request)
      (catch Exception e
        (if (:status-code (ex-data e))
          {:status (:status-code (ex-data e))
           :body (json/write-str {:success false :message (ex-message e)})
           :headers {"Content-Type" "application/json"}}
          (do
            ;; Log unexpected error, but don't leak it to caller
            (log/error (ex-message e))
            {:status 500
             :body (json/write-str {:success false :message "Internal Error"})
             :headers {"Content-Type" "application/json"}}))))))

(def app
  (-> (handler/api app-routes)
      (wrap-json-body)
      (wrap-json-response)
      (wrap-exceptions!)))

(defonce server (atom nil))

(defn start-server []
  (reset! server
         (jetty/run-jetty (fn [req] (app req)) ;; we pass in a function that calls app
                          {:port 3001
                           :join? false})))

(defn -main [& _args]
  (start-server))
