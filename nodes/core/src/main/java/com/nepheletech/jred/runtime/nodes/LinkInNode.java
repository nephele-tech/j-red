package com.nepheletech.jred.runtime.nodes;

import org.apache.camel.Exchange;

import com.nepheletech.jred.runtime.flows.Flow;
import com.nepheletech.jton.JtonObject;

public class LinkInNode extends AbstractNode {

  public LinkInNode(Flow flow, JtonObject config) {
    super(flow, config);
  }

  @Override
  protected void onMessage(Exchange exchange, JtonObject msg) {
    logger.trace(">>> onMessage: msg={}", msg);

    send(exchange, msg);
  }
}
