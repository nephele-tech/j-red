package com.nepheletech.jred.runtime.nodes;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nepheletech.jred.runtime.flows.Flow;
import com.nepheletech.jton.JtonObject;

public final class StatusNode extends AbstractNode {
  private static final Logger log = LoggerFactory.getLogger(StatusNode.class);

  public StatusNode(Flow flow, JtonObject config) {
    super(flow, config);
  }

  @Override
  protected void onMessage(JtonObject msg) {
    log.trace(">>> onMessage: msg={}" + msg);
    
    send(msg);
  }
}
