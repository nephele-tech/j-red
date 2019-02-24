package com.nepheletech.jred.runtime.nodes;

import com.nepheletech.jred.runtime.flows.Flow;
import com.nepheletech.json.JsonObject;

public class UiNode extends AbstractUINode {

  public UiNode(Flow flow, JsonObject config) {
    super(flow, config, "test.mt");
  }
}
