package com.nepheletech.flows.runtime.flows;

import com.nepheletech.flows.runtime.nodes.Node;
import com.nepheletech.json.JsonObject;

public interface Flow {
  
  Node getNode(String id);

  Node getNode(String id, boolean cancelBubble);
  
  boolean handleError(final Node node, Throwable t, final JsonObject msg, Node reportingNode);
}
