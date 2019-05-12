package com.nepheletech.jred.runtime.nodes;

import com.nepheletech.jred.runtime.flows.Flow;
import com.nepheletech.jton.JtonObject;

public class ExceptionNode extends AbstractNode {
  private final String message;

  public ExceptionNode(Flow flow, JtonObject config) {
    super(flow, config);
    this.message = config.getAsString("message", null);
  }

  @Override
  protected void onMessage(JtonObject msg) {
    throw new RuntimeException(message);
  }
}
