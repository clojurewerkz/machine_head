(ns clojurewerkz.machine-head.client
  "Key MQTT client functions: connection, subscription, publishing"
  (:require [clojurewerkz.machine-head.conversion :as cnv]
            [clojurewerkz.support.bytes :refer [to-byte-array]])
  (:import [org.eclipse.paho.client.mqttv3
            IMqttClient MqttClient MqttCallback
            MqttMessage IMqttDeliveryToken MqttClientPersistence
            IMqttDeliveryToken]))

(defn ^IMqttClient prepare
  "Instantiates a new client"
  ([^String uri ^String client-id]
     (MqttClient. uri client-id))
  ([^String uri ^String client-id ^MqttClientPersistence persister]
     (MqttClient. uri client-id persister)))

(defn ^IMqttClient connect
  "Instantiates a new client and connects to MQTT broker."
  ([^String uri ^String client-id]
     (doto (prepare uri client-id)
       .connect))
  ([^String uri ^String client-id opts]
     (doto (prepare uri client-id)
       (.connect (cnv/->connect-options opts))))
  ([^String uri ^String client-id ^MqttClientPersistence persister opts]
     (doto (prepare uri client-id persister)
       (.connect (cnv/->connect-options opts)))))

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
  "Generates a client id"
  []
  (MqttClient/generateClientId))

(defn connected?
  "Returns true if client is currently connected"
  [^IMqttClient client]
  (.isConnected client))

(defn publish
  "Publishes a message to a topic."
  ([^IMqttClient client ^String topic payload]
     (.publish client topic (cnv/to-message payload)))
  ([^IMqttClient client ^String topic payload qos]
     (.publish client topic ^bytes (to-byte-array payload) qos true))
  ([^IMqttClient client ^String topic payload qos retained?]
     (.publish client topic ^bytes (to-byte-array payload) qos retained?)))

(defn ^:private ^MqttCallback reify-mqtt-callback
  [delivery-fn delivery-complete-fn connection-lost-fn]
  (reify MqttCallback
    (^void messageArrived [this ^String topic ^MqttMessage msg]
      (delivery-fn topic (cnv/message->metadata msg) (.getPayload msg)))
    (^void connectionLost [this ^Throwable reason]
      (when connection-lost-fn
        (connection-lost-fn ^Throwable reason)))
    (^void deliveryComplete [this ^IMqttDeliveryToken token]
      (when delivery-complete-fn
        (delivery-complete-fn ^IMqttDeliveryToken token)))))

(defn subscribe
  "Subscribes to one or multiple topics (if `topics` is a collection
   or sequence).

   Provided handler function will be invoked with 3 arguments:

    * Topic message was received on
    * Immutable map of message metadata
    * Byte array of message payload

   Options:

    * :on-delivery-complete:
    * :on-connection-lost: function that will be called when connection
                          to broker is lost
    * :qos Optional QoS levels for the topic supplied.
           QoS level must be an int (from 0 to 2) or a collection of ints if many topics were given.
  ([^IMqttClient client topics handler-fn]
     (subscribe client topics handler-fn {}))"
  ([^IMqttClient client topics handler-fn {:keys [on-connection-lost
                                                  on-delivery-complete
                                                  qos]}]
     (let [cb (reify-mqtt-callback handler-fn on-delivery-complete on-connection-lost)]
       (.setCallback client cb)
       (if qos
         (.subscribe client (cnv/->topic-array topics) (cnv/->int-array qos))
         (.subscribe client (cnv/->topic-array topics)))
       client)))

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
