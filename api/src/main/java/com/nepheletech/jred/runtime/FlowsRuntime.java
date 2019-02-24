package com.nepheletech.jred.runtime;

import java.io.IOException;

import com.nepheletech.jred.runtime.nodes.Node;
import com.nepheletech.jred.runtime.storage.Storage;
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
