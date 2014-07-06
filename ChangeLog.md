## Changes Between 1.0.0-beta8 and 1.0.0-beta9

### Subscription API Change

Previously `clojurewerkz.machine-head.client/subscribe` accepted
a list of topics and optionally a list of QoS levels:

``` clojure
(mh/subscribe c ["mh/topics/#" "mh/alt.topics/+"]
                  (fn [^String topic meta ^bytes payload])
                  {:qos [0 1]})
```

This turns out to be a fairly confusing API.

We've changed it to accept a map of topics to QoS levels. While
a bit more verbose, this API make it very clear what topic will use
what QoS:

``` clojure
(mh/subscribe c {"mh/topics/#" 0 "mh/alt.topics/+" 1}
                  (fn [^String topic meta ^bytes payload] ))
```


## Changes Between 1.0.0-beta7 and 1.0.0-beta8

### Clojure 1.6 By Default

The project now depends on `org.clojure/clojure` version `1.6.0`. It is
still compatible with Clojure 1.4 and if your `project.clj` depends on
a different version, it will be used, but 1.6 is the default now.

We encourage all users to upgrade to 1.6, it is a drop-in replacement
for the majority of projects out there.


## Changes Between 1.0.0-beta6 and 1.0.0-beta7

### Retain Default Change

When publishing, `retain` now defaults to `false`,
which is a much more sensible default.

Contributed by Martin Trojer.


## Changes Between 1.0.0-beta5 and 1.0.0-beta6

### :clean-session Accepts false

It is now possible to set `:clean-session` to `false.

Contributed by Martin Trojer.


## Changes Between 1.0.0-beta4 and 1.0.0-beta5

### Last Will and Testament

Machine Head now supports providing client last will and testament:

``` clojure
(require '[clojurewerkz.machine-head.client :as mh])

(let [will {:topic "lw-topic" :payload (.getBytes "last will") :qos 0 :retain false}]
  (mh/connect "" (mh/generate-id) {:will will}))
```

Contributed by Paul Bellamy.



## Changes Between 1.0.0-beta3 and 1.0.0-beta4

### Client ID Limit

MQTT spec dictates that client [ID's should be limited to
23 bytes](http://publib.boulder.ibm.com/infocenter/wmqv7/v7r0/index.jsp?topic=%2Fcom.ibm.mq.amqtat.doc%2Ftt60310_.htm). Eclipse Paho client generator [can produce ID's
longer than that](https://bugs.eclipse.org/bugs/show_bug.cgi?id=404378).

Machine Head now will limit ID length to the last 23 bytes.

Contributed by Yodit Stanton.


## Changes Between 1.0.0-beta2 and 1.0.0-beta3

### Clean Session Support

`clojurewerkz.machine-head.client/connect` now supports one more
option: `:clean-session`. When set to true, the option means that
the client and MQTT broker should discard state that might have
been kept from earlier connections.


## Changes Between 1.0.0-beta1 and 1.0.0-beta2

### `client/subscribe-with-qos` is Removed

`clojurewerkz.machine-head.client/subscribe-with-qos` is removed. Instead,
`clojurewerkz.machine-head.client/subscribe` now takes a new option, `:qos`.

Contributed by Martin Trojer.


### Disconnect and Close

### More Flexible Publish

`clojurewerkz.machine-head.client/disconnect-and-close` is a new function that's
identical to `clojurewerkz.machine-head.client/disconnect` but also releases
all resources used by the client (e.g. file descriptors used by durable write-ahead
log).

### More Flexible Publish

`clojurewerkz.machine-head.client/publish` now also accepts `MqttMessage` instances
that makes it easy to republish pending messages returned by `clojurewerkz.machine-head.client/pending-messages`.


### Pending Messages

`clojurewerkz.machine-head.client/pending-messages` is
a new function that is similar to `clojurewerkz.machine-head.client/pending-delivery-tokens`
but returns messages instead of delivery tokens (identifiers). It returns
a lazy sequence.

The sequence can only be non-empty if client terminated before
finishing delivering all previously published messages.

### Pending Delivery Tokens

`clojurewerkz.machine-head.client/pending-delivery-tokens` is
a new function that takes a client and returns a collection of
pending delivery tokens. The collection can only be non-empty
if client terminated before finishing delivering all previously
published messages.



## Machine Head 1.0.0-beta1 (initial release)

### Core Functionality

Machine Head `beta1` supports connecting to MQTT brokers
(tested against Mosquitto and RabbitMQ MQTT plugin),
topic subscriptions and publishing.

The API is a subject to change at any time.
