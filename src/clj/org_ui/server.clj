(ns org-ui.server
  (:gen-class)
  (:require [clojure.java.io :as io]
            [org-ui.dev :refer [is-dev? inject-devmode-html start-figwheel]]
            [compojure.core :refer [GET defroutes]]
            [compojure.route :refer [resources]]
            [net.cgrand.enlive-html :refer [deftemplate]]
            [ring.middleware.reload :as reload]
            [environ.core :refer [env]]
            [ring.adapter.jetty :refer [run-jetty]]))

(deftemplate page
  (io/resource "index.html") [] [:body] (if is-dev? inject-devmode-html identity))

(defroutes routes
  (resources "/")
  (resources "/react" {:root "react"})
  (GET "/*" req (page)))

(def http-handler
  (if is-dev?
    (reload/wrap-reload routes)
    routes))

(defn run [& [port]]
  (let [server (do
                 (when is-dev? (start-figwheel))
                 (let [port (Integer. (or port (env :port) 10555))]
                   (print "Starting web server on port" port ".\n")
                   (run-jetty http-handler {:port port
                                            :join? false})))]
    server))

(defn -main [& [port]]
  (run port))
