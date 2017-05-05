(ns clojurewerkz.machine-head.client
  "Key MQTT client functions: connection, subscription, publishing"
  (:require [clojurewerkz.machine-head.conversion :as cnv]
            [clojurewerkz.support.bytes :refer [to-byte-array]])
  (:import [org.eclipse.paho.client.mqttv3
            IMqttClient MqttClient MqttCallbackExtended
            MqttMessage IMqttDeliveryToken MqttClientPersistence
            IMqttDeliveryToken IMqttMessageListener]))

(declare reify-mqtt-callback)
(declare generate-id)

(defn ^IMqttClient prepare
  "Instantiates a new client"
  [^String uri ^String client-id ^MqttClientPersistence persister]
  (MqttClient. uri client-id persister))

(defn ^IMqttClient connect
  "Instantiates a new client and connects to MQTT broker.

   Options (all keys are optional):

    * :client-id: a client identifier that is unique on the server being connected to
    * :persister: the persistence class to use to store in-flight message; if bil then the
       default persistence mechanism is used
    * :opts: see Mqtt connect options below
    * :on-connect-complete: function called after connection to broker
    * :on-connection-lost: function called when connection to broker is lost
    * :on-delivery-complete: function called when sending and delivery for a message has
       been completed (depending on its QoS), and all acknowledgments have been received
    * :on-unhandled-message: function called when a message has arrived and hasn't been handled
       by a per subscription handler; invoked with 3 arguments:
       the topic message was received on, an immutable map of message metadata, a byte array of message payload

    Mqtt connect options: either a map with any of the keys bellow or an instance of MqttConnectOptions

    * :username (string)
    * :password (string or char array)
    * :auto-reconnect (bool)
    * :connection-timeout (int)
    * :clean-session (bool)
    * :keep-alive-interval (int)
    * :max-inflight (int)
    * :socket-factory (SocketFactory)
    * :will {:topic :payload :qos :retain}
    "
  ([^String uri]
    (connect uri {}))
  ([^String uri
    {:keys [^String client-id
            ^MqttClientPersistence persister
            opts
            on-delivery-complete
            on-connection-lost
            on-connect-complete
            on-unhandled-message]
     :or {client-id (generate-id)
          persister nil
          opts {}}}]
   (let [client (prepare uri client-id persister)
         cb (reify-mqtt-callback
              client
              on-unhandled-message
              on-delivery-complete
              on-connection-lost
              on-connect-complete)]
     (.setCallback client cb)                               ;; we must set the callback BEFORE connecting
     (.connect client (cnv/->connect-options opts))
     client)))

(defn disconnect
  "Disconnects from MQTT broker."
  ([^IMqttClient client]
     (.disconnect client))
  ([^IMqttClient client ^long timeout]
     (.disconnect client timeout)))

(defn disconnect-and-close
  "Disconnects from MQTT broker and releases all resources."
  ([^IMqttClient client]
     (doto client
       .disconnect
       .close))
  ([^IMqttClient client ^long timeout]
     (.disconnect client timeout)
     (.close client)))

(defn ^String generate-id
  "Generates a client id.

   Limits client id to 23 bytes, per MQTT spec"
  []
  (let [id (MqttClient/generateClientId)]
    (apply str (take-last 23 id))))

(defn connected?
  "Returns true if client is currently connected"
  [^IMqttClient client]
  (.isConnected client))

(defn publish
  "Publishes a message to a topic."
  ([^IMqttClient client ^String topic payload]
     (.publish client topic (cnv/to-message payload)))
  ([^IMqttClient client ^String topic payload qos]
     (.publish client topic ^bytes (to-byte-array payload) qos false))
  ([^IMqttClient client ^String topic payload qos retained?]
     (.publish client topic ^bytes (to-byte-array payload) qos retained?)))

(defn ^:private ^MqttCallbackExtended reify-mqtt-callback
  [client message-arrived-fn delivery-complete-fn connection-lost-fn on-connect-complete-fn]
  (reify MqttCallbackExtended
    (^void messageArrived [this ^String topic ^MqttMessage msg]
      (when message-arrived-fn
        (message-arrived-fn topic (cnv/message->metadata msg) (.getPayload msg))))
    (^void connectionLost [this ^Throwable reason]
      (when connection-lost-fn
        (connection-lost-fn ^Throwable reason)))
    (^void connectComplete [this ^boolean reconnect ^String serverURI]
      (when on-connect-complete-fn
        (on-connect-complete-fn client reconnect serverURI)))
    (^void deliveryComplete [this ^IMqttDeliveryToken token]
      (when delivery-complete-fn
        (delivery-complete-fn ^IMqttDeliveryToken token)))))

(defn ^:private ^IMqttMessageListener reify-message-listener
  [delivery-fn]
  (reify IMqttMessageListener
    (^void messageArrived [this ^String topic ^MqttMessage msg]
      (delivery-fn topic (cnv/message->metadata msg) (.getPayload msg)))))

(defn subscribe
  "Subscribes to one or multiple topics (if `topics` is a collection
   or sequence).

   Provided handler function will be invoked with 3 arguments:

    * The topic message was received on
    * Immutable map of message metadata
    * Byte array of message payload

   BEWARE: Don't use dots in topic names with RabbitMQ - see rabbitmq-mqtt/issues/58"
  [^IMqttClient client topics-and-qos handler-fn]
  ;; ensure topics and qos are in the same order,
  ;; even though we do not require the user to pass an
  ;; order-preserving map. MK.
  (let [topics (keys topics-and-qos)
        qos (map (fn [^String s]
                   (get topics-and-qos s))
                 topics)]
    (.subscribe
      client
      (cnv/->topic-array topics)
      (cnv/->int-array qos)
      ;#_^"[Lorg.eclipse.paho.client.mqttv3.IMqttMessageListener;"
      (into-array
        IMqttMessageListener
        (repeat
          (count topics)
          (reify-message-listener handler-fn))))))

(defn unsubscribe
  "Unsubscribes from one or multiple topics (if `topics` is a collection
   or sequence)"
  ([^IMqttClient client topics]
     (.unsubscribe client (cnv/->topic-array topics))))

(defn pending-delivery-tokens
  "Retuns pending message delivery tokens, if any.

   If all messages were published successfully after last
   client termination, returns an empty collection."
  [^IMqttClient client]
  (into [] (.getPendingDeliveryTokens client)))

(defn pending-messages
  "Retuns pending messages, if any.

   If all messages were published successfully after last
   client termination, returns an empty collection."
  [^IMqttClient client]
  (map (fn [^IMqttDeliveryToken t] (.getMessage t)) (pending-delivery-tokens client)))
