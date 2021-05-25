package com.nepheletech.camel.component.cron4j;

import java.util.Map;

import org.apache.camel.Endpoint;
import org.apache.camel.spi.annotations.Component;
import org.apache.camel.support.DefaultComponent;
import org.apache.camel.util.ObjectHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component("cron4j")
public class Cron4jComponent extends DefaultComponent {

  private static final Logger logger = LoggerFactory.getLogger(Cron4jComponent.class);

  @Override
  protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {
    logger.info(">>> createEndpoint: uri={}, remaining={}, parameters={}", uri, remaining, parameters);

    Cron4jEndpoint endpoint = new Cron4jEndpoint(uri, this);
    endpoint.setName(remaining);

    // special for schedule where we replace + as space
    String schedule = getAndRemoveParameter(parameters, "schedule", String.class);
    if (schedule != null) {
      // replace + as space
      schedule = schedule.replace('+', ' ');
    }
    endpoint.setSchedule(schedule);

    setProperties(endpoint, parameters);

    // validate configuration
    validate(endpoint);

    return endpoint;
  }

  private void validate(Cron4jEndpoint endpoint) {
    ObjectHelper.notNull(endpoint.getName(), "name");
    ObjectHelper.notNull(endpoint.getSchedule(), "schedule");

    String[] parts = endpoint.getSchedule().split("\\s");
    if (parts.length < 5 || parts.length > 7) {
      throw new IllegalArgumentException(
          "Invalid number of parts in cron expression. Expected 5 to 7, got: " + parts.length);
    }
  }

}
