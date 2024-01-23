(defproject image-object-detection "0.1.0-SNAPSHOT"
  :description "HTTP REST API for a service that ingests user images, analyzes them for object
                detection, and returns the enhanced content."
  :url "http://localhost:3001/"
  :dependencies [[org.clojure/clojure               "1.11.1"]
                 [org.clojure/data.json             "2.5.0"]
                 [org.clojure/tools.logging         "1.2.4"]
                 [com.github.seancorfield/honeysql  "2.5.1103"]
                 [com.github.seancorfield/next.jdbc "1.3.909"]
                 [org.postgresql/postgresql         "42.7.1"] 
                 [ring/ring-core                    "1.10.0"]
                 [ring/ring-jetty-adapter           "1.10.0"]
                 [ring/ring-json                    "0.5.1"]
                 [compojure                         "1.7.0"]
                 [clj-http                          "3.12.3"]]
  :main ^:skip-aot image-object-detection.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}})
