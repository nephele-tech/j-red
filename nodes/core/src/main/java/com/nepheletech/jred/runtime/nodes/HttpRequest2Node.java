package com.nepheletech.jred.runtime.nodes;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;
import static org.apache.commons.lang3.StringUtils.trimToNull;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.apache.camel.Exchange;
import org.apache.camel.component.http.HttpComponent;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.CookieStore;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheException;
import com.github.mustachejava.MustacheFactory;
import com.nepheletech.jred.runtime.flows.Flow;
import com.nepheletech.jton.JtonElement;
import com.nepheletech.jton.JtonObject;

public class HttpRequest2Node extends AbstractNode implements HasCredentials {

  private static final long DEFAULT_REQ_TIMEOUT = 120000L;

  private final String nodeUrl;
  private final boolean isTemplateUrl;
  private final String nodeMethod;

  private final boolean nodeHTTPPersistent;

  private final TlsConfigNode tlsNode;

  private final String ret;
  private final String authType;

  private final long reqTimeout;

  private final boolean paytoqs;
  private final boolean paytobody;

//Mustache
  private MustacheFactory mf;
  private Mustache mustache;

  public HttpRequest2Node(Flow flow, JtonObject config) {
    super(flow, config);
    this.nodeUrl = config.getAsString("url", null);
    this.isTemplateUrl = trimToEmpty(this.nodeUrl).indexOf("{{") != -1;
    this.nodeMethod = config.getAsString("method", "GET");
    this.nodeHTTPPersistent = config.getAsBoolean("persist", false);
    var tls = trimToNull(config.getAsString("tls", null));
    this.tlsNode = (tls != null)
        ? (TlsConfigNode) flow.getNode(tls)
        : null;
    this.ret = config.getAsString("ret", "txt");
    this.authType = config.getAsString("authType", "basic");
    this.reqTimeout = (flow.getSetting("httpRequestTimeout").isJtonNull())
        ? DEFAULT_REQ_TIMEOUT
        : flow.getSetting("httpRequestTimeout").asLong(DEFAULT_REQ_TIMEOUT);
    var paytoqs = config.getAsString("paytoqs", null);
    this.paytoqs = "true".equals(paytoqs) || "query".equals(paytoqs);
    this.paytobody = "body".equals(paytoqs);

    if (isTemplateUrl) {
      mf = new DefaultMustacheFactory();

      try {
        mustache = mf.compile(new StringReader(this.nodeUrl),
            getName() != null ? getName() : "http-request");
      } catch (MustacheException e) {
        mustache = null;
        // TODO log(Constants.NODE_RED_ERROR, e, ExceptionUtils.getRootCauseMessage(e));
      }
    }
    
    HttpComponent httpComponent = getContext().getComponent("http", HttpComponent.class);
    CookieStore cookieStore = httpComponent.getCookieStore();
    
    cookieStore.addCookie(null);
  }

  @Override
  public void setCredentials(JtonObject credentials) {
    if (credentials != null) {
    }
  }

  @Override
  public void configure() throws Exception {
    super.configure();

    fromF("direct:%s#request", getId())
        .toF("log:%s?level=TRACE&showAll=true", logger.getName())
        .to("http://request?mapHttpMessageHeaders=true")
        .toF("log:%s?level=DEBUG&showAll=true", logger.getName());
  }

  @Override
  protected JtonElement onMessage(JtonObject msg) {
    logger.trace(">>> onMessage: {}", this);

    status(new JtonObject()
        .set("fill", "bluw")
        .set("shape", "dot")
        .set("text", "requesting"));

    final String msgUrl = msg.getAsString("url", null);
    String url = (nodeUrl != null) ? nodeUrl : msgUrl;
    if (nodeUrl != null && msgUrl != null && !Objects.equals(nodeUrl, msgUrl)) {
      // warn(common.errors.nooverride);
      logger.warn("common.errors.nooverride");
    }
    if (isTemplateUrl) {
      url = getUrl(msg);
    }
    if (StringUtils.trimToNull(url) == null) {
      // node.error(RED._("httpin.errors.no-url"),msg);
      log.warn("httpin.errors.no-url");
      // TODO nodeDone();
      return null;
    }
    // url must start http:// or https:// so assume http:// if not set
    if (url.indexOf("://") != -1 && url.indexOf("http") != 0) {
      // TODO warn("non-http transport requested");
      status(new JtonObject()
          .set("fill", "red")
          .set("shape", "ring")
          .set("text", "non-http transport requested"));
      // TODO nodeDone();
      return null;
    }
    if (!((url.indexOf("http://") == 0) || (url.indexOf("https://") == 0))) {
      if (tlsNode != null) {
        url = "https://" + url;
      } else {
        url = "http://" + url;
      }
    }
    
    final String msgMethod = msg.getAsString("method", null);
    String method = nodeMethod.toUpperCase();
    if (msgMethod != null && nodeMethod != null && !"use".equals(nodeMethod)) { // warn if override not set
      // TODO warn("Warning: msg properties can not override node properties.");
    }
    if (msgMethod != null && nodeMethod != null && "use".equals(nodeMethod)) {
      method = msgMethod.toUpperCase(); // use the msg parameter
    }
    
    // TODO headers

    final Map<String, Object> headers = new HashMap<>();
    headers.put(Exchange.HTTP_URI, url);
    headers.put(Exchange.HTTP_METHOD, method);

    Object response = template
        .requestBodyAndHeaders(format("direct:%s#request", getId()), msg, headers);

    logger.debug("Response: {}", response);

    return null;
  }

  private String getUrl(JtonObject msg) {
    try {
      final StringWriter output = new StringWriter();
      mustache.execute(output, msg.deepCopy()
          .set("flow", getFlowContext())
          .set("global", getGlobalContext()));
      return output.toString();
    } catch (MustacheException e) {
      throw new RuntimeException(e);
    }
  }

}
