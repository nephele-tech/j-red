package com.nepheletech.jred.runtime.nodes;

import static com.nepheletech.jred.runtime.util.JRedUtil.setMessageProperty;
import static com.nepheletech.jred.runtime.util.JRedUtil.toBuffer;
import static com.nepheletech.jred.runtime.util.JRedUtil.toJtonArray;
import static com.nepheletech.jton.JtonParser.parse;
import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.UUID.randomUUID;
import static org.apache.commons.lang3.StringUtils.trimToNull;

import java.util.Collection;
import java.util.Map;

import org.apache.camel.Exchange;

import com.nepheletech.jred.runtime.flows.Flow;
import com.nepheletech.jred.runtime.util.JRedUtil;
import com.nepheletech.jton.JtonArray;
import com.nepheletech.jton.JtonElement;
import com.nepheletech.jton.JtonObject;
import com.nepheletech.jton.JtonPrimitive;

public class SplitNode extends AbstractNode {

  private static final String HEADER_ID_KEY = SplitNode.class.getName() + "#ID";

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
      this.spltBin = null;
      this.spltLen = -1;
    } else if ("bin".equals(spltType)) {
      this.spltStr = config.getAsString("splt");
      this.spltBin = toBuffer(parse(spltStr).asJtonArray());
      this.spltLen = -1;
    } else if ("len".equals(spltType)) {
      this.spltLen = config.getAsInt("splt", 1);
      this.spltStr = null;
      this.spltBin = null;
    } else {
      this.spltStr = null;
      this.spltBin = null;
      this.spltLen = -1;
    }

    this.stream = config.getAsBoolean("stream", false);

    this.arraySplt = config.getAsInt("arraySplt", 1);
    final String arraySpltType = config.getAsString("arraySpltType", "len");
    if (!"len".equals(arraySpltType)) {
      throw new IllegalArgumentException("arraySpltType should be 'len'");
    }

    this.addname = trimToNull(config.getAsString("addname", null));
  }

  @Override
  public void configure() throws Exception {
    super.configure();

    fromF("direct:%s#split", getId())
        .toF("log:%s?level=TRACE", logger.getName())

        // @formatter:off
        
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
    
        // @formatter:on

    fromF("direct:%s#object", getId())
        .toF("log:%s?level=TRACE", logger.getName())
        .process((x) -> x.getIn().setHeader(HEADER_ID_KEY, randomUUID().toString()))
        .process((x) -> x.getIn().setBody(x.getIn().getBody(JtonObject.class).entrySet()))
        .split(bodyAs(Collection.class))
        .toF("log:%s?level=TRACE", logger.getName())
        .process(this::handleObjectParts);

    fromF("direct:%s#array", getId())
        .toF("log:%s?level=TRACE", logger.getName())
        .process((x) -> x.getIn().setHeader(HEADER_ID_KEY, randomUUID().toString()))
        .split(bodyAs(Collection.class)) // TODO arraySplt implementation
        .toF("log:%s?level=TRACE", logger.getName())
        .process(this::handleArrayParts);

    if ("len".equals(spltType)) {
      fromF("direct:%s#string", getId())
          .toF("log:%s?level=TRACE", logger.getName())
          .process((x) -> x.getIn().setHeader(HEADER_ID_KEY, randomUUID().toString()))
          .process((x) -> x.getIn().setBody(x.getIn().getBody(JtonPrimitive.class).getValue()))
          .split(bodyAs(String.class).tokenize("", spltLen, false)).streaming(stream)
          .toF("log:%s?level=TRACE", logger.getName())
          .process(this::handleStringParts);
    } else if ("bin".equals(spltType)) {
      fromF("direct:%s#string", getId())
          .toF("log:%s?level=TRACE", logger.getName())
          .process((x) -> x.getIn().setHeader(HEADER_ID_KEY, randomUUID().toString()))
          .process((x) -> x.getIn().setBody(x.getIn().getBody(JtonPrimitive.class).getValue()))
          .split(bodyAs(String.class).tokenize(new String(spltBin, UTF_8))).streaming(stream)
          .toF("log:%s?level=TRACE", logger.getName())
          .process(this::handleStringParts);
    } else { // str
      fromF("direct:%s#string", getId())
          .toF("log:%s?level=TRACE", logger.getName())
          .process((x) -> x.getIn().setHeader(HEADER_ID_KEY, randomUUID().toString()))
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
        .process(this::handleOtherwise);
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
    msg.set("parts", new JtonObject()
        .set("id", (String) exchange.getIn().removeHeader(HEADER_ID_KEY))
        .set("type", "object")
        .set("key", entry.getKey())
        .set("count", exchange.getProperty(Exchange.SPLIT_SIZE, Integer.class))
        .set("index", exchange.getProperty(Exchange.SPLIT_INDEX, Integer.class))
        .set("complete", exchange.getProperty(Exchange.SPLIT_COMPLETE, Boolean.class)));

    if (addname != null) {
      setMessageProperty(msg, addname, entry.getKey(), true);
    }

    send(exchange, msg);
  }

  protected void handleArrayParts(Exchange exchange) {
    final JtonObject msg = getMsg(exchange);

    msg.set("payload", exchange.getIn().getBody(JtonElement.class));
    msg.set("parts", new JtonObject()
        .set("id", (String) exchange.getIn().removeHeader(HEADER_ID_KEY))
        .set("type", "array")
        .set("count", exchange.getProperty(Exchange.SPLIT_SIZE, Integer.class))
        .set("len", 1) // TODO arraySplt implementation
        .set("index", exchange.getProperty(Exchange.SPLIT_INDEX, Integer.class))
        .set("complete", exchange.getProperty(Exchange.SPLIT_COMPLETE, Boolean.class)));

    send(exchange, msg);
  }

  protected void handleStringParts(Exchange exchange) {
    final JtonObject msg = getMsg(exchange);

    msg.set("payload", exchange.getIn().getBody(String.class));
    msg.set("parts", new JtonObject()
        .set("id", (String) exchange.getIn().removeHeader(HEADER_ID_KEY))
        .set("type", "string")
        .set("count", exchange.getProperty(Exchange.SPLIT_SIZE, Integer.class))
        .set("index", exchange.getProperty(Exchange.SPLIT_INDEX, Integer.class))
        .set("complete", exchange.getProperty(Exchange.SPLIT_COMPLETE, Boolean.class)));

    if ("len".equals(spltType)) {
      msg.getAsJtonObject("parts")
          .set("len", spltLen);
    } else if ("bin".equals(spltType)) {
      msg.getAsJtonObject("parts")
          .set("ch", toJtonArray(spltBin));
    } else { // str
      msg.getAsJtonObject("parts")
          .set("ch", spltStr);
    }

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
