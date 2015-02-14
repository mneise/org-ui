(ns org-ui.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [chord.client :refer [ws-ch]]
            [cljs.core.async :refer [<! >! put! close! chan]]))

(enable-console-print!)

(defonce app-state (atom {:text "Hello Chestnut!"}))

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

(setup-connection)
(send-msg! "Hello Server")

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Views

(defn main []
  (om/root
    (fn [app owner]
      (reify
        om/IRender
        (render [_]
          (dom/h1 nil (:text app)))))
    app-state
    {:target (. js/document (getElementById "app"))}))
