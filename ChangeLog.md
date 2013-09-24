## Changes Between 1.0.0-beta1 and 1.0.0-beta2

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
