(ns clojurewerkz.machine-head.client-test
  (:require [clojurewerkz.machine-head.client :as mh]
            [clojure.test :refer :all])
  (:import java.util.concurrent.atomic.AtomicInteger))


(deftest test-connection
  (dotimes [i 50]
    (let [id (format "mh.tests-%d" i)
          c  (mh/connect "tcp://127.0.0.1:1883" id)]
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
    (mh/subscribe-with-qos c ["mh.topic"] 1 (fn [^String topic meta ^bytes payload]
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
    (mh/subscribe-with-qos c ["mh.topic1" "mh.topic3"] [1 2] (fn [^String topic meta ^bytes payload]
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
    (is (= 10 (.get i)))
    (mh/disconnect c)))

;; does not demonstrate how QoS actually works. MK.
(deftest test-publishing-messages-with-qos
  (let [c (mh/connect "tcp://127.0.0.1:1883" (mh/generate-id))]
    (is (mh/connected? c))
    (dotimes [i 1000]
      (mh/publish c "mh.qos.topic1" "hello" 1))
    (mh/disconnect c)))
