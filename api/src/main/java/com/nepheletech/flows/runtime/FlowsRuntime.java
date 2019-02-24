package com.nepheletech.flows.runtime;

import java.io.IOException;

import com.nepheletech.flows.runtime.nodes.Node;
import com.nepheletech.flows.runtime.storage.Storage;
import com.nepheletech.json.JsonArray;
import com.nepheletech.json.JsonObject;

public interface FlowsRuntime {

  String loadFlows(boolean forceStart);

  void startFlows();

  /**
   * 
   * @param config
   * @param type
   * @return flow revision
   * @throws IOException
   */
  String setFlows(JsonArray config, String type) throws IOException;

  void stopFlows();

  /**
   * Get the current flow configuration.
   * 
   * @return the active flow configuration.
   */
  JsonObject getFlows();

  /**
   * 
   * @param id
   * @return
   */
  Node getNode(String id);

  // ---

  Storage getStorage();
}
