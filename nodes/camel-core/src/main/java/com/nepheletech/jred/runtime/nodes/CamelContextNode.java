package com.nepheletech.jred.runtime.nodes;

import org.apache.camel.CamelContext;
import org.apache.camel.impl.DefaultCamelContext;

import com.nepheletech.jred.runtime.events.NodesStartedEvent;
import com.nepheletech.jred.runtime.events.NodesStartedEventListener;
import com.nepheletech.jred.runtime.events.NodesStoppedEvent;
import com.nepheletech.jred.runtime.events.NodesStoppedEventListener;
import com.nepheletech.jred.runtime.flows.Flow;
import com.nepheletech.json.JsonObject;
import com.nepheletech.messagebus.MessageBus;

public class CamelContextNode extends AbstractConfigurationNode
    implements NodesStartedEventListener, NodesStoppedEventListener {

  private final DefaultCamelContext camelContext;

  public CamelContextNode(Flow flow, JsonObject config) {
    super(flow, config);

    camelContext = new DefaultCamelContext();
    camelContext.disableJMX();
  }

  public CamelContext getCamelContext() { return camelContext; }

  @Override
  public void onNodesStarted(NodesStartedEvent event) throws Exception {
    logger.trace(">>> onNodesStarted:");

    if (camelContext != null) {
      MessageBus.sendMessage(getId(), camelContext);

      try {
        camelContext.start();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }

  @Override
  public void onNodesStopped(NodesStoppedEvent event) throws Exception {
    logger.trace(">>> onNodesStopped:");

    if (camelContext != null) {
      try {
        camelContext.stop();
        camelContext.removeRouteDefinitions(camelContext.getRouteDefinitions());
      } catch (Exception e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
  }
}