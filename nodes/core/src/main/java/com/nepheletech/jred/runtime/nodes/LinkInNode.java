package com.nepheletech.jred.runtime.nodes;

import static com.nepheletech.messagebus.MessageBus.subscribe;

import com.nepheletech.jred.runtime.flows.Flow;
import com.nepheletech.jton.JtonElement;
import com.nepheletech.jton.JtonObject;
import com.nepheletech.messagebus.Subscription;

public class LinkInNode extends AbstractNode {
  private final String event;

  private Subscription subscription = null;

  public LinkInNode(Flow flow, JtonObject config) {
    super(flow, config);
    this.event = "node:" + getId();
    this.subscription = subscribe(this.event, (String topic, JtonObject message) -> {
      receiveMsg(message);
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
  protected JtonElement onMessage(JtonObject msg) {
    logger.trace(">>> onMessage: msg={}", msg);
    return(msg);
  }
}
