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
    (mh/subscribe c ["mh.topic"] (fn [^String topic meta ^bytes payload]
                                   (.incrementAndGet i)))
    (is (mh/connected? c))
    (dotimes [_ 100]
      (mh/publish c "mh.topic" "payload"))
    (Thread/sleep 100)
    (is (= 100 (.get i)))
    (mh/disconnect c)))

(deftest test-topic-subscription-with-multi-segment-wildcard
  (let [id (mh/generate-id)
        c  (mh/connect "tcp://127.0.0.1:1883" id)
        i  (AtomicInteger.)]
    (mh/subscribe c ["mh/topics/#"] (fn [^String topic meta ^bytes payload]
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
    (mh/subscribe c ["mh/topics/+"] (fn [^String topic meta ^bytes payload]
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
    (mh/subscribe c ["mh.topic1" "mh.topic2"] (fn [^String topic meta ^bytes payload]
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
    (mh/subscribe c ["mh.topic"] (fn [^String topic meta ^bytes payload]
                                   (.incrementAndGet i))
                  {:qos 1})
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
    (mh/subscribe c ["mh.topic1" "mh.topic3"] (fn [^String topic meta ^bytes payload]
                                                (.incrementAndGet i))
                  {:qos [1 2]})
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
    (mh/subscribe c ["mh.temp-topic"] (fn [^String topic meta ^bytes payload]
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
    (mh/subscribe c ["mh.temp-topic1"
                     "mh.temp-topic2"
                     "mh.temp-topic3"] (fn [^String topic meta ^bytes payload]
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

(when-not ci?
  (deftest test-publishing-messages-with-qos-2
    (let [c (mh/connect "tcp://127.0.0.1:1883" (mh/generate-id))]
      (is (mh/connected? c))
      (dotimes [i 1000]
        (mh/publish c "mh.qos.topic1" "hello" 2))
      (mh/disconnect c))))
