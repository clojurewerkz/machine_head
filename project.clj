(defproject clojurewerkz/machine_head "1.1.0"
  :description "Clojure MQTT client"
  :dependencies [[org.clojure/clojure          "1.8.0"]
                 [org.eclipse.paho/org.eclipse.paho.client.mqttv3 "1.2.5"]
                 [clojurewerkz/support         "1.5.0"]
                 [clojurewerkz/propertied      "1.3.0"]]
  :profiles {:1.7 {:dependencies [[org.clojure/clojure "1.7.0"]]}
             :1.8 {:dependencies [[org.clojure/clojure "1.8.0"]]}
             :1.9 {:dependencies [[org.clojure/clojure "1.9.0"]]}
             :1.10 {:dependencies [[org.clojure/clojure "1.10.3"]]}
             :1.11 {:dependencies [[org.clojure/clojure "1.11.1"]]}
             :master {:dependencies [[org.clojure/clojure "1.12.0-master-SNAPSHOT"]]}
             :dev {:resource-paths ["test/resources"]
                   :plugins [[codox/codox "0.10.8"]]
                   :codox {:sources ["src/clojure"]
                           :output-dir "doc/api"}}}
  :aliases {"all" ["with-profile" "dev:dev,1.7:dev,1.8:dev,1.9:dev,1.10:dev,1.11:dev,master"]}
  :repositories {"eclipse-paho" {:url "https://repo.eclipse.org/content/repositories/paho-releases/"
                                 :snapshots false
                                 :releases {:checksum :fail}}
                 "sonatype" {:url "https://oss.sonatype.org/content/repositories/releases"
                             :snapshots false
                             :releases {:checksum :fail :update :always}}
                 "sonatype-snapshots" {:url "https://oss.sonatype.org/content/repositories/snapshots"
                                       :snapshots true
                                       :releases {:checksum :fail :update :always}}}
  :javac-options      ["-target" "1.6" "-source" "1.6"]
  :jvm-opts           ["-Dfile.encoding=utf-8"]
  :source-paths       ["src/clojure"]
  :java-source-paths  ["src/java"])
