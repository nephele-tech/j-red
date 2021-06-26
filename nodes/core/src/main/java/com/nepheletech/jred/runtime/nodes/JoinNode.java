package com.nepheletech.jred.runtime.nodes;

import static java.lang.String.format;

import org.apache.camel.Exchange;
import org.apache.camel.Message;

import com.nepheletech.jred.runtime.flows.Flow;
import com.nepheletech.jton.JtonArray;
import com.nepheletech.jton.JtonElement;
import com.nepheletech.jton.JtonObject;

public class JoinNode extends AbstractNode {

  private static final String AGGREGATE_TYPE = "JRedAggregateType";
  private static final String CORRELATION_KEY = "JRedCorrelationKey";

  // ---

  private final String mode;

  public JoinNode(Flow flow, JtonObject config) {
    super(flow, config);

    this.mode = config.getAsString("mode", "auto");
  }

  @Override
  public void configure() throws Exception {
    super.configure();

    fromF("direct:%s#join", getId())
        .toF("log:%s?level=TRACE&showAll=true", logger.getName())
        .choice()
        .when((x) -> "array".equals(x.getIn().getHeader(AGGREGATE_TYPE, String.class)))
        .toF("direct:%s#array", getId())
        .when((x) -> "object".equals(x.getIn().getHeader(AGGREGATE_TYPE, String.class)))
        .toF("direct:%s#object", getId())
        .otherwise()
        .toF("direct:%s#otherwise", getId());

    fromF("direct:%s#array", getId()).routeId(getType() + ':' + getId() + ":array")
        .toF("log:%s?level=TRACE&showAll=true", logger.getName())
        .aggregate(header(CORRELATION_KEY), (oldExchange, newExchange) -> {
          if (oldExchange == null) {

            newExchange.getIn().setBody(new JtonArray()
                .push(newExchange.getIn().getBody(JtonElement.class)));

            return newExchange;

          } else {

            oldExchange.getIn().getBody(JtonArray.class)
                .push(newExchange.getIn().getBody(JtonElement.class));

            return oldExchange;
          }
        })

        .completion((x) -> {
          final JtonObject msg = getMsg(x);
          final JtonObject parts = msg.getAsJtonObject("parts");
          // final int count = parts.getAsInt("count");
          // final int index = parts.getAsInt("index");
          final boolean complete = parts.getAsBoolean("complete", false);
          // return (count - 1) == index;
          return complete;
        })

        .toF("log:%s?level=TRACE&showBody=true", logger.getName())
        .process(this::handleAggregation);

    fromF("direct:%s#object", getId()).routeId(getType() + ':' + getId() + ":object")
        .toF("log:%s?level=TRACE&showAll=true", logger.getName())
        .aggregate(header(CORRELATION_KEY), (oldExchange, newExchange) -> {
          final JtonObject msg = getMsg(newExchange);

          logger.info("--------------------------------------{}", msg);

          final JtonObject parts = msg.getAsJtonObject("parts");

          if (oldExchange == null) {

            newExchange.getIn().setBody(new JtonObject()
                .set(parts.getAsString("key"), newExchange.getIn().getBody(JtonElement.class)));

            return newExchange;

          } else {

            oldExchange.getIn().getBody(JtonObject.class)
                .set(parts.getAsString("key"), newExchange.getIn().getBody(JtonElement.class));

            return oldExchange;
          }

        })

        .completion((x) -> {
          final JtonObject msg = getMsg(x);
          final JtonObject parts = msg.getAsJtonObject("parts");
          // final int count = parts.getAsInt("count");
          // final int index = parts.getAsInt("index");
          final boolean complete = parts.getAsBoolean("complete", false);
          // return (count - 1) == index;
          return complete;
        })
        
        .parallelProcessing()

        .toF("log:%s?level=TRACE&showBody=true", logger.getName())
        .process(this::handleAggregation);

    fromF("direct:%s#otherwise", getId()).routeId(getType() + ':' + getId() + ":otherwise")
        .toF("log:%s?level=TRACE", logger.getName());
  }

  protected String getAdditionalRoute() {
    return format("direct:%s#join", getId());
  }

  protected void handleAggregation(Exchange exchange) {
    final JtonObject msg = getMsg(exchange);

    msg.set("payload", exchange.getIn().getBody(JtonElement.class));
    msg.remove("parts");

    send(exchange, msg);
  }

  @Override
  protected void onMessage(Exchange exchange, JtonObject msg) {
    exchange.getIn().setBody(msg.get("payload"));

    final JtonObject parts = msg.getAsJtonObject("parts");
    
    final Message message = exchange.getIn();
    message.setHeader(AGGREGATE_TYPE, parts.getAsString("type"));
    message.setHeader(CORRELATION_KEY, parts.getAsString("id"));
  }
}
