package com.nepheletech.jred.runtime.nodes;

import org.apache.camel.Exchange;

import com.nepheletech.jred.runtime.flows.Flow;
import com.nepheletech.jton.JtonObject;

public class LinkOutNode extends AbstractNode {

  public LinkOutNode(Flow flow, JtonObject config) {
    super(flow, config);
  }

  @Override
  protected void onMessage(final Exchange exchange, final JtonObject msg) {
    logger.trace(">>> onMessage: msg={}", msg);

    send(exchange, msg);
  }
}
