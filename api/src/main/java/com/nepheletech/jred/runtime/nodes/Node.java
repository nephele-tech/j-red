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
