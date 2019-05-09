package com.nepheletech.jred.runtime.nodes;

import com.nepheletech.jred.runtime.flows.Flow;
import com.nepheletech.jton.JtonObject;

public class UiNode extends AbstractUINode {

  public UiNode(Flow flow, JtonObject config) {
    super(flow, config, "test.mt");
  }
}
