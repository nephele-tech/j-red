/*
 *     This file is part of J-RED project.
 *
 *   J-RED is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   J-RED is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with J-RED.  If not, see <https://www.gnu.org/licenses/>.
 */
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