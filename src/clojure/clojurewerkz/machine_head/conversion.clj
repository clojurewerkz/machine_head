(ns clojurewerkz.machine-head.conversion
  "Internal conversion functions that transform
   Clojure data structures to Paho Java client classes"
  (:require [clojurewerkz.support.chars         :refer [to-char-array]]
            [clojurewerkz.support.bytes         :refer [to-byte-array]]
            [clojurewerkz.propertied.properties :refer [map->properties]])
  (:import [org.eclipse.paho.client.mqttv3
            MqttClient MqttConnectOptions MqttMessage]))

(defn ->connect-options
  [m]
  {:pre [(or (map? m) (instance? MqttConnectOptions m))]}
  (if (instance? MqttConnectOptions m)
    m
    (let [o (MqttConnectOptions.)]
      (doseq [[k v] m]
        (condp #(%1 %2) {k v}
          :username            :>> #(.setUserName o %)
          :password            :>> #(.setPassword o (to-char-array %))
          :keep-alive-interval :>> #(.setKeepAliveInterval o %)
          :connection-timeout  :>> #(.setConnectionTimeout o (Integer/valueOf %))
          :clean-session       :>> #(.setCleanSession o %)
          :max-inflight        :>> #(.setMaxInflight o %)
          :socket-factory      :>> #(.setSocketFactory o %)
          :auto-reconnect      :>> #(.setAutomaticReconnect o %)
          :server-uris         :>> #(.setServerURIs o (into-array String %))
          :ssl-properties      :>> #(.setSSLProperties o (map->properties %))
          :custom-websocket-headers :>> #(.setCustomWebSocketHeaders o (map->properties %))
          :mqtt-version        :>> #(.setMqttVersion
                                      o (case %
                                          "3.1"
                                          MqttConnectOptions/MQTT_VERSION_3_1
                                          "3.1.1"
                                          MqttConnectOptions/MQTT_VERSION_3_1_1))
          :will                :>> #(.setWill
                                      ^MqttConnectOptions o
                                      ^String (:topic %)
                                      ^bytes (:payload % (byte-array 0))
                                      (Integer/valueOf (:qos % 0))
                                      ^boolean (:retain % false))))
      o)))

(defprotocol MessageSource
  (^MqttMessage to-message [input] "Instantiates an MQTT message from input"))

(extend-protocol MessageSource
  MqttMessage
  (to-message [input]
    input)

  nil
  (to-message [input]
    (MqttMessage.))

  Object
  (to-message [input]
    (MqttMessage. ^bytes (to-byte-array input))))


(defn message->metadata
  "Produces an immutable map of message metadata (all attributes
   except for payload)"
  [^MqttMessage msg]
  {:retained?  (.isRetained msg)
   :qos        (.getQos msg)
   :duplicate? (.isDuplicate msg)})

(defn ^"[S" ->topic-array
  "Coerces the input to an array of strings
   (topic names)"
  [s]
  (into-array String (if (coll? s)
                       s
                       [s])))

(defn ^"[I" ->int-array
  "Coerces the input to an array of integers"
  [i]
  (int-array (if (coll? i)
               i
               [i])))
