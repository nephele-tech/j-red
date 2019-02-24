package com.nepheletech.flows.runtime.nodes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nepheletech.flows.runtime.events.NodesStartedEvent;
import com.nepheletech.flows.runtime.events.NodesStartedEventListener;
import com.nepheletech.flows.runtime.flows.Flow;
import com.nepheletech.json.JsonObject;
import com.nepheletech.json.JsonUtil;
import com.nepheletech.messagebus.MessageBus;
import com.nepheletech.messagebus.MessageBusListener;

public class HttpInNode extends AbstractNode implements NodesStartedEventListener {
  private static final Logger logger = LoggerFactory.getLogger(HttpInNode.class);

  public static String createListenerTopic(String method, String url) {
    return HttpInNode.class.getName() + ':' + method + ':' + url;
  }

  // ---

  private final String url;
  private final String method;
  private final boolean upload;

  private final MessageBusListener<JsonObject> requestListener = (topic, msg) -> receive(msg);
  private final String requestListenerTopic;

  public HttpInNode(Flow flow, JsonObject config) {
    super(flow, config);

    final String url = config.get("url").asString(null);
    if (url == null) { throw new RuntimeException("missing path"); }
    this.url = (url.charAt(0) != '/') ? '/' + url : url;
    this.method = config.get("method").asString("GET").toUpperCase();
    this.upload = config.get("upload").asBoolean();
    // TODO swagger

    this.requestListenerTopic = createListenerTopic(this.method, this.url);
  }

  @Override
  public void onNodesStarted(NodesStartedEvent event) {
    logger.trace(">>> ----------------------------- onNodesStarted: event={}", event);
    
    logger.debug("subscribe to: {}", requestListenerTopic);
    MessageBus.subscribe(requestListenerTopic, requestListener);
  }

  @Override
  public void close() {
    logger.trace(">>> close");

    try {
      MessageBus.subscribe(requestListenerTopic, requestListener);
    } finally {
      super.close();
    }
  }

  @Override
  protected void onMessage(JsonObject msg) {
    logger.trace(">>> onMessage: msg={}", msg);

    // TODO uploads...
    
    logger.info("-------------------------{}", JsonUtil.getProperty(msg, "req._pathInfo"));
    
    send(msg);
  }
}
