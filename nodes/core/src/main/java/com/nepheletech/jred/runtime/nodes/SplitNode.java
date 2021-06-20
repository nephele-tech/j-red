package com.nepheletech.jred.runtime.nodes;

import static java.lang.String.format;

import java.util.Collection;
import java.util.Map;

import org.apache.camel.Exchange;

import com.nepheletech.jred.runtime.flows.Flow;
import com.nepheletech.jton.JtonElement;
import com.nepheletech.jton.JtonObject;
import com.nepheletech.jton.JtonPrimitive;

public class SplitNode extends AbstractNode {

  // String / Buffer
  private final String spltStr;
  private final int spltLen;
  private final byte[] spltBin;
  
  private final String spltType;
  private final boolean stream;

  // Array
  private final int arraySplt;

  // Object

  private final String addname;

  public SplitNode(Flow flow, JtonObject config) {
    super(flow, config);
    
    this.spltType = config.getAsString("spltType", "str");
    
    if ("str".equals(this.spltType)) {
      this.spltStr = config.getAsString("splt", "\n")
          .replace("\\n", "\n")
          .replace("\\r", "\r")
          .replace("\\t", "\t"); // TODO \\e, \\f, \\0
    } else {
      this.spltStr = null;
    }
    

    if ("len".equals(spltType)) {
      this.spltLen = config.getAsInt("splt", 1); 
    } else {
      this.spltLen = -1;
    }
    

    if ("bin".equals(spltType)) {
      spltBin = config.getAsBytes("splt");
    } else {
      spltBin = null;
    }
    
    this.stream = config.getAsBoolean("stream", false);

    this.arraySplt = config.getAsInt("arraySplt", 1);

    this.addname = config.getAsString("addname");
  }

  @Override
  public void configure() throws Exception {
    super.configure();

    fromF("direct:%s#split", getId())
        .toF("log:%s?level=TRACE", logger.getName())
        .choice()
        .when((x) -> x.getIn().getBody(JtonElement.class).isJtonObject())
        .toF("direct:%s#object", getId())
        .when((x) -> x.getIn().getBody(JtonElement.class).isJtonArray())
        .toF("direct:%s#array", getId())
        .when((x) -> x.getIn().getBody(JtonElement.class).isJtonPrimitive()
            && x.getIn().getBody(JtonElement.class).asJtonPrimitive().isString())
        .toF("direct:%s#string", getId())
        .when((x) -> x.getIn().getBody(JtonElement.class).isJtonPrimitive()
            && x.getIn().getBody(JtonElement.class).asJtonPrimitive().isBuffer())
        .toF("direct:%s#buffer", getId())
        .otherwise()
        .toF("direct:%s#otherwise", getId());

    fromF("direct:%s#object", getId())
        .toF("log:%s?level=TRACE", logger.getName())
        .process((x) -> x.getIn().setBody(x.getIn().getBody(JtonObject.class).entrySet()))
        .split(body())
        .toF("log:%s?level=TRACE", logger.getName())
        .process(this::handleObjectParts);

    fromF("direct:%s#array", getId())
        .toF("log:%s?level=TRACE", logger.getName())
        .split(bodyAs(Collection.class))
        .toF("log:%s?level=TRACE", logger.getName())
        .process(this::handleArrayParts);

    if ("len".equals(spltType)) {
      fromF("direct:%s#string", getId())
          .toF("log:%s?level=TRACE", logger.getName())
          .process((x) -> x.getIn().setBody(x.getIn().getBody(JtonPrimitive.class).getValue()))
          .split(bodyAs(String.class).tokenize("", spltLen, false)).streaming(stream)
          .toF("log:%s?level=TRACE", logger.getName())
          .process(this::handleStringParts);
    } else if ("bin".equals(spltType)) {
      fromF("direct:%s#string", getId())
          .toF("log:%s?level=TRACE", logger.getName())
          .process((x) -> x.getIn().setBody(x.getIn().getBody(JtonPrimitive.class).getValue()))
          .split(bodyAs(String.class).tokenize(spltStr)).streaming(stream)
          .toF("log:%s?level=TRACE", logger.getName())
          .process(this::handleStringParts);
    } else { // str
      fromF("direct:%s#string", getId())
          .toF("log:%s?level=TRACE", logger.getName())
          .process((x) -> x.getIn().setBody(x.getIn().getBody(JtonPrimitive.class).getValue()))
          .split(bodyAs(String.class).tokenize(spltStr)).streaming(stream)
          .toF("log:%s?level=TRACE", logger.getName())
          .process(this::handleStringParts);
    }

    fromF("direct:%s#buffer", getId())
        .toF("log:%s?level=TRACE", logger.getName())
        .split(body())
        .toF("log:%s?level=TRACE", logger.getName())
        .process(this::handleBufferParts);

    fromF("direct:%s#otherwise", getId())
        .toF("log:%s?level=TRACE", logger.getName())
        .process(this::handleArrayParts);
  }

  protected String getAdditionalRoute() {
    return format("direct:%s#split", getId());
  }

  @Override
  protected void onMessage(Exchange exchange, JtonObject msg) {
    exchange.getIn().setBody(msg.get("payload"));
  }

  protected void handleObjectParts(Exchange exchange) {
    final JtonObject msg = getMsg(exchange);
    @SuppressWarnings("unchecked")
    final Map.Entry<String, JtonElement> entry = exchange.getIn().getBody(Map.Entry.class);
    msg.set("payload", entry.getValue());
    send(exchange, msg);
  }

  protected void handleArrayParts(Exchange exchange) {
    final JtonObject msg = getMsg(exchange);
    msg.set("payload", exchange.getIn().getBody(JtonElement.class));
    send(exchange, msg);
  }

  protected void handleStringParts(Exchange exchange) {
    final JtonObject msg = getMsg(exchange);
    msg.set("payload", exchange.getIn().getBody(String.class));
    send(exchange, msg);
  }

  protected void handleBufferParts(Exchange exchange) {
    final JtonObject msg = getMsg(exchange);
    msg.set("payload", exchange.getIn().getBody(JtonElement.class));
    send(exchange, msg);
  }

  protected void handleOtherwise(Exchange exchange) {
    final JtonObject msg = getMsg(exchange);
    msg.set("payload", exchange.getIn().getBody(JtonElement.class));
    send(exchange, msg);
  }
}
