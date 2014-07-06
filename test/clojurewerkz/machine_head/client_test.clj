(ns clojurewerkz.machine-head.client-test
  (:require [clojurewerkz.machine-head.client     :as mh]
            [clojurewerkz.machine-head.durability :as md]
            [clojure.test :refer :all])
  (:import java.util.concurrent.atomic.AtomicInteger
           org.eclipse.paho.client.mqttv3.persist.MemoryPersistence))

(def ci? (System/getenv "CI"))

(deftest test-connection
  (dotimes [i 50]
    (let [id (format "mh.tests-%d" i)
          c  (mh/connect "tcp://127.0.0.1:1883" id)]
      (is (mh/connected? c))
      (mh/disconnect-and-close c))))

(deftest test-connection-with-provided-persister
  (dotimes [i 10]
    (let [id  (format "mh.tests-%d" i)
          p   (md/new-memory-persister)
          c   (mh/connect "tcp://127.0.0.1:1883" id p)]
      (is (mh/connected? c))
      (mh/disconnect c))))

(deftest test-connection-with-clean-session
  (dotimes [i 50]
    (let [id (format "mh.tests-%d" i)
          c  (mh/connect "tcp://127.0.0.1:1883" id {:clean-session true})]
      (is (mh/connected? c))
      (mh/disconnect-and-close c))))

(deftest test-connection-with-connection-timeout
  (dotimes [i 50]
    (let [id (format "mh.tests-%d" i)
          c  (mh/connect "tcp://127.0.0.1:1883" id {:connection-timeout 10})]
      (is (mh/connected? c))
      (mh/disconnect-and-close c))))

(deftest test-connection-with-last-will
  (dotimes [i 50]
    (let [id (format "mh.tests-%d" i)
          w  {:topic "lw-topic" :payload (.getBytes "last will") :qos 0 :retain false}
          c  (mh/connect "tcp://127.0.0.1:1883" id {:clean-session true :will w})]
      (is (mh/connected? c))
      (mh/disconnect-and-close c))))


(deftest test-publishing-empty-messages
  (let [c (mh/connect "tcp://127.0.0.1:1883" "mh.tests-1")]
    (is (mh/connected? c))
    (dotimes [i 1000]
      (mh/publish c "mh.topic1" nil))
    (mh/disconnect c)))

(deftest test-publishing-messages
  (let [c (mh/connect "tcp://127.0.0.1:1883" "mh.tests-1")]
    (is (mh/connected? c))
    (dotimes [i 1000]
      (mh/publish c "mh.topic1" "hello"))
    (mh/disconnect c)))

(deftest test-basic-topic-subscription
  (let [id (mh/generate-id)
        c  (mh/connect "tcp://127.0.0.1:1883" id)
        i  (AtomicInteger.)]
    (mh/subscribe c {"mh.topic" 0} (fn [^String topic meta ^bytes payload]
                                     (.incrementAndGet i)))
    (is (mh/connected? c))
    (dotimes [_ 100]
      (mh/publish c "mh.topic" "payload"))
    (Thread/sleep 100)
    (is (= 100 (.get i)))
    (mh/disconnect c)))

(deftest test-basic-topic-subscription-with-multiple-consumers
  (let [c1 (mh/connect "tcp://127.0.0.1:1883" (mh/generate-id))
        c2 (mh/connect "tcp://127.0.0.1:1883" (mh/generate-id))
        i  (AtomicInteger.)
        f  (fn [^String topic meta ^bytes payload]
             (.incrementAndGet i))
        m  {"mh.topic" 0}]
    (mh/subscribe c1 m f)
    (mh/subscribe c2 m f)
    (dotimes [_ 100]
      (mh/publish c1 "mh.topic" "payload"))
    (Thread/sleep 250)
    (is (= 200 (.get i)))
    (mh/disconnect c1)
    (mh/disconnect c2)))


(deftest test-topic-subscription-with-multi-segment-wildcard
  (let [id (mh/generate-id)
        c  (mh/connect "tcp://127.0.0.1:1883" id)
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
  (let [id (mh/generate-id)
        c  (mh/connect "tcp://127.0.0.1:1883" id)
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
  (let [id (mh/generate-id)
        c  (mh/connect "tcp://127.0.0.1:1883" id)
        i  (AtomicInteger.)]
    (mh/subscribe c {"mh.topic1" 0 "mh.topic2" 0}
                  (fn [^String topic meta ^bytes payload]
                    (.incrementAndGet i)))
    (is (mh/connected? c))
    (dotimes [_ 50]
      (mh/publish c "mh.topic1" "payload"))
    (dotimes [_ 60]
      (mh/publish c "mh.topic2" "payload"))
    (Thread/sleep 100)
    (is (= 110 (.get i)))
    (mh/disconnect c)))

;; very simplistic test, does not try to demonstrate
;; how QoS actually works. MK.
(deftest test-basic-topic-subscription-with-qos
  (let [id (mh/generate-id)
        c  (mh/connect "tcp://127.0.0.1:1883" id)
        i  (AtomicInteger.)]
    (mh/subscribe c {"mh.topic" 1}
                  (fn [^String topic meta ^bytes payload]
                    (.incrementAndGet i)))
    (is (mh/connected? c))
    (dotimes [_ 100]
      (mh/publish c "mh.topic" "payload"))
    (Thread/sleep 100)
    (is (= 100 (.get i)))
    (mh/disconnect c)))


(deftest test-multi-topic-subscription-with-qos
  (let [id (mh/generate-id)
        c  (mh/connect "tcp://127.0.0.1:1883" id)
        i  (AtomicInteger.)]
    (mh/subscribe c {"mh.topic1" 0 "mh.topic3" 1}
                  (fn [^String topic meta ^bytes payload]
                    (.incrementAndGet i)))
    (is (mh/connected? c))
    (dotimes [_ 100]
      (mh/publish c "mh.topic1" "payload"))
    (dotimes [_ 100]
      (mh/publish c "mh.topic2" "payload"))
    (dotimes [_ 100]
      (mh/publish c "mh.topic3" "payload"))
    (Thread/sleep 100)
    (is (= 200 (.get i)))
    (mh/disconnect c)))

(deftest test-basic-topic-unsubscription
  (let [id (mh/generate-id)
        c  (mh/connect "tcp://127.0.0.1:1883" id)
        i  (AtomicInteger.)]
    (mh/subscribe c {"mh.temp-topic" 0}
                  (fn [^String topic meta ^bytes payload]
                    (.incrementAndGet i)))
    (mh/unsubscribe c ["mh.temp-topic"])
    (is (mh/connected? c))
    (dotimes [_ 100]
      (mh/publish c "mh.temp-topic" "payload"))
    (is (= 0 (.get i)))
    (mh/disconnect c)))


(deftest test-multi-topic-unsubscription
  (let [id (mh/generate-id)
        c  (mh/connect "tcp://127.0.0.1:1883" id)
        i  (AtomicInteger.)]
    (mh/subscribe c {"mh.temp-topic1" 0
                     "mh.temp-topic2" 0
                     "mh.temp-topic3" 0}
                     (fn [^String topic meta ^bytes payload]
                       (.incrementAndGet i)))
    (mh/unsubscribe c ["mh.temp-topic1"
                       "mh.temp-topic2"])
    (is (mh/connected? c))
    (dotimes [_ 10]
      (mh/publish c "mh.temp-topic1" "payload"))
    (dotimes [_ 10]
      (mh/publish c "mh.temp-topic2" "payload"))
    (dotimes [_ 10]
      (mh/publish c "mh.temp-topic3" "payload"))
    (Thread/sleep 50)
    (is (= 10 (.get i)))
    (mh/disconnect c)))

;; does not demonstrate how QoS actually works. MK.
(deftest test-publishing-messages-with-qos-0
  (let [c (mh/connect "tcp://127.0.0.1:1883" (mh/generate-id))]
    (is (mh/connected? c))
    (dotimes [i 1000]
      (mh/publish c "mh.qos.topic1" "hello" 0))
    (mh/disconnect c)))

(deftest test-publishing-messages-with-qos-1
  (let [c (mh/connect "tcp://127.0.0.1:1883" (mh/generate-id))]
    (is (mh/connected? c))
    (dotimes [i 1000]
      (mh/publish c "mh.qos.topic1" "hello" 1))
    (mh/disconnect c)))
