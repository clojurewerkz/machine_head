(ns clojurewerkz.machine-head.client
  (:require [clojurewerkz.machine-head.conversion :as cnv]
            [clojurewerkz.support.bytes :refer [to-byte-array]])
  (:import [org.eclipse.paho.client.mqttv3
            IMqttClient MqttClient
            MqttCallback MqttMessage IMqttDeliveryToken]))

(defn ^IMqttClient prepare
  "Instantiates a new client"
  [^String uri ^String client-id]
  (MqttClient. uri client-id))

(defn ^IMqttClient connect
  "Instantiates a new client and connects to MQTT broker."
  ([^String uri ^String client-id]
     (doto (prepare uri client-id)
       .connect))
  ([^String uri ^String client-id opts]
     (doto (prepare uri client-id)
       (.connect (cnv/->connect-options opts)))))

(defn disconnect
  "Disconnects from MQTT broker."
  ([^IMqttClient client]
     (.disconnect client))
  ([^IMqttClient client ^long timeout]
     (.disconnect client timeout)))

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
     (.publish client topic (cnv/->message payload)))
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
                          to broker is lost"
  ([^IMqttClient client topics handler-fn]
     (subscribe client topics handler-fn {}))
  ([^IMqttClient client topics handler-fn {:keys [on-connection-lost
                                                  on-delivery-complete]}]
     (let [cb (reify-mqtt-callback handler-fn on-delivery-complete on-connection-lost)]
       (.setCallback client cb)
       (.subscribe client (cnv/->topic-array topics))
       client)))

(defn subscribe-with-qos
  "Subscribes to one or multiple topics (if `topics` is a collection
   or sequence) with provided QoS level(s).

   QoS level must be either an int (from 0 to 2) or a collection of ints from.

   Provided handler function will be invoked with 3 arguments:

    * Topic message was received on
    * Immutable map of message metadata
    * Byte array of message payload

   Options:

    * :on-delivery-complete:
    * :on-connection-lost: function that will be called when connection
                          to broker is lost"
  ([^IMqttClient client topics qos handler-fn]
     (subscribe-with-qos client topics qos handler-fn {}))
  ([^IMqttClient client topics qos handler-fn {:keys [on-connection-lost
                                                      on-delivery-complete]}]
     (let [cb (reify-mqtt-callback handler-fn on-delivery-complete on-connection-lost)]
       (.setCallback client cb)
       (.subscribe client (cnv/->topic-array topics) (cnv/->int-array qos))
       client)))

(defn unsubscribe
  "Unsubscribes from one or multiple topics (if `topics` is a collection
   or sequence)"
  ([^IMqttClient client topics]
     (.unsubscribe client (cnv/->topic-array topics))))
