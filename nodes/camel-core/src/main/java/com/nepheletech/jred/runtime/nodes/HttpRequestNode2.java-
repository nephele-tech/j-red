package com.nepheletech.jred.runtime.nodes;

import java.util.Map;
import java.util.Map.Entry;

import javax.net.ssl.SSLContext;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.Builder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.http4.HttpClientConfigurer;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.HttpClientBuilder;

import com.nepheletech.jred.runtime.flows.Flow;
import com.nepheletech.jton.JtonElement;
import com.nepheletech.jton.JtonObject;
import com.nepheletech.jton.JsonParser;
import com.nepheletech.jton.JtonPrimitive;

public class HttpRequestNode2 extends AbstractCamelNode implements Processor {

  private final String method;
  private final String url;
  private final String ret;
  private final String tls;

  public HttpRequestNode2(Flow flow, JtonObject config) {
    super(flow, config);
    this.method = config.getAsString("method", "GET");
    this.url = config.getAsString("url");
    this.ret = config.getAsString("ret", "txt");

    this.tls = config.getAsString("tls", null);
    

    
    logger.info("==================================++++++++++++++++++++===========================>>>1");

    if (tls != null) {

      
      logger.info("==================================++++++++++++++++++++===========================>>>2");
      final TlsConfigNode tlsNode = (TlsConfigNode) getFlow().getNode(tls);
      if (tlsNode != null) {

        
        logger.info("==================================++++++++++++++++++++===========================>>>3");
        try {
          final SSLContext sslFactory = tlsNode.getSSLContext();
          if (sslFactory != null) {

            
            logger.info("==================================++++++++++++++++++++===========================>>>4");
            
            HttpClientConfigurer httpClientConfigurer = new HttpClientConfigurer() {
              @Override
              public void configureHttpClient(HttpClientBuilder clientBuilder) {
                
                logger.info("==================================++++++++++++++++++++===========================>>>5");
                
                clientBuilder.setSSLSocketFactory(sslSocketFactory(sslFactory));
              }
            };

            getCamelContext().getRegistry().bind(httpClientConfigurerKey(), httpClientConfigurer);
          } else {
            throw new NullPointerException("SSLContext is null");
          }
        } catch (Exception e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }
    }
  }
  
  private static SSLConnectionSocketFactory sslSocketFactory(SSLContext sslContext) {
    // Allow TLSv1 protocol only
    SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(
        sslContext,
        new String[] { "TLSv1", "TLSv1.1", "TLSv1.2" },
        null,
        SSLConnectionSocketFactory.getDefaultHostnameVerifier());

    return sslsf;
  }

  @Override
  protected void addRoutes(CamelContext camelContext) throws Exception {
    logger.trace(">>> addRoutes: {}", getId());
    camelContext.addRoutes(new RouteBuilder() {
      @Override
      public void configure() throws Exception {
        // onException(Exception.class)

        String url;
        if (HttpRequestNode2.this.url.startsWith("https://")) {
          url = HttpRequestNode2.this.url.replaceAll("^https://", "https4:");
          // TODO port
        } else {
          url = HttpRequestNode2.this.url.replaceAll("^http://", "http4:");
        }

        if (tls != null) {
          url += url.contains("?") ? "&" : "?";
          url += "httpClientConfigurer=" + httpClientConfigurerKey();
        }
        
        logger.trace(":::::::::::::::::::::::::::::::::::::::::::::::::::::::::::: url: {}", url);

        if ("txt".equals(HttpRequestNode2.this.ret)
            || "obj".equals(HttpRequestNode2.this.ret)) {
          from("direct:" + getId())
              .log(">>> HTTP4: Begin")
              .toD(url)
              .convertBodyTo(String.class)
              .process(HttpRequestNode2.this)
              .log(">>> HTTP4: End");
        } else if ("bin".equals(HttpRequestNode2.this.ret)) {
          from("direct:" + getId())
              .log(">>> HTTP4: Begin")
              .toD(url)
              .convertBodyTo(byte[].class)
              .process(HttpRequestNode2.this)
              .log(">>> HTTP4: End");
        } else {
          from("direct:" + getId())
              .log(">>> HTTP4: Begin")
              .toD(url)
              .process(HttpRequestNode2.this)
              .log(">>> HTTP4: End");
        }
      }
    });
  }

  private String httpClientConfigurerKey() {
    return "HttpClientConfigurer-" + getId().replace('.', '-');
  }

  @Override
  protected void onMessage(JtonObject msg) {
    logger.trace(">>> onMessage: msg={}, thread={}", msg, Thread.currentThread().getId());

    final ProducerTemplate template = getCamelContext().createProducerTemplate();
    template.send("direct:" + getId(), new Processor() {
      @Override
      public void process(Exchange exchange) throws Exception {
        final Message in = exchange.getIn();
        in.getHeaders().put("msg-" + getId(), msg);

        String method = HttpRequestNode2.this.method;
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

        final JtonObject headers = msg.getAsJsonObject("headers", false);
        if (headers != null) {
          for (Entry<String, JtonElement> entry : headers.entrySet()) {
            final String key = entry.getKey();
            final JtonElement value = entry.getValue();
            if (value.isJsonPrimitive()) {
              final JtonPrimitive _value = value.asJsonPrimitive();
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
    final JtonObject msg = (JtonObject) in.removeHeader("msg-" + getId());

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
      final JtonObject _headers = new JtonObject();
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
}
