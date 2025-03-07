(ns telegram-bot
  (:require
   [integrant.core :as ig]
   [long-polling-telegram-bot :refer [long-polling]]
   [taoensso.timbre :as log]
   [telegrambot-lib.core :as tbot]
   [utils :refer [pformat]]))

(defmethod ig/init-key ::msg-handler [_ {:keys [bot courier-chat-id]}]
  (fn [{{{chat-id :id} :chat
         message-id :message_id
         message-text :text
         :as message} :message
        :keys [callback_query]
        :as upd}]
    (if-let [msg (or message (-> callback_query
                                 :message
                                 (assoc :data (:data callback_query))))]
      (do (log/info "Received message")
          (log/info (pformat msg))
          (when (< 0 chat-id)
            (if (= message-text "/start")
              (tbot/send-message bot chat-id "Добро пожаловать! Отправьте любой заказ в сообщении и он будет исполнен!)")
              (do (->> message-id
                       (tbot/forward-message bot courier-chat-id chat-id)
                       :result
                       :message_id
                       (tbot/pin-chat-message bot courier-chat-id))
                  (tbot/send-message bot chat-id "Приём заказов окончен. До встречи в следующем году и ещё раз с праздником!)")
                  #_(tbot/send-message bot chat-id "Ваш заказ принят и скоро будет выполнен!")))))
      (log/error "unexpected message type" (pformat upd)))))

(defmethod ig/halt-key! ::run-client [_ {:keys [thread bot]}]
  (when thread
    (log/info "Stop telegram bot")
    (tbot/delete-webhook bot)
    (.interrupt ^Thread thread)))

(defmethod ig/init-key ::run-client [_ {:keys [bot
                                               url
                                               long-polling-config
                                               msg-handler]}]
  (log/info "Start telegram bot: " (or url long-polling-config))
  (merge
   {:bot bot}
   (if (nil? url)
     {:thread (long-polling bot long-polling-config msg-handler)}
     {:webhook (tbot/set-webhook bot {:url url
                                      :content-type :multipart})})))

(defmethod ig/init-key ::client [_ {:keys [token]}]
  (log/info "Start client-bot")
  (if (nil? token)
    (log/error "No client-bot token")
    (let [bot (tbot/create token)]
      (log/info (tbot/get-me bot))
      bot)))
