package com.nepheletech.jred.runtime.flows;

import java.util.Map;

import com.nepheletech.jred.runtime.nodes.Node;
import com.nepheletech.json.JsonArray;
import com.nepheletech.json.JsonObject;

public interface Flow {
  
  Node getNode(String id);

  Node getNode(String id, boolean cancelBubble);

  Map<String, Node> getActiveNodes();

  void update(JsonObject global, JsonObject flow);

  void start(JsonObject diff);

  void stop(JsonArray stopList, JsonArray removedList);
  
  boolean handleError(final Node node, Throwable t, final JsonObject msg, Node reportingNode);

  String getSetting(String key);
}
