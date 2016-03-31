ZeroMQ Logstash Torrent Plugin for ElasticSearch
==================================

This plugin allows fast indexing from [logstash](https://www.elastic.co/products/logstash) or [node-logstash](https://github.com/bpaquet/node-logstash), using the ZeroMQ transport. This plugin replace the [elasticsearch zeromq river](https://www.elastic.co/products/logstash), which can not be used with ElasticSearch > 1.x.

Compatibility

|  Elasticsearch    | Plugin  | Doc                                                                        | 
|-------------------|---------|----------------------------------------------------------------------------|
| 2.1.1             | 0.3     | [github](https://github.com/bpaquet/elasticsearch-zeromq-torrent/tree/0.3) |
| 2.2.0             | 0.4     | [github](https://github.com/bpaquet/elasticsearch-zeromq-torrent/tree/0.4) |
| 2.2.1             | 0.4     | [github](https://github.com/bpaquet/elasticsearch-zeromq-torrent/tree/0.5) |

This plugin use by default [JeroMQ](https://github.com/zeromq/jeromq), a pure Java ZeroMQ implementation. See below to use [JZmq](https://github.com/zeromq/jzmq).

Why
---

The standard logstash method to index use [HTTP Bulk insert](https://www.elastic.co/guide/en/logstash/current/plugins-outputs-elasticsearch.html). It can been slow, and expensive.

Using ZeroMQ instead of HTTP is cheaper and faster. And you can use built-in load-balancing feature of ZeroMQ.

Installation
---

```sh
bin/plugin install https://github.com/bpaquet/elasticsearch-zeromq-torrent/releases/download/0.3/elasticsearch-zeromq-torrent-0.3.zip
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

On client side, from logstash, use

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

The installation is more complicated, but performances should be better.

* Install [jzmq](https://github.com/zeromq/jzmq)
* Install the plugin (see above)
* Go into plugin directory : ``elasticsearch-zeromq-torrent``
* Remove jeromq : ``rm jeromq*.jar``
* Download the jzmq.jar, or copy it from your JZMQ install: ``wget https://repo1.maven.org/maven2/org/zeromq/jzmq/3.1.0/jzmq-3.1.0.jar``
* Launch Elasticsearch, with following options to specify your JZMQ path and disable the security manager : ``JAVA_OPTS="-Djava.library.path=$HOME/jzmq/src/main/c++/.libs -Des.security.manager.enabled=false" bin/elasticsearch``
