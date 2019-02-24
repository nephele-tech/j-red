package com.nepheletech.flows.runtime.nodes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nepheletech.flows.runtime.flows.Flow;
import com.nepheletech.flows.runtime.nodes.AbstractNode;
import com.nepheletech.json.JsonObject;

public final class CatchNode extends AbstractNode {
  private static final Logger logger = LoggerFactory.getLogger(CatchNode.class);

  protected CatchNode(Flow flow, JsonObject config) {
    super(flow, config);
  }

  @Override
  protected void onMessage(JsonObject msg) {
    logger.trace(">>> onMessage: msg={}" + msg);

    send(msg);
  }
}
