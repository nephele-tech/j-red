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
package com.nepheletech.jred.runtime.flows;

import java.util.Map;

import com.nepheletech.jred.runtime.nodes.Node;
import com.nepheletech.jton.JtonArray;
import com.nepheletech.jton.JtonElement;
import com.nepheletech.jton.JtonObject;

public interface Flow {

  Node getNode(String id);

  Node getNode(String id, boolean cancelBubble);

  Map<String, Node> getActiveNodes();

  void update(JtonObject global, JtonObject flow);

  void start(JtonObject diff);

  void stop(JtonArray stopList, JtonArray removedList);

  /**
   * Get a flow setting value. This currently automatically defers to the parent
   * flow which returns {@code env[key]}. This lays the groundwork for Subflow to
   * have instance-specific settings.
   * 
   * @param key
   * @return
   */
  JtonElement getSetting(String key);

  /**
   * Handle a status event from a node within this flow.
   * 
   * @param node            The original node that triggered the event.
   * @param statusMessage   The status object.
   * @param reportingNode   The node emitting the status event. This could be a
   *                        subflow instance node when the status is being
   *                        delegated up.
   * @param muteStatusEvent Whether to emit the status event
   * @return {@code true} if the status event was handled; {@code false}
   *         otherwise.
   */
  boolean handleStatus(Node node, JtonObject statusMessage, Node reportingNode, boolean muteStatusEvent);

  /**
   * Handle an error event from a node within this flow. If there are no Catch
   * nodes within this flow, pass the event to the parent flow.
   * 
   * @param node          The original node that triggered the event.
   * @param msg           The error object.
   * @param reportingNode The node emitting the error event. This could be a
   *                      subflow instance node when the error is being delegated
   *                      up.
   * @return {@code true} if the error event was handled; {@code false} otherwise.
   */
  boolean handleError(Node node, Throwable logMessage, JtonObject msg, Node reportingNode);

  /**
   * 
   * @param type
   * @return
   */
  JtonObject getContext(String type);

  /**
   * 
   * @param node
   */
  void setup(Node node);

}
