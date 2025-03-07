(ns http
  (:require
   [cheshire.core :as json]
   [integrant.core :as ig]
   [org.httpkit.server :as hk-server]
   [taoensso.timbre :as log]
   [utils :refer [->num]]))

(defmethod ig/init-key ::handler [_ msg-handler]
  #(try
     (-> %
         :body
         slurp
         (json/parse-string true)
         msg-handler)
     (catch Exception e (log/error (.getMessage e)))
     (finally {:status  200
               :headers {"Content-Type" "text/html"}})))

(defmethod ig/init-key ::server [_ {:keys [handler port]}]
  (log/info "Start http-server on port " port)
  (hk-server/run-server handler {:port (->num port)}))

(defmethod ig/halt-key! ::server [_ server]
  (log/info "Stopping server" server)
  (when server
    (server :timeout 100)))
