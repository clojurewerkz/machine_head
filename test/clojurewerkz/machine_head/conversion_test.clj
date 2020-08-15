(ns clojurewerkz.machine-head.conversion-test
  (:require [clojurewerkz.machine-head.conversion :refer [->connect-options]]
            [clojurewerkz.propertied.properties   :refer [map->properties
                                                          properties->map]]
            [clojure.test :refer :all])
  (:import (org.eclipse.paho.client.mqttv3 MqttConnectOptions)))

(deftest test-->connect-options
  (let [opts {:username            "foo"
              :password            "bar"
              :keep-alive-interval 100
              :connection-timeout  1000
              :clean-session       true
              :max-inflight        10
              ; :socket-factory    ...
              :auto-reconnect      true
              :server-uris         ["ssl://mqtt.example.com"]
              :ssl-properties      {"com.ibm.ssl.protocol" "SSLv3"}
              :mqtt-version        "3.1.1"
              :will                {:topic   "will"
                                    :qos     0
                                    :retain  true
                                    :payload (.getBytes "argl")}}
        o (->connect-options opts)
        feedback-opts {:username            (.getUserName o)
                       :password            (String. (.getPassword o))
                       :keep-alive-interval (.getKeepAliveInterval o)
                       :connection-timeout  (.getConnectionTimeout o)
                       :clean-session       (.isCleanSession o)
                       :max-inflight        (.getMaxInflight o)
                       ; :socket-factory    ...
                       :auto-reconnect      (.isAutomaticReconnect o)
                       :server-uris         (vec (.getServerURIs o))
                       :ssl-properties      (properties->map (.getSSLProperties o))
                       :mqtt-version        (condp = (.getMqttVersion o)
                                              MqttConnectOptions/MQTT_VERSION_3_1
                                              "3.1"
                                              MqttConnectOptions/MQTT_VERSION_3_1_1
                                              "3.1.1")
                       :will                (let [wm (.getWillMessage o)]
                                              {:topic   (.getWillDestination o)
                                               :qos     (.getQos wm)
                                               :retain  (.isRetained wm)
                                               :payload (.getPayload wm)})}]
    (is (= (update-in opts          [:will :payload] #(String. %))
           (update-in feedback-opts [:will :payload] #(String. %))))))
