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
