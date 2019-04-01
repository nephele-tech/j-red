package com.nepheletech.jred.runtime.nodes;

import com.nepheletech.jred.runtime.flows.Flow;
import com.nepheletech.json.JsonArray;
import com.nepheletech.json.JsonElement;
import com.nepheletech.json.JsonObject;

public interface Node {

  String getId();

  String getType();

  String getZ();

  String getName();

  String getAlias();

  void updateWires(JsonArray wires);

  void send(JsonElement msg);

  void receive(JsonObject msg);

  void close();

  Flow getFlow();

  JsonObject getContext(String type);
}
