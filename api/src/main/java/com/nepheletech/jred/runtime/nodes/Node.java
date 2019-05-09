package com.nepheletech.jred.runtime.nodes;

import com.nepheletech.jred.runtime.flows.Flow;
import com.nepheletech.json.JsonArray;
import com.nepheletech.json.JsonElement;
import com.nepheletech.json.JsonObject;
import com.nepheletech.messagebus.MessageBusListener;
import com.nepheletech.messagebus.Subscription;

public interface Node {

  String getId();

  String getType();

  String getZ();

  String getName();

  String getAlias();

  void updateWires(JsonArray wires);

  void send(JsonElement msg);

  void receive(JsonObject msg);

  void close(boolean removed);

  Flow getFlow();

  default JsonObject getContext(String type) {
    return getFlow().getContext(type);
  }

  default JsonObject getFlowContext() { return getFlow().getContext("flow"); }

  default JsonObject getGlobalContext() { return getFlow().getContext("global"); }

  /**
   * 
   * @param text simple text status
   */
  default void status(String text) {
    status(new JsonObject().set("text", text));
  }

  /**
   * 
   * @param status <code>{ fill:"red|green", shape:"dot|ring", text:"blah" }</code>
   */
  void status(JsonObject status);

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
