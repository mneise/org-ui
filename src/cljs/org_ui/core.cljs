(ns org-ui.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [chord.client :refer [ws-ch]]
            [cljs.core.async :refer [<! >! put! close! chan]]
            [sablono.core :as html :refer-macros [html]]
            [cljs-uuid-utils :refer [make-random-uuid uuid-string]]))

(enable-console-print!)

(defonce app-state (atom {}))

(defn uuid []
  (uuid-string (make-random-uuid)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; WebSocket

(def server-url "ws://192.168.1.6:3034")
(def new-msg-ch (chan))

(defn send-msg! [msg]
  (go
    (>! new-msg-ch msg)))

(defn send-loop [server-ch]
  (go-loop []
    (when-let [msg (<! new-msg-ch)]
      (>! server-ch msg)
      (recur))))

(defn receive-msg! [server-ch]
  (go-loop []
    (when-let [msg (<! server-ch)]
      (println msg)
      (recur))))

(defn setup-connection []
  (go
    (let [{:keys [ws-channel error]} (<! (ws-ch server-url
                                                {:format :json}))]
      (def server-ch ws-channel)
      (receive-msg! server-ch)
      (send-loop server-ch))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Views

(defn application [cursor owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (send-msg! {"id" (uuid)
                  "command" "list"}))
    om/IRender
    (render [_]
      (html
       [:div.container
        [:div.page-header
         [:h1 "Org UI"]]]))))

(defn main []
  (setup-connection)
  (om/root
   application
   app-state
   {:target (. js/document (getElementById "app"))}))
