package com.nepheletech.jred.runtime.flows;

import java.util.Map;

import com.nepheletech.jred.runtime.nodes.Node;
import com.nepheletech.json.JsonArray;
import com.nepheletech.json.JsonElement;
import com.nepheletech.json.JsonObject;

public interface Flow {

  Node getNode(String id);

  Node getNode(String id, boolean cancelBubble);

  Map<String, Node> getActiveNodes();

  void update(JsonObject global, JsonObject flow);

  void start(JsonObject diff);

  void stop(JsonArray stopList, JsonArray removedList);

  /**
   * Get a flow setting value. This currently automatically defers to the parent
   * flow which returns {@code env[key]}. This lays the groundwork for Subflow to
   * have instance-specific settings.
   * 
   * @param key
   * @return
   */
  JsonElement getSetting(String key);

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
  boolean handleStatus(Node node, JsonObject statusMessage, Node reportingNode, boolean muteStatusEvent);

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
  boolean handleError(Node node, Throwable logMessage, JsonObject msg, Node reportingNode);

  /**
   * 
   * @param type
   * @return
   */
  JsonObject getContext(String type);

}
