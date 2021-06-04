package com.nepheletech.camel.component.cron4j;

import org.apache.camel.Category;
import org.apache.camel.Consumer;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.spi.Metadata;
import org.apache.camel.spi.UriEndpoint;
import org.apache.camel.spi.UriParam;
import org.apache.camel.spi.UriPath;
import org.apache.camel.support.DefaultEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Schedule sending of messages using the Cron4j scheduler.
 */
@UriEndpoint(scheme = "cron4j", firstVersion = "3.9.0", title = "Cron4j", syntax = "cron4j:name", consumerOnly = true, category = {
    Category.SCHEDULING })
public class Cron4jEndpoint extends DefaultEndpoint {

  private static final Logger logger = LoggerFactory.getLogger(Cron4jComponent.class);
  
  @UriPath
  @Metadata(required = true)
  private String name;

  @UriParam
  @Metadata(required = true)
  private String schedule;

  public Cron4jEndpoint(String endpointUri, Cron4jComponent component) {
    super(endpointUri, component);
  }

  public String getName() {
    return name;
  }

  /**
   * The name of the cron trigger
   */
  public void setName(String name) {
    this.name = name;
  }

  public String getSchedule() {
    return schedule;
  }

  /**
   * A cron expression that will be used to generate events
   */
  public void setSchedule(String schedule) {
    this.schedule = schedule;
  }

  @Override
  public Producer createProducer() throws Exception {
    throw new UnsupportedOperationException();
  }

  @Override
  public Consumer createConsumer(Processor processor) throws Exception {
    logger.trace(">>> createConsumer: processor={}", processor);
    Consumer consumer = new Cron4jConsumer(this, processor);
    configureConsumer(consumer);
    return consumer;
  }

}
