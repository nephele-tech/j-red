package com.nepheletech.camel.component.cron4j;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.support.DefaultConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.sauronsoftware.cron4j.Scheduler;

public class Cron4jConsumer extends DefaultConsumer {

  private static final Logger logger = LoggerFactory.getLogger(Cron4jConsumer.class);

  private Scheduler scheduler = null;

  private final Runnable scheduledTask = () -> run();

  private final Cron4jEndpoint endpoint;

  public Cron4jConsumer(Cron4jEndpoint endpoint, Processor processor) {
    super(endpoint, processor);
    this.endpoint = endpoint;
  }

  private void run() {
    logger.trace(">>> run: {}", this);

    final Exchange exchange = endpoint.createExchange();

    try {
      getProcessor().process(exchange);
    } catch (Exception e) {
      // log exception if an exception occurred and was not handled
      if (exchange.getException() != null) {
        getExceptionHandler().handleException("Error processing exchange", exchange, exchange.getException());
      }
    }
  }

  @Override
  protected void doStart() throws Exception {
    logger.trace(">>> doStart: {}", this);

    super.doStart();

    scheduler = new Scheduler();
    // TODO scheduler.setDaemon(true);
    // TODO scheduler.setTimeZone(TimeZone.getTimeZone(getRouteId()));
    scheduler.schedule(endpoint.getSchedule(), scheduledTask);
    scheduler.start();
  }

  @Override
  protected void doStop() throws Exception {
    logger.trace(">>> doStop: {}", this);

    if (scheduler != null) {
      try {
        scheduler.stop();
      } catch (Exception e) {
        logger.error("Stopping cron4jScheduler error", e);
      } finally {
        scheduler = null;
      }
    }

    super.doStop();
  }
}
