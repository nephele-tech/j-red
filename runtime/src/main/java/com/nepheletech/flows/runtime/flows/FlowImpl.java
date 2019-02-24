package com.nepheletech.flows.runtime.flows;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nepheletech.flows.runtime.FlowsRuntime;
import com.nepheletech.flows.runtime.nodes.AbstractNode;
import com.nepheletech.flows.runtime.nodes.CatchNode;
import com.nepheletech.flows.runtime.nodes.Node;
import com.nepheletech.flows.runtime.nodes.StatusNode;
import com.nepheletech.json.JsonArray;
import com.nepheletech.json.JsonElement;
import com.nepheletech.json.JsonNull;
import com.nepheletech.json.JsonObject;
import com.nepheletech.json.JsonPrimitive;

/**
 * This class represents a flow within the runtime. It is responsible for
 * creating, starting and stopping all nodes within the flow.a
 */
public class FlowImpl implements Flow {
  private static final Logger logger = LoggerFactory.getLogger(FlowImpl.class);

  @SuppressWarnings("unused")
  private final FlowsRuntime flowsRuntime;

  private JsonObject global;
  private JsonObject flow;

  private final boolean isGlobalFlow;

  private final String id;

  private final Map<String, Node> activeNodes;
  private final Map<String, List<String>> subflowInstanceNodes;
  private final Map<String, List<CatchNode>> catchNodes;
  private final Map<String, List<StatusNode>> statusNodes;

  /**
   * Create a {@code Flow} object.
   * 
   * @param flowsRuntime
   * @param globalFlow   The global flow definition
   */
  public FlowImpl(FlowsRuntime flowsRuntime, JsonObject globalFlow) {
    this(flowsRuntime, globalFlow, null);
  }

  /**
   * Create a {@code Flow} object.
   * 
   * @param flowsRuntime
   * @param globalFlow   The global flow definition.
   * @param flow         This flow's definition.
   */
  public FlowImpl(FlowsRuntime flowsRuntime, JsonObject globalFlow, JsonObject flow) {
    this.flowsRuntime = flowsRuntime;
    this.global = globalFlow;
    if (flow == null) {
      this.flow = globalFlow;
      this.isGlobalFlow = true;
    } else {
      this.flow = flow;
      this.isGlobalFlow = false;
    }
    this.id = this.flow.get("id")
        .asString("global");
    this.activeNodes = new HashMap<>();
    this.subflowInstanceNodes = new HashMap<>();
    this.catchNodes = new HashMap<>();
    this.statusNodes = new HashMap<>();
  }

  /**
   * Start this flow.
   * <p>
   * The {@code diff} argument helps define what needs to be started in the case
   * of a modified-nodes/flows type deploy.
   * 
   * @param diff
   */
  public void start(final JsonObject diff) {
    logger.trace(">>> start: diff={}", diff);

    Node newNode = null;

    catchNodes.clear();
    statusNodes.clear();

    final JsonObject configs = flow.get("configs").asJsonObject();
    final List<String> configNodes = new ArrayList<>(configs.keySet());
    final JsonObject configNodesAttempts = new JsonObject();
    while (configNodes.size() > 0) {
      final String id = configNodes.remove(0);
      final JsonObject node = configs.get(id).asJsonObject();
      if (!activeNodes.containsKey(id)) {
        boolean readyToCreate = true;
        // This node doesn't exist.
        // Check it doesn't reference another non-existing config node.
        for (String prop : node.keySet()) {
          if (!"id".equals(prop) && !"wires".equals(prop) && !"_users".equals(prop)) {
            final String configRef = node.get(prop).asString(null);
            if (configRef != null && configs.has(configRef)) {
              if (!activeNodes.containsKey(configRef)) {
                // References a non-existing config node
                // Add it to the back of the list to try again later
                configNodes.add(id);
                configNodesAttempts.set(id, configNodesAttempts.get(id).asInt(0) + 1);
                if (configNodesAttempts.get(id).asInt() == 100) {
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
      final JsonArray rewired = diff.get("rewired").asJsonArray();
      for (JsonElement rewireNodeRef : rewired) {
        final Node rewireNode = activeNodes.get(rewireNodeRef.asString());
        if (rewireNode != null) {
          final JsonObject flowNodes = flow.get("nodes").asJsonObject();
          final JsonObject node = flowNodes.get(rewireNode.getId()).asJsonObject();
          rewireNode.updateWires(node.get("wires").asJsonArray());
        }
      }
    }

    final JsonObject flowNodes = flow.get("nodes").asJsonObject(null);
    if (flowNodes != null) {
      for (String id : flowNodes.keySet()) {
        final JsonObject node = flowNodes.get(id).asJsonObject();
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
              final String subflowRef = node.get("subflow").asString();
              final JsonObject flowSubflows = flow.get("subflows").asJsonObject();
              final JsonObject globalSubflows = global.get("subflows").asJsonObject();
              final JsonObject subflow = flowSubflows.has(subflowRef)
                  ? flowSubflows.get(subflowRef).asJsonObject()
                  : globalSubflows.get(subflowRef).asJsonObject();
              final List<Node> nodes = createSubflow(subflow, node, flowSubflows, globalSubflows);
              final List<String> subflowInstanceNodes_i = new ArrayList<>();
              subflowInstanceNodes.put(id, subflowInstanceNodes_i);
              for (Node n : nodes) {
                if (n != null) {
                  subflowInstanceNodes_i.add(n.getId());
                  activeNodes.put(n.getId(), n);
                }
              }
            } catch (Exception e) {
              e.printStackTrace();
            }
          }
        }
      }
    }

    // Build the map of catch/status nodes.
    for (Entry<String, Node> entry : activeNodes.entrySet()) {
      @SuppressWarnings("unused")
      final String id = entry.getKey();
      final Node node = entry.getValue();
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
  public void stop(JsonArray stopList, JsonArray removedList) {
    if (stopList != null) {
      for (int i = 0; i < stopList.size(); i++) {
        final String id = stopList.get(i).asString();
        if (subflowInstanceNodes.containsKey(id)) {
          // The first in the list is the instance node we already
          // know about
          final List<String> subflowInstanceIds = subflowInstanceNodes.get(id);
          for (int j = 1, jMax = subflowInstanceIds.size(); j < jMax; j++) {
            stopList.push(subflowInstanceIds.get(j));
          }
        }
      }
    } else {
      stopList = JsonObject.keys(activeNodes);
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
          subflowInstanceNodes.remove(id);
        }

        try {
          node.close(); // TODO removed
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
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
    if (id == null) { return null; }

    final JsonObject configs = flow.get("configs").asJsonObject();
    final JsonObject nodes = flow.get("nodes").asJsonObject();

    if (configs.has(id) || nodes.has(id)) {
      // This is a node owned by this flow, so return whatever we have got during a
      // stop/start, activeNodes cloud be null for this id
      return activeNodes.get(id);
    } else if (activeNodes.containsKey(id)) {
      // TEMP: this is a subflow internal node within this flow
      return activeNodes.get(id);
    }

    // if (!cancelBubble) {
    // flowsRuntime.getNode(id);
    // }

    return null;
  }

  /**
   * Get all of the nodes instantiated within this flow.
   * 
   * @return
   */
  public Map<String, Node> getActiveNodes() { return activeNodes; }

  public final boolean handleStatus(final Node node, final JsonObject statusMessage) {
    throw new UnsupportedOperationException();
  }

  /**
   * Handle an error event from a node within this flow. If there are no
   * {@link CatchNode}s within this flow, pass the event to the parent flow.
   */
  @Override
  public final boolean handleError(final Node node, Throwable t, final JsonObject msg, Node reportingNode) {
    if (reportingNode == null) {
      reportingNode = node;
    }

    int count = 1;
    if (msg != null && msg.isJsonObject("error")) {
      final JsonObject error = msg.asJsonObject("error");
      if (error.isJsonObject("source")) {
        final JsonObject source = error.asJsonObject("source");
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

  private List<Node> createSubflow(JsonObject sf, JsonObject sfn, JsonObject subflows, JsonObject globalSubflows) {
    final List<Node> nodes = new ArrayList<>();
    final JsonObject node_map = new JsonObject();
    final JsonArray newNodes = new JsonArray();

    final Consumer<JsonObject> createNodeInSubflow = new Consumer<JsonObject>() {
      @Override
      public void accept(JsonObject def) {
        final JsonObject node = def.deepCopy();
        final String nid = UUID.randomUUID().toString();
        final String nodeId = node.get("id").asString();
        node_map.set(nodeId, node);
        node.set("_alias", nodeId);
        node.set("id", nid);
        node.set("z", sfn.get("id"));
        newNodes.push(node);
      }
    };

    // Clone all of the subflow node definitions and give them new IDs
    final JsonObject _configs = sf.get("configs").asJsonObject();
    for (Entry<String, JsonElement> entry : _configs.entrySet()) {
      createNodeInSubflow.accept(entry.getValue().asJsonObject());
    }

    // Clone all of the subflow node definitions and give them new IDs
    final JsonObject _nodes = sf.get("nodes").asJsonObject();
    for (Entry<String, JsonElement> entry : _nodes.entrySet()) {
      createNodeInSubflow.accept(entry.getValue().asJsonObject());
    }

    // Look for any catch/status nodes and update their scope ids
    // Update all subflow interior wiring to reflect new node IDs
    for (int i = 0; i < newNodes.size(); i++) {
      final JsonObject node = newNodes.get(i).asJsonObject();
      if (node.has("wires")) {
        final JsonArray outputs = node.get("wires").asJsonArray();
        for (int j = 0; j < outputs.size(); j++) {
          final JsonArray wires = outputs.get(j).asJsonArray();
          for (int k = 0; k < wires.size(); k++) {
            final String wire = wires.get(k).asString();
            wires.set(k, node_map.get(wire).asJsonObject().get("id"));
          }
        }
        final String type = node.get("type").asString();
        if (("catch".equals(type) || "status".equals(type))
            && node.get("scope") != JsonNull.INSTANCE) {
          final JsonArray scope = node.get("scope").asJsonArray();
          for (int j = 0; j < scope.size(); j++) {
            final String id = scope.get(j).asString();
            scope.set(j, node_map.has(id) ? node_map.get(id).asJsonObject().get("id") : new JsonPrimitive(""));
          }
        } else {
          for (String prop : node.keySet()) {
            if (!"_alias".equals(prop) && node.get(prop).isJsonPrimitive()) {
              final String nodeRef = node.get(prop).asString();
              if (node_map.has(nodeRef)) {
                node.set(prop, node_map.get(nodeRef).asJsonObject().get("id"));
              }
            }
          }
        }
      }
    }

    // Create a subflow node to accept inbound messages and route appropriately
    final JsonObject subflowInstance = new JsonObject()
        .set("id", sfn.get("id"))
        .set("type", sfn.get("type"))
        .set("z", sfn.get("z"))
        .set("name", sfn.get("name"))
        .set("wires", new JsonArray());

    if (sf.has("in")) {
      final JsonArray subflowInstanceWires = new JsonArray();
      final JsonArray in = sf.get("in").asJsonArray();

      // System.out.println("===============================================>"+sf.toString(3));
      // System.out.println("===============================================>"+node_map.toString(3));

      for (int i = 0, n = in.size(); i < n; i++) {
        final JsonArray wires = in.get(i).asJsonObject().get("wires").asJsonArray()
            .deepCopy(); // !!! deepCopy() is required
        for (int j = 0, k = wires.size(); j < k; j++) {
          final String _id = wires.get(j).asJsonObject().get("id").asString();
          final JsonObject node = node_map.get(_id).asJsonObject();
          wires.set(j, node.get("id"));
        }
        subflowInstanceWires.push(wires);
      }
      subflowInstance.set("wires", subflowInstanceWires);
      subflowInstance.set("_originalWires", subflowInstanceWires.deepCopy());

      // System.out.println("===============================================>"+subflowInstanceWires);

    }

    final SubflowNode subflowNode = new SubflowNode(this, subflowInstance) {
      @Override
      public synchronized void updateWires(JsonElement newWires) {
        if (getInstanceNodes() == null) {
          super.updateWires(newWires);
          return;
        }
        // Wire the subflow outputs
        if (sf.has("out")) {
          // Restore the original wiring to the internal nodes
          subflowInstance.set("wires", subflowInstance.get("_originalWires").deepCopy());
          final JsonArray out = sf.get("out").asJsonArray();
          for (int i = 0; i < out.size(); i++) {
            final JsonArray wires = out.get(i).asJsonObject().get("wires").asJsonArray();
            for (int j = 0; j < wires.size(); j++) {
              final JsonObject wire_j = wires.get(j).asJsonObject();
              if (!wire_j.get("id").equals(sf.get("id"))) {
                final JsonObject node = node_map.get(wire_j.get("id").asString()).asJsonObject();
                if (node.has("_originalWires")) {
                  node.set("wires", node.get("_originalWires").deepCopy());
                }
              }
            }
          }
          /*
           * final JsonObject modifiedNodes = new JsonObject(); boolean
           * subflowInstanceModified = false;
           * 
           * for (int i = 0; i < out.size(); i++) { final JsonArray wires =
           * out.get(i).asJsonObject().get("wires").asJsonArray(); for (int j = 0; j <
           * wires.size(); j++) { final JsonObject wire_j = wires.get(j).asJsonObject();
           * final String wireId = wire_j.get("id").asString(); final int wirePort =
           * wire_j.get("port").asInt(); if (wireId.equals(sf.get("id").asString())) {
           * final JsonArray subflowWires = subflowInstance.get("wires").asJsonArray();
           * subflowWires.update(wirePort, Json.concat(subflowWires.get(wirePort),
           * newWires.get(i))); subflowInstanceModified = true; } else { final JsonObject
           * node = node_map.get(wireId).asJsonObject();
           * node.get("wires").asJsonArray().update(wirePort,
           * Json.concat(node.get("wires").asJsonArray().get(wirePort), newWires.get(i)));
           * modifiedNodes.set(node.get("id").asString(), node); } } }
           * 
           * modifiedNodes.keySet().forEach((id) -> { final JsonObject node =
           * modifiedNodes.get(id).asJsonObject();
           * getInstanceNodes().get(id).updateWires(node.get("wires").asJsonArray()); });
           * 
           * if (subflowInstanceModified) {
           * super.updateWires(subflowInstance.get("wires").asJsonArray()); }
           */
        }
      }
    };

    nodes.add(subflowNode);
    /*
     * // Wire the subflow outputs if (sf.has("out")) { final JsonObject
     * modifiedNodes = new JsonObject(); final JsonArray out =
     * sf.get("out").asJsonArray(); for (int i = 0; i < out.size(); i++) { final
     * JsonArray outWires = out.get(i).asJsonObject().get("wires").asJsonArray();
     * for (int j = 0; j < outWires.size(); j++) { final JsonObject outWire_j =
     * outWires.get(j).asJsonObject(); final int port =
     * outWire_j.get("port").asInt(); if (outWire_j.get("id").equals(sf.get("id")))
     * { // A subflow input wired straight to a subflow output final JsonArray
     * subflowInstanceWires = subflowInstance.get("wires").asJsonArray();
     * subflowInstanceWires.update(port, Json.concat(subflowInstanceWires.get(port),
     * sfn.get("wires").asJsonArray().get(i)));
     * subflowNode._updateWires(subflowInstance.get("wires").asJsonArray()); } else
     * { final JsonObject node =
     * node_map.get(outWire_j.get("id").asString()).asJsonObject();
     * modifiedNodes.set(node.get("id").asString(), node); if
     * (!node.has("_originalWires")) { node.set("_originalWires",
     * node.get("wires").deepCopy()); } final JsonArray nodeWires =
     * node.get("wires").asJsonArray(); nodeWires.update(port,
     * Json.concat(nodeWires.get(port), sfn.get("wires").asJsonArray().get(i))); } }
     * } }
     */
    // Instantiate the nodes
    for (int i = 0; i < newNodes.size(); i++) {
      final JsonObject node = newNodes.get(i).asJsonObject();
      final String type = node.get("type").asString();

      if (!type.startsWith("subflow:")) {
        final Node newNode = FlowUtil.createNode(this, node);
        if (node != null) {
          activeNodes.put(newNode.getId(), newNode); // <================== ggeorg ===
          nodes.add(newNode);
        }
      } else {
        final String subflowId = type.substring("subflow:".length());
        final JsonObject _sf = subflows.has(subflowId)
            ? subflows.get(subflowId).asJsonObject()
            : globalSubflows.get(subflowId).asJsonObject();
        nodes.addAll(createSubflow(_sf, node, subflows, globalSubflows));
      }
    }

    subflowNode.getInstanceNodes().clear();

    for (Node n : nodes) {
      subflowNode.getInstanceNodes().put(n.getId(), n);
    }

    return nodes;
  }

  private static abstract class SubflowNode extends AbstractNode {
    private final Map<String, Node> instanceNodes;

    public SubflowNode(Flow flow, JsonObject config) {
      super(flow, config);
      instanceNodes = new HashMap<>();
    }

    public Map<String, Node> getInstanceNodes() { return instanceNodes; }

    public void _updateWires(JsonArray wires) {
      super.updateWires(wires);
    }

    @Override
    protected void onMessage(JsonObject msg) {
      send(msg);
    }
  }

  @Override
  public String toString() {
    return "Flow [flow=" + id + "]";
  }
}
