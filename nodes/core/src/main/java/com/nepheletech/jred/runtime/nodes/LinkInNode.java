package com.nepheletech.jred.runtime.nodes;

import static com.nepheletech.messagebus.MessageBus.subscribe;

import org.apache.camel.Exchange;

import com.nepheletech.jred.runtime.flows.Flow;
import com.nepheletech.jton.JtonObject;
import com.nepheletech.messagebus.Subscription;

public class LinkInNode extends AbstractNode {
  private final String event;

  private Subscription subscription = null;

  public LinkInNode(Flow flow, JtonObject config) {
    super(flow, config);
    this.event = "node:" + getId();
    this.subscription = subscribe(this.event, (String topic, JtonObject message) -> {
      receive(message);
    });
  }

  @Override
  protected void onClosed(boolean removed) {
    if (subscription != null) {
      subscription.unsubscribe();
      subscription = null;
    }
  }

  @Override
  protected void onMessage(Exchange exchange, JtonObject msg) {
    logger.trace(">>> onMessage: msg={}", msg);
    send(exchange, msg);
  }
}
