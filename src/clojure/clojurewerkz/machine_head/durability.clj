(ns clojurewerkz.machine-head.durability
  "Functions that provide message persister (durable write-ahead log)
   implementations"
  (:import [org.eclipse.paho.client.mqttv3.persist
            MemoryPersistence MqttDefaultFilePersistence]
           org.eclipse.paho.client.mqttv3.MqttClientPersistence))

;;
;; API
;;

(defn ^MqttClientPersistence new-memory-persister
  "Returns new in-memory persister"
  []
  (MemoryPersistence.))

(defn new-file-persister
  "Returns new file-based persister"
  (^MqttClientPersistence []
     (MqttDefaultFilePersistence.))
  (^MqttClientPersistence [^String dir]
     (MqttDefaultFilePersistence. dir)))
