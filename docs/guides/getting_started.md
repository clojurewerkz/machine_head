---
title: "Getting Started with MQTT and Clojure"
layout: article
---


## About this guide

This guide is a quick tutorial that helps you to get started with the MQTT protocol in general and the Machine Head in particular.
It should take about 15 minutes to read and study the provided code examples. This guide covers:

 * Installing an MQTT broker
 * Adding Machine Head dependency with [Leiningen](http://leiningen.org) or [Maven](http://maven.apache.org/)
 * Running a "Hello, world" messaging example that is a simple demonstration of 1:1 communication.
 * Creating a "Twitter-like" publish/subscribe example with one publisher and four subscribers that demonstrates 1:n communication.
 * Creating a topic routing example with two publishers and eight subscribers showcasing n:m communication when subscribers only receive messages that they are interested in.

This work is licensed under a <a rel="license" href="http://creativecommons.org/licenses/by/3.0/">Creative Commons Attribution 3.0 Unported License</a>
(including images and stylesheets). The source is available [on Github](https://github.com/clojurewerkz/machine_head.docs).


## What version of Machine Head does this guide cover?

This guide covers Machine Head 1.1.0.


## Supported Clojure Versions

Machine Head requires Clojure 1.8+. The latest stable release is recommended.


## Supported MQTT Brokers

Machine Head is tested against [RabbitMQ 3.x MQTT plugin](http://www.rabbitmq.com/mqtt.html) and [Mosquitto](http://mosquitto.org/).


## Overview

Machine Head is a Clojure client for [MQTT](http://mqtt.org/) v3.1 brokers. MQTT is
an efficient messaging protocol designed primarily for low-power devices such as
telemetry sensors.

Clients communicate with MQTT brokers such as RabbitMQ and Mosquitto. Clients that
publish messages are called *producers* or *publishers*, those that consume
messages are *consumers* or *subscribers*. Message distribution happens in
communication points known as *topics*, which filter published messages and
deliver those that match to consumers.

Machine Head is a minimalistic Clojure MQTT client. It is designed with
ease of use and efficiency in mind.


## Installing MQTT Broker

### RabbitMQ

The RabbitMQ site has a good [installation
guide](http://www.rabbitmq.com/install.html) that addresses many
operating systems.  On Mac OS X, the fastest way to install RabbitMQ
is with [Homebrew](http://mxcl.github.com/homebrew/):

    brew install rabbitmq

Next, enable MQTT plugin:

    rabbitmq-plugins enable rabbitmq_mqtt

then run the broker:

    rabbitmq-server

On Debian and Ubuntu, you can either [download the RabbitMQ .deb
package](http://www.rabbitmq.com/server.html) and install it with
[dpkg](http://www.debian.org/doc/FAQ/ch-pkgtools.en.html) or make use
of the [RabbitMQ apt
repository](http://www.rabbitmq.com/debian.html#apt).

For RPM-based distributions like RedHat or CentOS, the RabbitMQ team
provides an [RPM package](http://www.rabbitmq.com/install.html#rpm).

<div class="alert alert-error"><strong>Note:</strong> The RabbitMQ
package that ships with some of the recent Ubuntu versions (for
example, 11.10 and 12.04) is outdated and *may not ship with MQTT
plugin* (you will need at least RabbitMQ v3.0 for use with this
guide).</div>



### Mosquitto

On Mac OS X, the fastest way to install Mosquitto is with
[Homebrew](http://mxcl.github.com/homebrew/):

    brew install mosquitto

then run the broker:

    # alter configuration file path depending on your
    # Homebrew root location
    mosquitto /usr/local/etc/mosquitto/mosquitto.conf


## Adding Machine Head Dependency To Your Project

Machine Head artifacts are [released to Clojars](https://clojars.org/clojurewerkz/machine_head).

### With Leiningen

Add the dependency:

``` clojure
[clojurewerkz/machine_head "1.1.0"]
```

### With Maven

Add Clojars and Eclipse Paho repository definitions to your `pom.xml`:

``` xml
<repository>
  <id>clojars.org</id>
  <url>http://clojars.org/repo</url>
</repository>
<repository>
  <id>eclipse-paho</id>
  <url>https://repo.eclipse.org/content/repositories/paho-releases/</url>
</repository>
```

And then the dependency:

``` xml
<dependency>
  <groupId>clojurewerkz</groupId>
  <artifactId>machine_head</artifactId>
  <version>1.1.0</version>
</dependency>
```

### Verifying Your Installation

You can verify your installation in the REPL:

    $ lein repl
    user=> (require '[clojurewerkz.machine-head.client :as mh])
    ;= nil
    user=> (mh/connect "tcp://127.0.0.1:1883" (mh/generate-id))
    ;= #<MqttClient org.eclipse.paho.client.mqttv3.MqttClient@4819d03a>



## "Hello, World" example

Let us begin with the classic "Hello, world" example. First, here is the code:

``` clojure
(ns clojurewerkz.machine-head.examples.hello-world
  (:gen-class)
  (:require [clojurewerkz.machine-head.client :as mh]))

(defn -main
  [& args]
  (let [conn (mh/connect "tcp://127.0.0.1:1883")]
    (mh/subscribe conn {"hello" 0} (fn [^String topic _ ^bytes payload]
                                   (println (String. payload "UTF-8"))
                                   (mh/disconnect conn)
                                   (System/exit 0)))
    (mh/publish conn "hello" "Hello, world")))
```

This example demonstrates a very common communication scenario:
*application A* wants to publish a message on a topic that
*application B* listens on. In this case, the topic name is
"hello". Let us go through the code step by step:

``` clojure
(ns clojurewerkz.machine-head.examples.hello-world
  (:gen-class)
  (:require [clojurewerkz.machine-head.client :as mh]))
```

defines our example app namespace that requires (loads) main Machine
Head namespace, `clojurewerkz.machine-head.client`. Our namespace will be compiled
ahead-of-time (so we can run it).

Clojure applications are compiled to JVM bytecode. The `-main`
function is the entry point.

A few things is going on here:

 * We connect to MQTT broker using `clojurewerkz.machine-head.client/connect`. We pass one argument
 to it: connection URI.
 * We start a consumer on topic named `"hello"`
 * We publish a message and disconnect when it is consumed

### Connect to MQTT Broker

``` clojure
(let [conn  (mh/connect "tcp://127.0.0.1:1883")]
  (comment ...))
```

connects to MQTT broker such as RabbitMQ at `127.0.0.1:1883` (generating a unique client id so that you can create multiple connections), returning the connection.

`mh` is an alias for `clojurewerkz.machine-head.client` (see the `ns` snippet above).


### Start a Consumer (Subscriber)

Now that we have a connection open, we can start consuming messages on
a topic with QoS level 0:

``` clojure
(mh/subscribe conn {"hello" 0} (fn [^String topic _ ^bytes payload]
                                 (comment ...)))
```

We use `clojurewerkz.machine-head.client/subscribe` to add a consumer (subscription).
Here's the handling function:

``` clojure
(fn [^String topic _ ^bytes payload]
  (println (String. payload "UTF-8"))
  (mh/disconnect conn)
  (System/exit 0))
```

It takes a topic the message is delivered on, a Clojure map of message
metadata and message payload as array of bytes. We turn it into a
string and print it, then disconnect and exit.

It is possible to subscribe to multiple topics at once and to use different QoS for them:

``` clojure
(mh/subscribe conn {"hello" 1 "/another/topic/#" 0} (fn [^String topic _ ^bytes payload]
                                                      (comment ...)))
```

### Publish a Message

To publish a message, we use `clojurewerkz.machine-head.client/publish`,
which takes a connection, a topic and a payload (as a string or byte array):

``` clojure
(mh/publish conn "hello" "Hello, world")
```

### Disconnect

Then we use `clojurewerkz.machine-head.client/disconnect` to close both the connection.

``` clojure
(mh/disconnect conn)
```

For the sake of simplicity, both the message producer (application A) and the
consumer (application B) are running in the same JVM process. Now let us move
on to a little bit more sophisticated example.


## Blabbr: One-to-Many Publish/Subscribe (pubsub) Routing Example

The previous example demonstrated how a connection to a broker is made
and how to do 1:1 communication. Now let us
take a look at another common scenario: broadcast, or multiple
consumers and one producer.

A very well-known broadcast example is Twitter: every time a person
tweets, followers receive a notification. Blabbr, our imaginary
information network, models this scenario: every network member has a
separate queue and publishes blabs to a separate exchange. Three
Blabbr members, Joe, Aaron and Bob, follow the official NBA account on
Blabbr to get updates about what is happening in the world of
basketball. Here is the code:

``` clojure
(ns clojurewerkz.machine-head.examples.blabbr
  (:gen-class)
  (:require [clojurewerkz.machine-head.client :as mh]))

(def ^:const topic "nba/scores")

(defn start-consumer
  [conn ^String username]
  (mh/subscribe conn
                {topic 0}
                (fn [^String topic _ ^bytes payload]
                  (println (format "[consumer] %s received %s" username (String. payload "UTF-8"))))))

(defn -main
  [& args]
  (let [conn  (mh/connect "tcp://127.0.0.1:1883")
        users ["joe" "aaron" "bob"]]
    (doseq [u users]
      (let [c (mh/connect "tcp://127.0.0.1:1883" {:client-id (format "consumer.%s" u)})]
        (start-consumer c u)))
    (mh/publish conn topic "BOS 101, NYK 89")
    (mh/publish conn topic "ORL 85, ALT 88")
    (Thread/sleep 100)
    (mh/disconnect conn)
    (System/exit 0)))
```

In this example, connection is no different to opening a
channel in the previous example:

``` clojure
(let [conn  (mh/connect "tcp://127.0.0.1:1883")]
  (comment ...))
```

This piece of code

``` clojure
(defn start-consumer
  [conn ^String username]
  (mh/subscribe conn
                {topic 0}
                (fn [^String topic _ ^bytes payload]
                  (println (format "[consumer] %s received %s" username (String. payload "UTF-8"))))))

(doseq [u users]
  (let [c (mh/connect "tcp://127.0.0.1:1883" {:client-id (format "consumer.%s" u)})]
    (start-consumer c u)))
```

opens consumer connections (notice that each connection needs a unique `client-id`) and subscribes to 3 topics: `consumer.joe`,
`consumer.aaron`, and `consumer.joe`. We emulate multiple users by connecting multiple
times from the same JVM.


## Weathr: Many-to-Many Topic Routing Example

So far, we have seen point-to-point communication and
broadcasting. Those two communication styles are possible with many
protocols, for instance, HTTP handles these scenarios just fine. Next
we are going to introduce you to *wildcard topics* and subscription
with patterns.

Our third example involves weather condition updates. What makes it
different from the previous two examples is that not all of the
consumers are interested in all of the messages. People who live in
Portland usually do not care about the weather in Hong Kong (unless
they are visiting soon). They are much more interested in weather
conditions around Portland, possibly all of Oregon and sometimes a few
neighbouring states.

Our example features multiple consumer applications monitoring updates
for different regions. Some are interested in updates for a specific
city, others for a specific state and so on, all the way up to
continents. Updates may overlap so that an update for San Diego, CA
appears as an update for California, but also should show up on the
North America updates list.

Here is the code:

```clojure
(ns clojurewerkz.machine-head.examples.weathr
  (:gen-class)
  (:require [clojurewerkz.machine-head.client :as mh]))

(defn handle-delivery
  [^String subscribed-for ^String topic _ ^bytes payload]
  (println
      (format "[consumer of %s] received %s for topic %s"
        subscribed-for
        (String. payload "UTF-8")
        topic)))


(defn -main
  [& args]
  (let [conn  (mh/connect "tcp://127.0.0.1:1883")]
    (mh/subscribe conn {"americas/north/#" 0} (partial handle-delivery "americas/north/#"))
    (mh/subscribe conn {"americas/south/#" 0} (partial handle-delivery "americas/south/#"))
    (mh/subscribe conn {"americas/north/us/ca/+" 0} (partial handle-delivery "americas/north/us/ca/+"))
    (mh/subscribe conn {"europe/italy/rome" 0} (partial handle-delivery "europe/italy/rome"))
    (mh/subscribe conn {"asia/southeast/hk/+" 0} (partial handle-delivery "asia/southeast//hk/+"))
    (mh/subscribe conn {"asia/southeast/#" 0} (partial handle-delivery "asia/southeast/#"))
    (mh/publish conn "americas/north/us/ca/sandiego"     "San Diego update")
    (mh/publish conn "americas/north/us/ca/berkeley"     "Berkeley update")
    (mh/publish conn "americas/north/us/ca/sanfrancisco" "SF update")
    (mh/publish conn "americas/north/us/ny/newyork"      "NYC update")
    (mh/publish conn "americas/south/brazil/saopaolo"    "São Paolo update")
    (mh/publish conn "asia/southeast/hk/hongkong"        "Hong Kong update")
    (mh/publish conn "asia/southeast/japan/kyoto"        "Kyoto update")
    (mh/publish conn "asia/southeast/prc/shanghai"       "Shanghai update")
    (mh/publish conn "europe/italy/rome"                 "Rome update")
    (mh/publish conn "europe/france/paris"               "Paris update")
    (Thread/sleep 150)
    (mh/disconnect conn)
    (System/exit 0)))
```

In this example we use a single connection for publishing and consuming.

Multiple consumers use a single topic in this example. This is an
example of [multicast](http://en.wikipedia.org/wiki/Multicast)
messaging where consumers indicate which topics they are interested in
(think of it as subscribing to a feed for an individual tag in your
favourite blog as opposed to the full feed). For that, a *topic wildcard*
(pattern) is used:

``` clojure
(mh/subscribe conn {"americas/south/#" 0} (partial handle-delivery "americas/south/#")
(mh/subscribe conn {"americas/north/us/ca/+" 0} (partial handle-delivery "americas/north/us/ca/+"))
```

A topic pattern consists of several words separated by slashes, in a
similar way to URI path segments. Here are a few examples:

 * asia/southeast/thailand/bangkok
 * sports/basketball
 * usa/nasdaq/aapl
 * tasks/search/indexing/accounts

Now let us take a look at a few topics that match the "americas/south/#" pattern:

 * americas/south
 * americas/south/**brazil**
 * americas/south/**brazil/saopaolo**
 * americas/south/**chile.santiago**

In other words, the `#` part of the pattern matches 1 or more words.

For a pattern like `americas/south/+`, some matching routing keys would be:

 * americas/south/**brazil**
 * americas/south/**chile**
 * americas/south/**peru**

but not

 * americas/south
 * americas/south/chile/santiago

so `+` only matches a single word. Topic segments (words) may contain
the letters A-Z and a-z, digits 0-9 and spaces, separated by slashes.

When you run this example, the output will look a bit like this:

```
[consumer of americas/north/us/ca/+] received San Diego update for topic americas/north/us/ca/sandiego
[consumer of americas/north/#] received San Diego update for topic americas/north/us/ca/sandiego
[consumer of americas/north/us/ca/+] received Berkeley update for topic americas/north/us/ca/berkeley
[consumer of americas/north/#] received Berkeley update for topic americas/north/us/ca/berkeley
[consumer of americas/north/us/ca/+] received SF update for topic americas/north/us/ca/sanfrancisco
[consumer of americas/north/#] received SF update for topic americas/north/us/ca/sanfrancisco
[consumer of europe/italy/rome] received Rome update for topic europe/italy/rome
[consumer of americas/north/#] received NYC update for topic americas/north/us/ny/newyork
[consumer of americas/south/#] received São Paolo update for topic americas/south/brazil/saopaolo
[consumer of asia/southeast/#] received Hong Kong update for topic asia/southeast/hk/hongkong
[consumer of asia/southeast//hk/+] received Hong Kong update for topic asia/southeast/hk/hongkong
[consumer of asia/southeast/#] received Kyoto update for topic asia/southeast/japan/kyoto
[consumer of asia/southeast/#] received Shanghai update for topic asia/southeast/prc/shanghai
```

As you can see, some messages - the Paris update - were not routed to any consumer
("deadlettered"). (You could set a handler for such messages via the option
`:on-unhandled-message` of `connect`.)


## Wrapping Up

This is the end of the tutorial. Congratulations! You have learned
quite a bit about both MQTT and Machine Head.  MQTT has more features
built into the protocol. To learn more about them, see below. To stay up to date with Machine
Head development, [follow @clojurewerkz on
Twitter](http://twitter.com/clojurewerkz) and [join our mailing
list](http://groups.google.com/group/clojure-mqtt).


## What to Read Next

The documentation is organized as [a number of guides](/articles/guides.html), covering various topics.

You might also want to check the docstrings of `connect`, `subscribe`, and `publish`. And some of the Java Paho Client JavaDocs might be of relevance.
