(ns clojurewerkz.machine-head.conversion
  "Internal conversion functions that transform
   Clojure data structures to Paho Java client classes"
  (:require [clojurewerkz.support.internal :as i])
  (:import [org.eclipse.paho.client.mqttv3 MqttClient MqttConnectOptions]))

(defprotocol CharSource
  (to-char-array [input] "Converts an input to char[]"))

(extend-protocol CharSource
  String
  (to-char-array [input]
    (.toCharArray input)))

(extend i/char-array-type
  CharSource
  {:to-char-array identity})

(extend nil
  CharSource
  {:to-char-array (constantly nil)})



(defn ->connect-options
  [m]
  (let [o (MqttConnectOptions.)]
    (when-let [u (:username m)]
      (.setUserName o u))
    (when-let [p (to-char-array (:password m))]
      (.setPassword o p))
    (when-let [i (:keep-alive-interval m)]
      (.setKeepAliveInterval o i))
    o))
