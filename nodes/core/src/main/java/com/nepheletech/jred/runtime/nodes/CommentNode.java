package com.nepheletech.jred.runtime.nodes;

import com.nepheletech.jred.runtime.flows.Flow;
import com.nepheletech.json.JsonObject;

public final class CommentNode extends AbstractNode {

  public CommentNode(Flow flow, JsonObject config) {
    super(flow, config);
  }

  @Override
  protected void onMessage(JsonObject msg) {
    throw new UnsupportedOperationException();
  }
}
