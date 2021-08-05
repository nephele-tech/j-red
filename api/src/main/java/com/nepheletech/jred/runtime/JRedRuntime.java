/*
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
package com.nepheletech.jred.runtime;

import java.io.IOException;

import org.apache.camel.CamelContext;

import com.nepheletech.jred.runtime.nodes.Node;
import com.nepheletech.jred.runtime.storage.Storage;
import com.nepheletech.jton.JtonArray;
import com.nepheletech.jton.JtonObject;

/**
 * This interface provides the core runtime component of J-RED. It does *not*
 * include the J-RED Editor. All interaction with the editor is done using the
 * API provided.
 */
public interface JRedRuntime {

  void startFlows();

  void stopFlows();

  String loadFlows(boolean forceStart);

  /**
   * 
   * @param config
   * @param type
   * @return flow revision
   * @throws IOException
   */
  String setFlows(JtonArray config, String type) throws IOException;

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
   * @param id   the node id
   * @return the safe credentials
   */
  JtonObject getNodeCredentials(String type, String id);

  // ---

  Storage getStorage();

  JtonObject getGlobalContext();

  CamelContext getCamelContext();
}
