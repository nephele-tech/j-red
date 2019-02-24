package com.nepheletech.flows.runtime.nodes;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nepheletech.flows.runtime.flows.Flow;
import com.nepheletech.flows.runtime.nodes.AbstractNode;
import com.nepheletech.json.JsonObject;

public final class StatusNode extends AbstractNode {
  private static final Logger log = LoggerFactory.getLogger(StatusNode.class);

  protected StatusNode(Flow flow, JsonObject config) {
    super(flow, config);
  }

  @Override
  protected void onMessage(JsonObject msg) {
    log.trace(">>> onMessage: msg={}" + msg);
    
    send(msg);
  }
}
