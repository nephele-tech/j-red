package com.nepheletech.jred.runtime.nodes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nepheletech.jred.runtime.flows.Flow;
import com.nepheletech.json.JsonObject;

public class SwitchNode extends AbstractNode {
  private static final Logger logger = LoggerFactory.getLogger(SwitchNode.class);

  public SwitchNode(Flow flow, JsonObject config) {
    super(flow, config);
  }

  @Override
  protected void onMessage(JsonObject msg) {
    // TODO Auto-generated method stub
    
  }

}
