package com.nepheletech.jred.runtime.nodes;

import org.apache.camel.CamelContext;

import com.nepheletech.jred.runtime.flows.Flow;
import com.nepheletech.json.JsonObject;
import com.nepheletech.messagebus.MessageBus;
import com.nepheletech.messagebus.MessageBusListener;
import com.nepheletech.messagebus.Subscription;

public abstract class AbstractCamelNode extends AbstractNode {

  private final String camelContextRef;

  private final Subscription camelContextStartupSubscription;

  public AbstractCamelNode(Flow flow, JsonObject config) {
    super(flow, config);

    this.camelContextRef = config.getAsString("context");

    if (this.camelContextRef != null) {
      this.camelContextStartupSubscription = MessageBus
          .subscribe(this.camelContextRef, new MessageBusListener<CamelContext>() {
        @Override
        public void messageSent(String topic, CamelContext camelContext) {
          try {
            addRoutes(camelContext);
          } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
          }
        }
      });
    } else {
      this.camelContextStartupSubscription = null;
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
      camelContextStartupSubscription.unsubscribe();
    }
  }
}
