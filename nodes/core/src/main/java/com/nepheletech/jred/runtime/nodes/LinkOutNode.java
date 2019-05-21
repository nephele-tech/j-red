package com.nepheletech.jred.runtime.nodes;

import com.nepheletech.jred.runtime.flows.Flow;
import com.nepheletech.jton.JtonObject;
import com.nepheletech.messagebus.MessageBus;

public class LinkOutNode extends AbstractNode {
  private final String event;

  public LinkOutNode(Flow flow, JtonObject config) {
    super(flow, config);
    this.event = "node:" + getId();
  }

  @Override
  protected void onMessage(JtonObject msg) {
    logger.trace(">>> onMessage: msg={}", msg);

    MessageBus.sendMessage(event, msg);
    send(msg);
  }
}
