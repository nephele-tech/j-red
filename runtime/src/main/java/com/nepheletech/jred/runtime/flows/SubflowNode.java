package com.nepheletech.jred.runtime.flows;

import java.util.function.BiConsumer;

import com.nepheletech.jred.runtime.nodes.AbstractNode;
import com.nepheletech.json.JsonArray;
import com.nepheletech.json.JsonObject;

final class SubflowNode extends AbstractNode {

  private BiConsumer<SubflowNode, JsonArray> updateWires;

  public SubflowNode(Flow flow, JsonObject config) {
    super(flow, config);
  }

  public void setUpdateWires(BiConsumer<SubflowNode, JsonArray> updateWires) { this.updateWires = updateWires; }

  @Override
  protected void onMessage(JsonObject msg) {
    logger.trace(">>> onMessage: msg={}", msg);
    send(msg);
  }

  @Override
  public void close() {
    super.close();

    // TODO clear status
  }

  @Override
  public void updateWires(JsonArray wires) {
    if (updateWires == null) {
      super.updateWires(wires);
    } else {
      updateWires.accept(this, wires);
    }
  }

  protected void _updateWires(JsonArray wires) {
    super.updateWires(wires);
  }
}