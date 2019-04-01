package com.nepheletech.jred.runtime.flows;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nepheletech.jred.runtime.nodes.CatchNode;
import com.nepheletech.jred.runtime.nodes.Node;
import com.nepheletech.jred.runtime.nodes.StatusNode;
import com.nepheletech.json.JsonArray;
import com.nepheletech.json.JsonElement;
import com.nepheletech.json.JsonObject;
import com.nepheletech.messagebus.MessageBus;

/**
 * This class represents a flow within the runtime. It is responsible for
 * creating, starting and stopping all nodes within the flow.a
 */
public class FlowImpl implements Flow {
  private static final Logger logger = LoggerFactory.getLogger(FlowImpl.class);

  protected final Flow parent;

  protected JsonObject global;
  protected JsonObject flow;

  private final boolean isGlobalFlow;

  private final String id;

  protected final Map<String, Node> activeNodes;
  private final Map<String, Subflow> subflowInstanceNodes;
  private final Map<String, List<CatchNode>> catchNodes;
  private final Map<String, List<StatusNode>> statusNodes;
  
  private final JsonObject context;

  /**
   * Create a {@code Flow} object.
   * 
   * @param flowsRuntime
   * @param globalFlow   The global flow definition
   */
  public FlowImpl(Flow parent, JsonObject globalFlow) {
    this(parent, globalFlow, null);
  }

  /**
   * Create a {@code Flow} object.
   * 
   * @param flowsRuntime
   * @param globalFlow   The global flow definition.
   * @param flow         This flow's definition.
   */
  public FlowImpl(Flow parent, JsonObject globalFlow, JsonObject flow) {
    this.parent = parent;
    this.global = globalFlow;
    if (flow == null) {
      this.flow = globalFlow;
      this.isGlobalFlow = true;
    } else {
      this.flow = flow;
      this.isGlobalFlow = false;
    }
    this.id = this.flow.get("id").asString("global");
    this.activeNodes = new HashMap<>();
    this.subflowInstanceNodes = new HashMap<>();
    this.catchNodes = new HashMap<>();
    this.statusNodes = new HashMap<>();
    this.context = new JsonObject();
  }

  /**
   * Start this flow.
   * <p>
   * The {@code diff} argument helps define what needs to be started in the case
   * of a modified-nodes/flows type deploy.
   * 
   * @param diff
   */
  @Override
  public void start(final JsonObject diff) {
    logger.trace(">>> start: diff={}", diff);

    Node newNode = null;

    catchNodes.clear();
    statusNodes.clear();

    final JsonObject configs = flow.getAsJsonObject("configs", true);
    final List<String> configNodes = new ArrayList<>(configs.keySet());
    final JsonObject configNodesAttempts = new JsonObject();
    while (configNodes.size() > 0) {
      final String id = configNodes.remove(0);
      final JsonObject node = configs.getAsJsonObject(id);
      if (!activeNodes.containsKey(id)) {
        boolean readyToCreate = true;
        // This node doesn't exist.
        // Check it doesn't reference another non-existing config node.
        for (String prop : node.keySet()) {
          if (!"id".equals(prop) && !"wires".equals(prop) && !"_users".equals(prop)) {
            final String configRef = node.getAsString(prop, null);
            if (configRef != null && configs.has(configRef)) {
              if (!activeNodes.containsKey(configRef)) {
                // References a non-existing config node
                // Add it to the back of the list to try again later
                configNodes.add(id);
                configNodesAttempts.set(id, configNodesAttempts.getAsInt(id, 0) + 1);
                if (configNodesAttempts.getAsInt(id) == 100) {
                  throw new RuntimeException("Circular config node dependency detected: " + id);
                }
                readyToCreate = false;
                break;
              }
            }
          }
        }
        if (readyToCreate) {
          newNode = FlowUtil.createNode(this, node);
          if (newNode != null) {
            activeNodes.put(id, newNode);
          }
        }
      }
    }

    if (diff != null && diff.has("rewired")) {
      final JsonArray rewired = diff.getAsJsonArray("rewired");
      for (JsonElement rewireNodeRef : rewired) {
        final Node rewireNode = activeNodes.get(rewireNodeRef.asString());
        if (rewireNode != null) {
          final JsonObject flowNodes = flow.getAsJsonObject("nodes");
          final JsonObject node = flowNodes.getAsJsonObject(rewireNode.getId());
          rewireNode.updateWires(node.getAsJsonArray("wires"));
        }
      }
    }

    final JsonObject flowNodes = flow.getAsJsonObject("nodes", false);
    if (flowNodes != null) {
      for (String id : flowNodes.keySet()) {
        final JsonObject node = flowNodes.getAsJsonObject(id);
        if (!node.has("subflow")) {
          if (!activeNodes.containsKey(id)) {
            newNode = FlowUtil.createNode(this, node);
            if (newNode != null) {
              activeNodes.put(id, newNode);
            }
          }
        } else {
          if (!subflowInstanceNodes.containsKey(id)) {
            try {
              final String subflowRef = node.getAsString("subflow");
              final JsonObject flowSubflows = flow.getAsJsonObject("subflows");
              final JsonObject globalSubflows = global.getAsJsonObject("subflows");
              final JsonObject subflowDefinition = flowSubflows.has(subflowRef)
                  ? flowSubflows.getAsJsonObject(subflowRef)
                  : globalSubflows.getAsJsonObject(subflowRef);
              // console.log("NEED TO CREATE A SUBFLOW",id,node.subflow);
              // subflowInstanceNodes.put(id, true);
              final Subflow subflow = new Subflow(this, this.global, subflowDefinition, node);
              subflowInstanceNodes.put(id, subflow);
              subflow.start(null);
              this.activeNodes.put(id, subflow.node);
            } catch (Exception e) {
              e.printStackTrace();
            }
          }
        }
      }
    }

    final int activeCount = activeNodes.size();
    if (activeCount > 0) {
      logger.trace("--------------------------------------|------------------------------|-----------------");
      logger.trace(" id                                   | type                         | alias           ");
      logger.trace("--------------------------------------|------------------------------|-----------------");
    }

    // Build the map of catch/status nodes.
    for (Entry<String, Node> entry : activeNodes.entrySet()) {
      final String id = entry.getKey();
      final Node node = entry.getValue();

      logger.trace(" {} | {} | {}",
          StringUtils.rightPad(id, 36),
          StringUtils.rightPad(node.getType(), 28),
          node.getAlias());

      if (node instanceof CatchNode) {
        if (!catchNodes.containsKey(node.getZ())) {
          catchNodes.put(node.getZ(), new ArrayList<CatchNode>());
        }
        catchNodes.get(node.getZ()).add((CatchNode) node);
      } else if (node instanceof StatusNode) {
        if (!statusNodes.containsKey(node.getZ())) {
          statusNodes.put(node.getZ(), new ArrayList<StatusNode>());
        }
        statusNodes.get(node.getZ()).add((StatusNode) node);
      }
    }

    // TODO catchNodes sort by scope

    if (activeCount > 0) {
      logger.trace("--------------------------------------|------------------------------|-----------------");
    }
  }

  /**
   * Stop this flow.
   * <p>
   * The {@code stopList} argument helps define what needs to be stopped in the
   * case of a modified-nodes/flows type deploy.
   * 
   * @param stopList
   * @param removedList
   */
  @Override
  public void stop(JsonArray stopList, JsonArray removedList) {
    logger.trace(">>> stop: stopList: {}, removedList: {}", stopList, removedList);

    if (stopList == null) {
      stopList = JsonObject.keys(this.activeNodes);
      logger.debug("stopList=", stopList);
    }

    if (removedList == null) {
      removedList = new JsonArray();
      logger.debug("removedList=", removedList);
    }

    // Convert the list to a map to avoid multiple scans of the list
    final HashSet<String> removedMap = new HashSet<>();
    for (JsonElement e : removedList) {
      removedMap.add(e.asString());
    }

    for (int i = 0; i < stopList.size(); i++) {
      final String id = stopList.get(i).asString();
      final Node node = activeNodes.remove(id);
      if (node != null) {
        if (subflowInstanceNodes.containsKey(id)) {
          try {
            final Subflow subflow = subflowInstanceNodes.get(id);
            stopNode(node, false);
            subflow.stop(null, null);
          } catch (Exception e) {
            e.printStackTrace(); // TODO node.error(err)
          }
          subflowInstanceNodes.remove(id);
        } else {
          try {
            stopNode(node, removedMap.contains(id));
          } catch (Exception e) {
            e.printStackTrace(); // TODO node.error(err)
          }
        }
      }
    }
  }

  /**
   * Stop an individual node within this flow.
   * 
   * @param node
   * @param removed
   */
  private void stopNode(Node node, boolean removed) {
    logger.trace(">>> stopNode: {}:{} {}", node.getType(), node.getId(), (removed ? " removed" : ""));

    try {
      node.close(); // TODO timeout
      logger.trace("Stopped node: {}:{}", node.getType(), node.getId());
    } catch (RuntimeException e) {
      logger.error("Error stopping node: {}:{}", node.getType(), node.getId());
      logger.debug(e.getMessage(), e);
    }
  }

  /**
   * Update the flow definition. This doesn't change anything that is running.
   * This should be called after {@link #stop(JsonArray, JsonArray)} and before
   * {@link #start(JsonObject)}.
   * 
   * @param global
   * @param flow
   */
  @Override
  public void update(JsonObject global, JsonObject flow) {
    this.global = global;
    this.flow = flow;
  }

  /**
   * Get a node instance from this flow.
   * <p>
   * If the node is not known to this flow, pass the request up to the parent.
   * 
   * @param id the node id
   * @return the node
   */
  @Override
  public Node getNode(String id) {
    return getNode(id, false);
  }

  /**
   * Get a node instance from this flow. If the node is not known to this flow,
   * pass the request up to the parent.
   * 
   * @param id           the node id
   * @param cancelBubble if {@code true}, prevents the flow from passing the
   *                     request to the parent. This stops infinite loops when the
   *                     parent asked this {@code Flow} for the node to begin
   *                     with.
   * @return the node
   */
  @Override
  public Node getNode(String id, boolean cancelBubble) {
    logger.trace(">>> getNode: id={}, cancelBubble={}", id, cancelBubble);

    if (id == null) { return null; }

    final JsonObject configs = flow.getAsJsonObject("configs", false);
    final JsonObject nodes = flow.getAsJsonObject("nodes", false);
    if ((configs != null && configs.has(id)) || (nodes != null && nodes.has(id))) {
      // This is a node owned by this flow, so return whatever we have got during a
      // stop/start, activeNodes could be null for this id
      return activeNodes.get(id);
    } else if (activeNodes.containsKey(id)) {
      // TEMP: this is a subflow internal node within this flow
      return activeNodes.get(id);
    } else if (cancelBubble) {
      // The node could be inside one of this flow's subflows
      throw new UnsupportedOperationException("TODO");
    } else {
      // Node not found inside this flow - ask the parent
      return parent.getNode(id);
    }
  }

  /**
   * Get all of the nodes instantiated within this flow.
   * 
   * @return all active nodes instantiated within this flow.
   */
  @Override
  public Map<String, Node> getActiveNodes() { return activeNodes; }

  /**
   * Get a flow setting value. This currently automatically defres to the parent
   * flow which, as defined in ./index.js returns {@link System#getenv()}.
   * <p>
   * This lays the groundwork for {@link Subflow} to have instance-specific
   * settings.
   * 
   * @param key
   * @return
   */
  @Override
  public JsonElement getSetting(String key) {
    return parent.getSetting(key);
  }

  /**
   * Handle a status event from a node within this flow.
   * 
   * @param node            The original node that triggered the event
   * @param statusMessage   The status object.
   * @param reportingNode   The emitting the status event.
   * @param muteStatusEvent Whether to emit the status event.
   */
  @Override
  public boolean handleStatus(final Node node, final JsonObject statusMessage, Node reportingNode,
      final boolean muteStatusEvent) {
    if (reportingNode == null) {
      reportingNode = node;
    }
    if (!muteStatusEvent) {
      MessageBus.sendMessage("node-status", new JsonObject()
          .set("id", node.getId())
          .set("stats", statusMessage));
    }

    boolean handled = false;

    // final JsonArray users = node.get("users").asJsonArray(true);
    if ("global".equals(this.id)) {
      // This is a global config node
      // Delegate status to any nodes using this config node

      // TODO

      handled = true;
    } else {
      boolean handledByUncaught = false;

      catchNodes.values().forEach(targetCatchNode -> {

        // TODO

      });
    }

    return handled;
  }

  /**
   * Handle an error event from a node within this flow. If there are no
   * {@link CatchNode}s within this flow, pass the event to the parent flow.
   */
  @Override
  public boolean handleError(final Node node, final String logMessage, final JsonObject msg, Node reportingNode) {
    if (reportingNode == null) {
      reportingNode = node;
    }

    int count = 1;
    if (msg != null && msg.isJsonObject("error")) {
      final JsonObject error = msg.getAsJsonObject("error");
      if (error.isJsonObject("source")) {
        final JsonObject source = error.getAsJsonObject("source");
        final String sourceId = source.get("id").asString(null);
        if (node.getId().equals(sourceId)) {
          count = source.get("count").asInt(0) + 1;
          if (count == 10) {
            // TODO node.warn(Log._("nodes.flow.error-loop"));
            return false;
          }
        }
      }
    }

    boolean handled = false;

    if ("global".equals(this.id)) {
      // This is a global config node
      // Delegate status to any nodes using this config node

      // TODO

      handled = true;
    } else {
      boolean handledByUncaught = false;

      catchNodes.values().forEach(targetCatchNode -> {

        // TODO

      });
    }

    return handled;
  }

  @Override
  public String toString() {
    return "Flow [flow=" + id + "]";
  }

  @Override
  public JsonObject getContext(String type) {
    if ("flow".equalsIgnoreCase(type)) {
      return context;
    } else {
      return parent.getContext(type);
    }
  }
}
