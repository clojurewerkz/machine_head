# Machine Head, a Clojure MQTT Client

Machine Head is a Clojure MQTT client.


## Project Goals

 * Cover all (or nearly all) MQTT v3 features
 * Be well documented
 * Be well tested
 * Provide a convenient API
 * Don't introduce a lot of latency and throughput overhead 


## Community

[Machine Head has a mailing
list](https://groups.google.com/forum/#!forum/clojure-mqtt). Feel free
to join it and ask any questions you may have.

To subscribe for announcements of releases, important changes and so on, please follow [@ClojureWerkz](https://twitter.com/#!/clojurewerkz) on Twitter.


## Project Maturity

Machine Head is *very* young and incomplete. It is currently *not useful to anyone
but the author*.



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

    [clojurewerkz/machine_head "1.0.0-SNAPSHOT"]


With Maven:

    <dependency>
      <groupId>clojurewerkz</groupId>
      <artifactId>machine_head</artifactId>
      <version>1.0.0-SNAPSHOT</version>
    </dependency>



## Documentation & Examples

Machine Head API is very unstable at the moment. It's not yet
time to work on documentation.


## Supported MQTT Broker Implementations

Machine Head is tested against [RabbitMQ with MQTT
plugin](http://www.rabbitmq.com/mqtt.html) and
[Mosquito](http://mosquitto.org/).


## Supported Clojure Versions

Machine Head requires Clojure 1.4+.


## Continuous Integration Status

[![Continuous Integration status](https://secure.travis-ci.org/clojurewerkz/machine_head.png)](http://travis-ci.org/clojurewerkz/machine_head)



## Machine Head Is a ClojureWerkz Project

Machine Head is part of the [group of Clojure libraries known as ClojureWerkz](http://clojurewerkz.org), together with

 * [Monger](http://clojuremongodb.info)
 * [Langohr](https://github.com/michaelklishin/langohr)
 * [Elastisch](https://github.com/clojurewerkz/elastisch)
 * [Titanium](http://titanium.clojurewerkz.org)
 * [Neocons](http://clojureneo4j.info)
 * [Quartzite](https://github.com/michaelklishin/quartzite)

and several others.


## Development

Machine Head uses [Leiningen 2](http://leiningen.org). Make sure you
have it installed and then run tests against supported Clojure
versions using

    lein2 all test

Then create a branch and make your changes on it. Once you are done
with your changes and all tests pass, submit a pull request on GitHub.



## License

Copyright (C) 2013 Michael S. Klishin, Alex Petrov, and the ClojureWerkz Team.

Double licensed under the [Eclipse Public License](http://www.eclipse.org/legal/epl-v10.html) (the same as Clojure) or
the [Apache Public License 2.0](http://www.apache.org/licenses/LICENSE-2.0.html).
