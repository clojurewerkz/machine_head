# Machine Head, a Clojure MQTT Client

Machine Head is a Clojure MQTT client.


## Project Goals

 * Cover all key MQTT v3 features
 * Be well documented
 * Be well tested
 * Provide a convenient API (Paho Java's API is poorly designed)
 * Don't introduce a lot of latency and throughput overhead


## Community

[Machine Head has a mailing list](https://groups.google.com/forum/#!forum/clojure-mqtt). Feel free
to join it and ask any questions you may have.

To subscribe for announcements of releases, important changes and so on, please follow [@ClojureWerkz](https://twitter.com/#!/clojurewerkz) on Twitter.


## Project Maturity

Machine Head is not a new project but some Paho Java features may be missing from the Clojure API.
However, the key functionality
(connections, publishing, subscriptions) is supported. Barring the fact that
[documentation](http://clojuremqtt.info/) is work in progress,
we encourage you to give the library a try.

The API for key operations may changes in the future, although this is fairly
unlikely, given how small the library is.



## Artifacts

Machine Head artifacts are [released to Clojars](https://clojars.org/clojurewerkz/machine_head). If you are using Maven, add the following repository
definition to your `pom.xml`:

``` xml
<repository>
  <id>clojars.org</id>
  <url>http://clojars.org/repo</url>
</repository>
```

### The Most Recent Release

With Leiningen:

    [clojurewerkz/machine_head "1.0.0"]


With Maven:

    <dependency>
      <groupId>clojurewerkz</groupId>
      <artifactId>machine_head</artifactId>
      <version>1.0.0</version>
    </dependency>



## Documentation & Examples

### Guides

Machine Head [documentation guides](http://clojuremqtt.info) are work in progress:

 * [Getting Started](https://github.com/clojurewerkz/machine_head/blob/master/docs/guides/getting_started.md)

### Examples

[Code examples](https://github.com/clojurewerkz/machine_head.examples) are available
in a separate repository.


## Supported MQTT Broker Implementations

Machine Head is tested against [RabbitMQ with MQTT plugin](http://www.rabbitmq.com/mqtt.html) and
[Mosquitto](http://mosquitto.org/).


## Supported Clojure Versions

Machine Head requires Clojure 1.6+.

## Machine Head Is a ClojureWerkz Project

Machine Head is part of the [group of Clojure libraries known as ClojureWerkz](http://clojurewerkz.org), together with

 * [Langohr](http://clojurerabbitmq.info)
 * [Elastisch](http://clojureelasticsearch.info)
 * [Monger](http://clojuremongodb.info)
 * [Titanium](http://titanium.clojurewerkz.org)
 * [Neocons](http://clojureneo4j.info)
 * [Quartzite](http://clojurequartz.info)

and several others.


## Development

Machine Head uses [Leiningen 2](http://leiningen.org). Make sure you
have it installed and then run tests against supported Clojure
versions using

    lein all test

Then create a branch and make your changes on it. Once you are done
with your changes and all tests pass, submit a pull request on GitHub.


## License

Copyright (C) 2014-2016 Michael S. Klishin, Alex Petrov, and the ClojureWerkz Team.

Double licensed under the [Eclipse Public License](http://www.eclipse.org/legal/epl-v10.html) (the same as Clojure) or
the [Apache Public License 2.0](http://www.apache.org/licenses/LICENSE-2.0.html).
