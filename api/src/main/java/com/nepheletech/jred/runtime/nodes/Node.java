/*
 * Copyright NepheleTech and other contributorns, http://www.nephelerech.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
