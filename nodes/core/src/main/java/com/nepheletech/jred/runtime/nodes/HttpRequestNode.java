package com.nepheletech.jred.runtime.nodes;

import java.util.Map;
import java.util.Map.Entry;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.Builder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;

import com.nepheletech.jred.runtime.events.NodesStartedEvent;
import com.nepheletech.jred.runtime.events.NodesStartedEventListener;
import com.nepheletech.jred.runtime.flows.Flow;
import com.nepheletech.json.JsonElement;
import com.nepheletech.json.JsonObject;
import com.nepheletech.json.JsonParser;
import com.nepheletech.json.JsonPrimitive;

public class HttpRequestNode extends AbstractNode implements NodesStartedEventListener, Processor {

  private final String method;
  private final String url;
  private final String ret;

  private final CamelContext camelContext;

  public HttpRequestNode(Flow flow, JsonObject config) {
    super(flow, config);
    this.method = config.getAsString("method", "GET");
    this.url = config.getAsString("url");
    this.ret = config.getAsString("ret", "txt");

    this.camelContext = new DefaultCamelContext();
    this.camelContext.disableJMX();
  }

  @Override
  protected void onMessage(JsonObject msg) {
    logger.trace(">>> onMessage: msg={}, thread={}", msg, Thread.currentThread().getId());

    final ProducerTemplate template = camelContext.createProducerTemplate();
    template.send("direct:" + getId(), new Processor() {
      @Override
      public void process(Exchange exchange) throws Exception {
        final Message in = exchange.getIn();
        in.getHeaders().put("msg-" + getId(), msg);

        String method = HttpRequestNode.this.method;
        if ("use".equals(method)) {
          msg.getAsString("url", "GET");
        }

        if ("GET".equals(method)) {
          in.getHeaders().put(Exchange.HTTP_METHOD,
              Builder.constant(org.apache.camel.component.http4.HttpMethods.GET));
        } else if ("POST".equals(method)) {
          in.getHeaders().put(Exchange.HTTP_METHOD,
              Builder.constant(org.apache.camel.component.http4.HttpMethods.POST));
        } else if ("PUT".equals(method)) {
          in.getHeaders().put(Exchange.HTTP_METHOD,
              Builder.constant(org.apache.camel.component.http4.HttpMethods.PUT));
        } else if ("DELETE".equals(method)) {
          in.getHeaders().put(Exchange.HTTP_METHOD,
              Builder.constant(org.apache.camel.component.http4.HttpMethods.DELETE));
        }

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

  @Override
  public void process(Exchange exchange) throws Exception {
    logger.trace(">>> process: exchange={}, thread={}", exchange, Thread.currentThread().getId());

    final Message in = exchange.getIn();
    final JsonObject msg = (JsonObject) in.removeHeader("msg-" + getId());

    final Object statusCode = in.removeHeader(Exchange.HTTP_RESPONSE_CODE);
    msg.set("statusCode", statusCode, false);

    if ("obj".equals(this.ret)) {
      final String body = (String) in.getBody();
      msg.set("payload", JsonParser.parse(body));
    } else if ("txt".equals(this.ret)) {
      final String body = (String) in.getBody();
      msg.set("payload", JsonParser.parse(body));
    } else {
      final Object body = in.getBody();
      if (body instanceof String) {
        msg.set("payload", body, false);
      } else if (body instanceof byte[]) {
        msg.set("payload", body, false);
      } else {
        msg.set("payload", body, true);
      }
    }

    final Map<String, Object> headers = in.getHeaders();
    if (headers != null && headers.size() > 0) {
      final JsonObject _headers = new JsonObject();
      for (Map.Entry<String, Object> entry : headers.entrySet()) {
        try {
          _headers.set(entry.getKey(), entry.getValue(), false);
        } catch (IllegalArgumentException e) {
          _headers.set(entry.getKey(), entry.getValue(), true);
        }
      }
      msg.set("headers", _headers);
    }

    send(msg);
  }

  @Override
  public void onNodesStarted(NodesStartedEvent event) throws Exception {
    logger.info("------------------------------------------->>> onNodesStarted");

    // get camel configuration

    this.camelContext.addRoutes(new RouteBuilder() {
      @Override
      public void configure() throws Exception {
        // onException(Exception.class)

        final String url;
        if (HttpRequestNode.this.url.startsWith("https://")) {
          url = HttpRequestNode.this.url.replaceAll("^https://", "");
          // TODO port
        } else {
          url = HttpRequestNode.this.url.replaceAll("^http://", "");
        }

        if ("txt".equals(HttpRequestNode.this.ret)
            || "obj".equals(HttpRequestNode.this.ret)) {
          from("direct:" + getId())
              .log(">>> HTTP4: Begin")
              .toD("http4:" + url)
              .convertBodyTo(String.class)
              .process(HttpRequestNode.this)
              .log(">>> HTTP4: End");
        } else if ("bin".equals(HttpRequestNode.this.ret)) {
          from("direct:" + getId())
              .log(">>> HTTP4: Begin")
              .toD("http4:" + url)
              .convertBodyTo(byte[].class)
              .process(HttpRequestNode.this)
              .log(">>> HTTP4: End");
        } else {
          from("direct:" + getId())
              .log(">>> HTTP4: Begin")
              .toD("http4:" + url)
              .process(HttpRequestNode.this)
              .log(">>> HTTP4: End");
        }
      }
    });

    this.camelContext.start();
  }

  @Override
  protected void onClosed(boolean removed) {
    logger.trace(">>> onClosed");
    try {
      this.camelContext.stop();
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }
}
