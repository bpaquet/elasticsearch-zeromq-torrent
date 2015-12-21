ZeroMQ Logstash Torrent Plugin for ElasticSearch
==================================

This plugin allows fast indexing from [logstash](https://www.elastic.co/products/logstash) or [node-logstash](https://github.com/bpaquet/node-logstash), using the ZeroMQ transport. This plugin replace the [elasticsearch zeromq river](https://www.elastic.co/products/logstash), which can not be used with ElasticSearch > 1.x.

ElasticSearch compatibilty : developped and tested against 2.1.1.

This plugin use by default [JeroMQ](https://github.com/zeromq/jeromq), a pure Java ZeroMQ implementation. See below to use [JZmq](https://github.com/zeromq/jzmq).

Why
----

The standard logstash method to index use [HTTP Bulk insert](https://www.elastic.co/guide/en/logstash/current/plugins-outputs-elasticsearch.html). It can been slow, and expensive.

Using ZeroMQ instead of HTTP is cheaper and faster. And you can use built-in load-balancing feature of ZeroMQ.

Installation
---

```sh
bin/plugin -install bpaquet/elasticsearch-river-zeromq/0.0.2 --url https://github.com/bpaquet/elasticsearch-river-zeromq/releases/download/v0.0.2/elasticsearch-river-zeromq-0.0.2.zip
```

How to use it
---

Add into ``elasticsearch.yml``:

```
zeromq.enabled: true
```

Optional parameters :

```
zeromq.adress: tcp://0.0.0.0:9700
zeromq.type: logs
zeromq.prefix: logstash
zeromq.bulk_size: 2000
zeromq.flush_interval: 1
```

From logstash, use

```
output {
  zeromq {
	  topology => "pushpull"
  	mode => "client"
  	address => ["tcp://127.0.0.1:5556"]
  }
}
```

From node-logstash, use

```
output://zeromq://tcp://127.0.0.1:5556
```

That's all !

Use JZMQ
---

* Install the plugin
* Go into plugin directory : ``plugins/river-zeromq``
* Remove jeromq and jzmq jars
* Copy the zmq.jar (standard installation path is ``/usr/share/java/zmq.jar``)
* Set ``java.library.path`` while starting ElasticSearch : ``JAVA_OPTS="-Djava.library.path=/lib" bin/elasticsearch -f``