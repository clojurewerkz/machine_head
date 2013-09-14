(ns clojurewerkz.machine-head.client
  (:require [clojurewerkz.machine-head.conversion :as cnv])
  (:import [org.eclipse.paho.client.mqttv3 IMqttClient]))

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

(defn connected?
  "Returns true if client is currently connected"
  [^IMqttClient client]
  (.isConnected client))

(defn publish
  "Publishes a message to a topic."
  [^IMqttClient client ^String topic payload]
  (.publish client topic (cnv/->message payload)))
