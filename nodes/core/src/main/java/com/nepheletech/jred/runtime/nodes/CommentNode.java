package com.nepheletech.jred.runtime.nodes;

import com.nepheletech.jred.runtime.flows.Flow;
import com.nepheletech.jton.JtonObject;

public final class CommentNode extends AbstractNode {

  public CommentNode(Flow flow, JtonObject config) {
    super(flow, config);
  }

  @Override
  protected void onMessage(JtonObject msg) {
    throw new UnsupportedOperationException();
  }
}
