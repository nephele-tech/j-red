package com.nepheletech.jred.runtime.nodes;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;

import com.nepheletech.jred.runtime.flows.Flow;
import com.nepheletech.json.JsonObject;
import com.nepheletech.json.JsonParser;

public class KafkaInNode extends AbstractCamelNode implements Processor {

  private final String broker;
  private final String topic;
  private final String groupId;

  public KafkaInNode(Flow flow, JsonObject config) {
    super(flow, config);

    this.broker = config.getAsString("broker");
    this.topic = config.getAsString("topic");
    this.groupId = config.getAsString("groupId", getId());
  }

  protected void addRoutes(CamelContext camelContext) throws Exception {
    logger.trace(">>> addRoutes: {}", getId());

    camelContext.addRoutes(new RouteBuilder() {
      // onException(Exception.class)

      @Override
      public void configure() throws Exception {
        from("kafka:" + topic + "?brokers=" + broker + "&groupId=" + groupId)
            .to("log:DEBUG?showBody=true&showHeaders=true")
            .process(KafkaInNode.this);
      }
    });
  }

  @Override
  public void process(Exchange exchange) throws Exception {
    logger.trace(">>> process: exchange={}", exchange);

    final Message message = exchange.getIn();
    receive(new JsonObject()
        .set("_uid", exchange.getExchangeId())
        .set("payload", JsonParser.parse(message.getBody(String.class))));
  }

  @Override
  protected void onMessage(JsonObject msg) {
    logger.trace(">>> onMessage: msg={}", msg);

    send(msg);
  }
}
