/*
 * Copyright NepheleTech, http://www.nephelerech.com
 *
 * This file is part of J-RED API project.
 *
 * J-RED API is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * J-RED API is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this J-RED API; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package com.nepheletech.jred.runtime.nodes;

import com.nepheletech.jred.runtime.flows.Flow;
import com.nepheletech.jton.JtonArray;
import com.nepheletech.jton.JtonElement;
import com.nepheletech.jton.JtonObject;
import com.nepheletech.messagebus.MessageBusListener;
import com.nepheletech.messagebus.Subscription;

public interface Node {

  String getId();

  String getType();

  String getZ();

  String getName();

  String getAlias();
  
  String getAliasOrIdIfNull();

  void updateWires(JtonArray wires);

  void send(JtonElement msg);

  void receive(JtonObject msg);

  void close(boolean removed);

  Flow getFlow();

  default JtonObject getContext(String type) {
    return getFlow().getContext(type);
  }

  default JtonObject getFlowContext() { return getFlow().getContext("flow"); }

  default JtonObject getGlobalContext() { return getFlow().getContext("global"); }

  /**
   * 
   * @param text simple text status
   */
  default void status(String text) {
    status(new JtonObject().set("text", text));
  }

  /**
   * 
   * @param status <code>{ fill:"red|green", shape:"dot|ring", text:"blah" }</code>
   */
  void status(JtonObject status);

  /**
   * Subscribe to node events.
   * 
   * @param <T>
   * @param event
   * @param messageListener
   * @return
   */
  <T> Subscription on(String event, MessageBusListener<T> messageListener);
}
