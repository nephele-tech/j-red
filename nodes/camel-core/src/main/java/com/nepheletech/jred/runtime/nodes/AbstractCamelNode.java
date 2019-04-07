package com.nepheletech.jred.runtime.nodes;

import org.apache.camel.CamelContext;

import com.nepheletech.jred.runtime.flows.Flow;
import com.nepheletech.json.JsonObject;
import com.nepheletech.messagebus.MessageBus;
import com.nepheletech.messagebus.MessageBusListener;

public abstract class AbstractCamelNode extends AbstractNode {

  private final String camelContextRef;

  private final MessageBusListener<CamelContext> camelContextStartupListener;

  public AbstractCamelNode(Flow flow, JsonObject config) {
    super(flow, config);

    this.camelContextRef = config.getAsString("context");
    this.camelContextStartupListener = new MessageBusListener<CamelContext>() {
      @Override
      public void messageSent(String topic, CamelContext camelContext) {
        try {
          addRoutes(camelContext);
        } catch (Exception e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }
    };

    if (this.camelContextRef != null) {
      MessageBus.subscribe(this.camelContextRef, camelContextStartupListener);
    }
  }

  protected CamelContext getCamelContext() {
    return ((CamelContextNode) getFlow().getNode(camelContextRef)).getCamelContext();
  }

  protected abstract void addRoutes(CamelContext camelContext) throws Exception;

  @Override
  protected void onClosed(boolean removed) {
    logger.trace(">>> onClosed: {}", removed);

    if (/*removed &&*/ camelContextRef != null) {
      MessageBus.unsubscribe(camelContextRef, camelContextStartupListener);
    }
  }
}
