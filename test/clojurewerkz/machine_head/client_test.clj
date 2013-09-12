(ns clojurewerkz.machine-head.client-test
  (:require [clojurewerkz.machine-head.client :as mh]
            [clojure.test :refer :all]))


(deftest test-connection
  (dotimes [i 50]
    (let [id (format "mh.tests-%d" i)
          c  (mh/connect "tcp://127.0.0.1:1883" "mh.tests-1")]
      (mh/disconnect c))))
