(ns clojurewerkz.machine-head.client-test
  "BEWARE: Start RabbitMQ before running the rests, either locally or via Docker
   (docker-compose build; docker-compose up)
  "
  (:require [clojurewerkz.machine-head.client     :as mh]
            [clojurewerkz.machine-head.durability :as md]
            [clojure.test :refer :all])
  (:import java.util.concurrent.atomic.AtomicInteger
           java.util.concurrent.CountDownLatch
           (org.eclipse.paho.client.mqttv3 MqttConnectOptions)
           (javax.net SocketFactory)
           (java.util.concurrent TimeUnit)))

(def ci? (System/getenv "CI"))

(deftest test-connection
  (dotimes [i 50]
    (let [id (format "mh/tests-%d" i)
          c  (mh/connect "tcp://127.0.0.1:1883")]
      (is (mh/connected? c))
      (mh/disconnect-and-close c))))

(deftest test-connection-with-provided-persister
  (dotimes [i 10]
    (let [id  (format "mh/tests-%d" i)
          p   (md/new-memory-persister)
          c   (mh/connect "tcp://127.0.0.1:1883" {:client-id id :persister p })]
      (is (mh/connected? c))
      (mh/disconnect c))))

(deftest test-connection-with-provided-options
  (dotimes [i 1]
    (let [called? (atom false)
          id  (format "mh/tests-%d" i)
          opt   (proxy
                  [MqttConnectOptions] []
                  (getConnectionTimeout []
                    (reset! called? true)
                    (proxy-super getConnectionTimeout)))
          c   (mh/connect "tcp://127.0.0.1:1883" {:client-id id :opts opt})]
      (is (mh/connected? c))
      (is (true? @called?))
      (mh/disconnect-and-close c))))

(deftest test-connection-with-clean-session
  (dotimes [i 50]
    (let [id (format "mh/tests-%d" i)
          c  (mh/connect "tcp://127.0.0.1:1883" {:client-id id :opts {:clean-session true}})]
      (is (mh/connected? c))
      (mh/disconnect-and-close c))))

(deftest test-connection-with-connection-timeout
  (dotimes [i 50]
    (let [id (format "mh/tests-%d" i)
          c  (mh/connect "tcp://127.0.0.1:1883" {:client-id id :opts {:connection-timeout 10}})]
      (is (mh/connected? c))
      (mh/disconnect-and-close c))))

(deftest test-connection-with-last-will
  (dotimes [i 50]
    (let [id (format "mh/tests-%d" i)
          w  {:topic "lw-topic" :payload (.getBytes "last will") :qos 0 :retain false}
          c  (mh/connect "tcp://127.0.0.1:1883" {:client-id id :opts {:clean-session true :will w}})]
      (is (mh/connected? c))
      (mh/disconnect-and-close c))))

(deftest test-connection-with-socket-factory
  (dotimes [i 1]
    (let [called? (atom false)
          id  (format "mh/tests-%d" i)
          default (SocketFactory/getDefault)
          f   (proxy [SocketFactory] []
                  (createSocket
                    ([]
                     (reset! called? true) (.createSocket default))
                    ([host port]
                     (reset! called? true) (.createSocket default host port))
                    ([address port localAddress localPort]
                     (reset! called? true) (.createSocket default address port localAddress localPort))
                    ))
          c   (mh/connect "tcp://127.0.0.1:1883" {:client-id id :opts {:socket-factory f}})]
      (is (mh/connected? c))
      (is (true? @called?))
      (mh/disconnect-and-close c))))

(deftest test-connection-with-handlers
  (dotimes [_ 50]
    (let [done (CountDownLatch. 3)
          calls-log (atom {})
          log-call (fn [key]
                     (swap! calls-log assoc key true)
                     (.countDown done))
          c  (mh/connect
               "tcp://127.0.0.1:1883"
               {:on-connect-complete (fn [client reconnect uri] (log-call :on-connect-complete))
                ;:on-connection-lost (fn [cause] (swap! calls-log assoc :on-connection-lost true)) ;; cannot be tested unless we can simulate conn. loss
                :on-delivery-complete (fn [token] (log-call :on-delivery-complete))
                :on-unhandled-message (fn [topic meta payload] (log-call :on-unhandled-message))})]
      ; Subscribe to a topic but do not provide any handler so that the
      ; default one will be invoked:
      (.subscribe c "mh/topicWithoutHandler" 0)
      (mh/publish c "mh/topicWithoutHandler" "hello!")
      (.await done 1000 TimeUnit/MILLISECONDS)
      (is (=
            {:on-connect-complete true
             :on-delivery-complete true
             :on-unhandled-message true}
            @calls-log))
      (mh/disconnect-and-close c))))

(deftest test-publishing-empty-messages
  (let [c (mh/connect "tcp://127.0.0.1:1883")]
    (is (mh/connected? c))
    (dotimes [i 1000]
      (mh/publish c "mh/topic1" nil))
    (mh/disconnect c)))

(deftest test-publishing-messages
  (let [c (mh/connect "tcp://127.0.0.1:1883")]
    (is (mh/connected? c))
    (dotimes [i 1000]
      (mh/publish c "mh/topic1" "hello"))
    (mh/disconnect c)))

(deftest test-basic-topic-subscription
  (let [c  (mh/connect "tcp://127.0.0.1:1883")
        i  (AtomicInteger.)]
    (mh/subscribe c {"mh/topic" 0} (fn [^String topic meta ^bytes payload]
                                     (.incrementAndGet i)))
    (is (mh/connected? c))
    (dotimes [_ 100]
      (mh/publish c "mh/topic" "payload"))
    (Thread/sleep 100)
    (is (= 100 (.get i)))
    (mh/disconnect c)))

(deftest test-basic-topic-subscription-with-multiple-consumers
  (let [c1 (mh/connect "tcp://127.0.0.1:1883")
        c2 (mh/connect "tcp://127.0.0.1:1883")
        i  (AtomicInteger.)
        f  (fn [^String topic meta ^bytes payload]
             (.incrementAndGet i))
        m  {"mh/topic" 0}]
    (mh/subscribe c1 m f)
    (mh/subscribe c2 m f)
    (dotimes [_ 100]
      (mh/publish c1 "mh/topic" "payload"))
    (Thread/sleep 250)
    (is (= 200 (.get i)))
    (mh/disconnect c1)
    (mh/disconnect c2)))


(deftest test-topic-subscription-with-multi-segment-wildcard
  (let [c  (mh/connect "tcp://127.0.0.1:1883")
        i  (AtomicInteger.)]
    (mh/subscribe c {"mh/topics/#" 0}
                  (fn [^String topic meta ^bytes payload]
                    (.incrementAndGet i)))
    (is (mh/connected? c))
    (dotimes [_ 100]
      (mh/publish c "mh/topics/a/b/c" "payload"))
    (Thread/sleep 100)
    (is (= 100 (.get i)))
    (mh/disconnect c)))

(deftest test-topic-subscription-with-single-segment-wildcard
  (let [c  (mh/connect "tcp://127.0.0.1:1883")
        i  (AtomicInteger.)]
    (mh/subscribe c {"mh/topics/+" 0} (fn [^String topic meta ^bytes payload]
                                        (.incrementAndGet i)))
    (is (mh/connected? c))
    (dotimes [_ 10]
      (mh/publish c "mh/topics/a" "payload"))
    (dotimes [_ 10]
      (mh/publish c "mh/topics/b" "payload"))
    (dotimes [_ 10]
      (mh/publish c "mh/topics/a/b" "payload"))
    (dotimes [_ 10]
      (mh/publish c "mh/topics/a/b/c" "payload"))
    (Thread/sleep 100)
    (is (= 20 (.get i)))
    (mh/disconnect c)))

(deftest test-multi-topic-subscription
  (let [c  (mh/connect "tcp://127.0.0.1:1883")
        i  (AtomicInteger.)]
    (mh/subscribe c {"mh/topic1" 0 "mh/topic2" 0}
                  (fn [^String topic meta ^bytes payload]
                    (.incrementAndGet i)))
    (is (mh/connected? c))
    (dotimes [_ 50]
      (mh/publish c "mh/topic1" "payload"))
    (dotimes [_ 60]
      (mh/publish c "mh/topic2" "payload"))
    (Thread/sleep 100)
    (is (= 110 (.get i)))
    (mh/disconnect c)))

(deftest test-different-subscriptions-different-handlers
  (let [c  (mh/connect "tcp://127.0.0.1:1883")
        countDownOne (CountDownLatch. 50)
        countDownTwo (CountDownLatch. 60)]
    (mh/subscribe c {"mh/topic1" 0}
                  (fn [^String topic meta ^bytes payload]
                    (.countDown countDownOne)))
    (mh/subscribe c {"mh/topic2" 0}
                  (fn [^String topic meta ^bytes payload]
                    (.countDown countDownTwo)))
    (is (mh/connected? c))
    (dotimes [_ 50]
      (mh/publish c "mh/topic1" "payload1"))
    (dotimes [_ 60]
      (mh/publish c "mh/topic2" "payload2"))
    (.await countDownOne 100 TimeUnit/MILLISECONDS)
    (.await countDownTwo 100 TimeUnit/MILLISECONDS)
    (is (= [0 0] [(.getCount countDownOne) (.getCount countDownTwo)]))
    (mh/disconnect c)))

;; very simplistic test, does not try to demonstrate
;; how QoS actually works. MK.
(deftest test-basic-topic-subscription-with-qos
  (let [c  (mh/connect "tcp://127.0.0.1:1883")
        i  (AtomicInteger.)]
    (mh/subscribe c {"mh/topic" 1}
                  (fn [^String topic meta ^bytes payload]
                    (.incrementAndGet i)))
    (is (mh/connected? c))
    (dotimes [_ 100]
      (mh/publish c "mh/topic" "payload"))
    (Thread/sleep 100)
    (is (= 100 (.get i)))
    (mh/disconnect c)))


(deftest test-multi-topic-subscription-with-qos
  (let [c  (mh/connect "tcp://127.0.0.1:1883")
        i  (AtomicInteger.)]
    (mh/subscribe c {"mh/topic1" 0 "mh/topic3" 1}
                  (fn [^String topic meta ^bytes payload]
                    (.incrementAndGet i)))
    (is (mh/connected? c))
    (dotimes [_ 100]
      (mh/publish c "mh/topic1" "payload"))
    (dotimes [_ 100]
      (mh/publish c "mh/topic2" "payload"))
    (dotimes [_ 100]
      (mh/publish c "mh/topic3" "payload"))
    (Thread/sleep 100)
    (is (= 200 (.get i)))
    (mh/disconnect c)))

(deftest test-basic-topic-unsubscription
  (let [c  (mh/connect "tcp://127.0.0.1:1883")
        i  (AtomicInteger.)]
    (mh/subscribe c {"mh/temp-topic" 0}
                  (fn [^String topic meta ^bytes payload]
                    (.incrementAndGet i)))
    (mh/unsubscribe c ["mh/temp-topic"])
    (is (mh/connected? c))
    (dotimes [_ 100]
      (mh/publish c "mh/temp-topic" "payload"))
    (is (= 0 (.get i)))
    (mh/disconnect c)))


(deftest test-multi-topic-unsubscription
  (let [c  (mh/connect "tcp://127.0.0.1:1883")
        i  (AtomicInteger.)]
    (mh/subscribe c {"mh/temp-topic1" 0
                     "mh/temp-topic2" 0
                     "mh/temp-topic3" 0}
                     (fn [^String topic meta ^bytes payload]
                       (.incrementAndGet i)))
    (mh/unsubscribe c ["mh/temp-topic1"
                       "mh/temp-topic2"])
    (is (mh/connected? c))
    (dotimes [_ 10]
      (mh/publish c "mh/temp-topic1" "payload"))
    (dotimes [_ 10]
      (mh/publish c "mh/temp-topic2" "payload"))
    (dotimes [_ 10]
      (mh/publish c "mh/temp-topic3" "payload"))
    (Thread/sleep 50)
    (is (= 10 (.get i)))
    (mh/disconnect c)))

;; does not demonstrate how QoS actually works. MK.
(deftest test-publishing-messages-with-qos-0
  (let [c (mh/connect "tcp://127.0.0.1:1883")]
    (is (mh/connected? c))
    (dotimes [i 1000]
      (mh/publish c "mh/qos.topic1" "hello" 0))
    (mh/disconnect c)))

(deftest test-publishing-messages-with-qos-1
  (let [c (mh/connect "tcp://127.0.0.1:1883")]
    (is (mh/connected? c))
    (dotimes [i 1000]
      (mh/publish c "mh/qos.topic1" "hello" 1))
    (mh/disconnect c)))