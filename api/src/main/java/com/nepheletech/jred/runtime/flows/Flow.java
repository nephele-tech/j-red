/*
 * Copyright NepheleTech, http://www.nephelerech.com
 *
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
package com.nepheletech.jred.runtime.flows;

import java.util.Map;

import org.apache.camel.CamelContext;

import com.nepheletech.jred.runtime.nodes.Node;
import com.nepheletech.jton.JtonArray;
import com.nepheletech.jton.JtonElement;
import com.nepheletech.jton.JtonObject;

/**
 * This class represents a flow within the runtime. It is responsible for
 * creating, starting and stopping all nodes within the flow.
 */
public interface Flow {
  
  String getPath();

  <T extends Node> T getNode(String id);

  <T extends Node> T getNode(String id, boolean cancelBubble);

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
  
  /**
   * 
   * @return
   */
  CamelContext getCamelContext();

}
