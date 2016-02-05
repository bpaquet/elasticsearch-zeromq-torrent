package com.baquet.elasticsearch.zeromq_elasticsearch;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.elasticsearch.SpecialPermission;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.component.AbstractLifecycleComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.util.concurrent.EsExecutors;

public class ZeroMQTorrentService extends AbstractLifecycleComponent<ZeroMQTorrentService> {
  
  private static final ESLogger logger = Loggers.getLogger(ZeroMQTorrentService.class);
  
  private static final String ZEROMQ_LOGSTASH = "zeromq-logstash";
  private static DateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd", Locale.US);

  private String address;
  private String dataType;
  private String prefix;
  private int bulkSize;
  private int flushInterval;

  private volatile static Thread thread;
  private volatile static boolean loop;
  private Client client;

  @Inject
  public ZeroMQTorrentService(Settings settings, Client client) {
    super(settings);
    
    this.client = client;
    
    address = settings.get("zeromq.address", "tcp://127.0.0.1:12345");
    dataType = settings.get("zeromq.type", "logs");
    prefix = settings.get("zeromq.prefix", "logstash");
    bulkSize = settings.getAsInt("zeromq.bulk_size", 2000);
    flushInterval = settings.getAsInt("zeromq.flush_interval", 1);
  }

  @Override
  protected void doStart() {
    if (thread != null) {
      throw new Error("ZeroMQ torrent already started");
    }
    logger.info("Starting ZeroMQ torrent [{}] using index prefix {}", address, prefix);
    logger.info("Using configuration bulkSize: {}, flushInterval: {}", bulkSize, flushInterval);
    
    loop = true;
    Runnable consumer = new Consumer(new ZeroMQWrapper(address, logger));
    thread = EsExecutors.daemonThreadFactory(settings, ZEROMQ_LOGSTASH).newThread(consumer);
    thread.start();
  }

  @Override
  protected void doClose() {
    if (thread != null) {
      logger.info("Closing ZeroMQ torrent [{}]", address);
      loop = false;
      try {
        thread.join();
        logger.info("ZeroMQ torrent [{}] closed", address);
      } catch (InterruptedException e) {
        logger.error("Interrupted while waiting end of ZeroMQ loop {}", e);
      }
    }
  }

  @Override
  protected void doStop() {
  }
  
  public static String computeIndex(String prefix) {
    return prefix + "-" + dateFormat.format(new Date());
  }

  private class Consumer implements Runnable {

    private ZeroMQWrapper zmq;

    private BulkProcessor bulkProcessor = BulkProcessor.builder(client, new BulkProcessor.Listener() {
      @Override
      public void beforeBulk(long executionId, BulkRequest request) {
        logger.debug("Starting bulking {} data", request.numberOfActions());
      }

      @Override
      public void afterBulk(long executionId, BulkRequest request, BulkResponse response) {
        logger.debug("Successfully bulked {} data", request.numberOfActions());
      }

      @Override
      public void afterBulk(long executionId, BulkRequest request, Throwable failure) {
        logger.error("Unable to index data, {}", failure);
      }
    }).setBulkActions(bulkSize).setFlushInterval(TimeValue.timeValueSeconds(flushInterval)).build();

    public Consumer(ZeroMQWrapper zeroMQWrapper) {
      this.zmq = zeroMQWrapper;
    }

    @Override
    public void run() {
      SecurityManager sm = System.getSecurityManager();
      if (sm != null) {
        // unprivileged code such as scripts do not have SpecialPermission
        sm.checkPermission(new SpecialPermission());
      }
      AccessController.doPrivileged(new PrivilegedAction<Void>() {
        public Void run() {
          if(zmq.createSocket()) {
            logger.info("Starting ZeroMQ loop");
            while (loop) {
              if(zmq.poll(500) > 0) {
                IndexRequestBuilder req = client.
                    prepareIndex().
                    setSource(zmq.receiveMessage()).
                    setIndex(computeIndex(prefix)).
                    setType(dataType);
    
                bulkProcessor.add(req.request());
              }
            }
            zmq.closeSocket();
            logger.info("End of ZeroMQ loop");
          }
          return null;
        }
      });
    }
  }
}
