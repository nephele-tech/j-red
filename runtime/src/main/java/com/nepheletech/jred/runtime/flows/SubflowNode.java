package com.nepheletech.jred.runtime.flows;

import java.util.function.BiConsumer;

import com.nepheletech.jred.runtime.nodes.AbstractNode;
import com.nepheletech.jton.JtonArray;
import com.nepheletech.jton.JtonObject;

final class SubflowNode extends AbstractNode {

  private BiConsumer<SubflowNode, JtonArray> updateWires;

  public SubflowNode(Flow flow, JtonObject config) {
    super(flow, config);
  }

  public void setUpdateWires(BiConsumer<SubflowNode, JtonArray> updateWires) { this.updateWires = updateWires; }

  @Override
  protected void onMessage(JtonObject msg) {
    logger.trace(">>> onMessage: msg={}", msg);
    send(msg);
  }

  @Override
  protected void onClosed(boolean removed) {
    logger.trace(">>> onClosed");

    // TODO clear status
  }

  @Override
  public void updateWires(JtonArray wires) {
    if (updateWires == null) {
      super.updateWires(wires);
    } else {
      updateWires.accept(this, wires);
    }
  }

  protected void _updateWires(JtonArray wires) {
    super.updateWires(wires);
  }
}