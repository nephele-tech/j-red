package com.nepheletech.jred.runtime.nodes;

import java.util.Map.Entry;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;

import com.nepheletech.jred.runtime.flows.Flow;
import com.nepheletech.json.JsonElement;
import com.nepheletech.json.JsonObject;
import com.nepheletech.json.JsonPrimitive;

public class KafkaOutNode extends AbstractCamelNode {

  private final String broker;
  private final String topic;

  public KafkaOutNode(Flow flow, JsonObject config) {
    super(flow, config);

    this.broker = config.getAsString("broker");
    this.topic = config.getAsString("topic");
  }

  @Override
  protected void addRoutes(CamelContext camelContext) throws Exception {
    logger.trace("----------------------------------------------------------------------->>> addRoutes {}", getId());

    camelContext.addRoutes(new RouteBuilder() {
      // onException(Exception.class)

      @Override
      public void configure() throws Exception {
        from("direct:" + getId())
            .to("log:DEBUG?showBody=true&showHeaders=true")
            .to("kafka:" + topic + "?brokers=" + broker);
      }
    });
  }

  @Override
  protected void onMessage(JsonObject msg) {
    logger.trace(">>> onMessage: msg={}", msg);

    final ProducerTemplate template = getCamelContext().createProducerTemplate();
    template.send("direct:" + getId(), new Processor() {
      @Override
      public void process(Exchange exchange) throws Exception {
        final Message in = exchange.getIn();

        in.setBody(msg.get("payload").toString(), String.class);

        final JsonObject headers = msg.getAsJsonObject("headers", false);
        if (headers != null) {
          for (Entry<String, JsonElement> entry : headers.entrySet()) {
            final String key = entry.getKey();
            final JsonElement value = entry.getValue();
            if (value.isJsonPrimitive()) {
              final JsonPrimitive _value = value.asJsonPrimitive();
              if (_value.isJsonTransient()) {
                in.getHeaders().put(key, _value.getValue());
              } else {
                in.getHeaders().put(key, _value);
              }
            }
          }
        }
      }
    });
  }
}
