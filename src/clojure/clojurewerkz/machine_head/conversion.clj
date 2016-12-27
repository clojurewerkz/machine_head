(ns clojurewerkz.machine-head.conversion
  "Internal conversion functions that transform
   Clojure data structures to Paho Java client classes"
  (:require [clojurewerkz.support.chars :refer [to-char-array]]
            [clojurewerkz.support.bytes :refer [to-byte-array]])
  (:import [org.eclipse.paho.client.mqttv3 MqttClient MqttConnectOptions
            MqttMessage]))

(defn ->connect-options
  [m]
  {:pre [(or (map? m) (instance? MqttConnectOptions m))]}
  (if (instance? MqttConnectOptions m)
    m
    (let [o (MqttConnectOptions.)]
     (when-let [u (:username m)]
       (.setUserName o u))
     (when-let [p (to-char-array (:password m))]
       (.setPassword o p))
     (when-let [i (:keep-alive-interval m)]
       (.setKeepAliveInterval o i))
     (when-let [t (:connection-timeout m)]
       (.setConnectionTimeout o (Integer/valueOf t)))
     (when-not (nil? (:clean-session m))
       (.setCleanSession o (:clean-session m)))
     (when-let [m (:max-inflight m)]
       (.setMaxInflight o m))
     (when-let [f (:socket-factory m)]
       (.setSocketFactory o f))
     (when-let [will (:will m)]
       (.setWill ^MqttConnectOptions o
                 ^String (get will :topic)
                 ^bytes (get will :payload (byte-array 0))
                 (Integer/valueOf (get will :qos 0))
                 ^boolean (get will :retain false)))
     (when-let [r (:auto-reconnect m)]
       (.setAutomaticReconnect o r))
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
