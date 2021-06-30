/*
 * This file is part of J-RED Runtime project.
 *
 * J-RED Runtime is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * J-RED Runtime is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this J-RED Runtime; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package com.nepheletech.jred.runtime.flows;

import java.util.function.BiConsumer;

import org.apache.camel.Exchange;

import com.nepheletech.jred.runtime.nodes.AbstractNode;
import com.nepheletech.jton.JtonArray;
import com.nepheletech.jton.JtonObject;

final class SubflowNode extends AbstractNode {

  private BiConsumer<SubflowNode, JtonArray> updateWires;

  public SubflowNode(Flow flow, JtonObject config) {
    super(flow, config);
  }

  public void setUpdateWires(BiConsumer<SubflowNode, JtonArray> updateWires) {
    this.updateWires = updateWires;
  }
  
  @Override
  public void configure() throws Exception {
    super.configure();
    
    logger.info(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> {}", this);
  }

  @Override
  protected void onMessage(final Exchange exchange, final JtonObject msg) {
    logger.trace(">>> onMessage: {}", getId());

    send(exchange, msg);
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