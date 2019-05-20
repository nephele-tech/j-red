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
package com.nepheletech.jred.runtime;

import java.io.IOException;

import com.nepheletech.jred.runtime.nodes.Node;
import com.nepheletech.jred.runtime.storage.Storage;
import com.nepheletech.jton.JtonArray;
import com.nepheletech.jton.JtonObject;

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
  String setFlows(JtonArray config, String type) throws IOException;

  void stopFlows();

  /**
   * Get the current flow configuration.
   * 
   * @return the active flow configuration.
   */
  JtonObject getFlows();

  /**
   * 
   * @param id
   * @return
   */
  Node getNode(String id);
  
  /**
   * Gets the safe credentials for a node.
   * 
   * @param type the node type to return the credential information for
   * @param id the node id
   * @return the safe credentials
   */
  JtonObject getNodeCredentials(String type, String id);

  // ---

  Storage getStorage();
}
