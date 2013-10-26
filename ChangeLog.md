## Changes Between 1.0.0-beta2 and 1.0.0-beta3

No changes yet.

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
