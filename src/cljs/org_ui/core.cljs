(ns org-ui.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [chord.client :refer [ws-ch]]
            [cljs.core.async :refer [<! >! put! close! chan]]
            [sablono.core :as html :refer-macros [html]]
            [cljs-uuid-utils :refer [make-random-uuid uuid-string]]))

(enable-console-print!)

(defonce app-state (atom {:title "Org UI"
                          :data {}}))

(defn uuid []
  (uuid-string (make-random-uuid)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; WebSocket

(def server-url "ws://192.168.1.6:3034")
(def new-msg-ch (chan))
(def requests (atom {}))

(defn send-msg! [msg & [handler]]
  (let [uuid (get msg "id")]
    (when handler
      (swap! requests assoc uuid handler))
    (go
      (>! new-msg-ch msg))))

(defn send-loop [server-ch]
  (go-loop []
    (when-let [msg (<! new-msg-ch)]
      (>! server-ch msg)
      (recur))))

(defn receive-msg! [server-ch]
  (go-loop []
    (let [{:keys [message]} (<! server-ch)
          message (clojure.walk/keywordize-keys message)
          {:keys [in-response-to data]} message]
      (when-let [handler (get @requests in-response-to)]
        (handler data)
        (swap! requests dissoc uuid))
      (recur))))

(defn setup-connection []
  (go
    (let [{:keys [ws-channel error]} (<! (ws-ch server-url
                                                {:format :json}))]
      (def server-ch ws-channel)
      (receive-msg! server-ch)
      (send-loop server-ch))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Handler

(defmulti handle-event
  (fn [event msg app owner] event))

(defmethod handle-event :list [_ msg app owner]
  (om/update! app :data msg))

(defmethod handle-event :default [_ msg app owner])

(defn create-handler [event app owner]
  (fn [msg]
    (handle-event event msg app owner)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Views

(defn application [cursor owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (send-msg! {"id" (uuid)
                  "command" "list"} (create-handler :list cursor owner)))
    om/IRender
    (render [_]
      (html
       [:div.container
        [:div.page-header
         [:h1 (:title cursor)]]]))))

(defn main []
  (setup-connection)
  (om/root
   application
   app-state
   {:target (. js/document (getElementById "app"))}))
