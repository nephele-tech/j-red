package com.nepheletech.jred.runtime.flows;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nepheletech.jred.runtime.nodes.AbstractNode;
import com.nepheletech.jred.runtime.nodes.Node;
import com.nepheletech.jred.runtime.util.JRedUtil;
import com.nepheletech.json.JsonArray;
import com.nepheletech.json.JsonElement;
import com.nepheletech.json.JsonNull;
import com.nepheletech.json.JsonObject;

/**
 * This class represents a subflow - which is handled as a special type of
 * {@link FlowImpl}.
 */
public class Subflow extends FlowImpl {
  private static final Logger logger = LoggerFactory.getLogger(Subflow.class);

  private final JsonObject subflowDef;
  private final JsonObject subflowInstance;
  private final JsonObject node_map;

  protected Node statusNode;

  protected SubflowNode node;

  private final JsonObject env;

  /**
   * Create a {@code Subflow} object.
   * <p>
   * This takes a subflow definition and instance node, creates a clone of the
   * definition with unique ids applied and passes to the super class.
   * 
   * @param parent
   * @param globalFlow
   * @param subflowDef
   * @param subflowInstance
   */
  public Subflow(Flow parent, JsonObject globalFlow, JsonObject subflowDef, JsonObject subflowInstance) {
    this(parent, globalFlow, subflowDef, subflowInstance,
        subflowInternalFlowConfig(parent, globalFlow, subflowDef, subflowInstance));
  }

  public Subflow(Flow parent, JsonObject globalFlow, JsonObject subflowDef, JsonObject subflowInstance,
      JsonObject[] data) {
    super(parent, globalFlow, data[0]);

    this.subflowDef = subflowDef;
    this.subflowInstance = subflowInstance;
    this.node_map = data[1];

    final JsonObject env = new JsonObject();
    if (this.subflowDef.has("env")) {
      this.subflowDef.getAsJsonArray("env").forEach(e -> {
        env.set(e.asJsonObject().getAsString("name"), e);
      });
    }
    if (this.subflowInstance.has("env")) {
      this.subflowInstance.getAsJsonArray("env").forEach(e -> {
        env.set(e.asJsonObject().getAsString("name"), e);
      });
    }
    this.env = env;
  }

  /**
   * Start the subflow.
   * <p>
   * This creates a subflow instance node to handle the inbound messages. It also
   * rewires and subflow internal node that is connected to an output so it is
   * connected to the parent flow nodes the subflow instance is wired to.
   * 
   * @param diff
   */
  @Override
  public void start(JsonObject diff) {
    if (this.subflowDef.has("status")) {
      final String subflowInstanceId = this.subflowInstance.getAsString("id");
      final JsonObject subflowStatusConfig = new JsonObject()
          .set("id", subflowInstanceId + ":status")
          .set("type", "subflow-status")
          .set("z", subflowInstanceId);

      this.statusNode = new AbstractNode(this, subflowStatusConfig) {
        @Override
        protected void onMessage(JsonObject msg) {
          if (msg.has("payload")) {
            final JsonElement payload = msg.get("payload");
            if (payload.isJsonTransient() && payload.asJsonTransient().isString()) {
              // if msg.payload is a String, use it as status text
            } else if (payload.isJsonObject()) {
              final JsonObject _payload = payload.asJsonObject();
              if (_payload.has("text") || _payload.has("fill") || _payload.has("shape")
                  || _payload.keySet().size() == 0) {
                // msg.payload is an object that looks like a status object
                // TODO status
                return;
              }
            }

            // Anything else - inspect it and use as status text
            // TODO
          } else if (msg.has("status")) {

          }
        }
      };
    }

    final JsonObject subflowInstanceConfig = new JsonObject()
        .set("id", subflowInstance.get("id"))
        .set("type", subflowInstance.get("type"))
        .set("z", subflowInstance.get("z"))
        .set("name", subflowInstance.get("name"))
        .set("wires", new JsonArray());

    if (subflowDef.has("in")) {
      final JsonArray subflowInstanceConfigWires = new JsonArray();
      final JsonArray in = subflowDef.getAsJsonArray("in");
      for (int i = 0, iMax = in.size(); i < iMax; i++) {
        final JsonArray wires = in.getAsJsonObject(i).getAsJsonArray("wires").deepCopy();
        for (int j = 0, jMax = wires.size(); j < jMax; j++) {
          final JsonObject w = wires.getAsJsonObject(j);
          final JsonObject node = node_map.getAsJsonObject(w.getAsString("id"));
          wires.set(j, node.get("id")); // uuid
        }
        subflowInstanceConfigWires.push(wires);
      }
      subflowInstanceConfig.set("wires", subflowInstanceConfigWires);
      subflowInstanceConfig.set("_originalWires", subflowInstanceConfigWires.deepCopy()); // XXX Why?
    }

    this.node = new SubflowNode(this, subflowInstanceConfig);
    this.node.setUpdateWires((subflowNode, newWires) -> {
      // Wire the subflow outputs
      if (subflowDef.has("out")) {
        // Restore the original wiring to the internal nodes
        subflowInstanceConfig.set("wires", subflowInstanceConfig.get("_originalWires").deepCopy());
        final JsonArray out = subflowDef.getAsJsonArray("out");
        for (int i = 0; i < out.size(); i++) {
          final JsonArray wires = out.getAsJsonObject(i).getAsJsonArray("wires");
          for (int j = 0; j < wires.size(); j++) {
            final JsonObject wire_j = wires.getAsJsonObject(j);
            if (!wire_j.get("id").equals(subflowDef.get("id"))) {
              final JsonObject node = node_map.getAsJsonObject(wire_j.getAsString("id"));
              if (node.has("_originalWires")) {
                node.set("wires", node.get("_originalWires").deepCopy());
              }
            }
          }
        }

        final JsonObject modifiedNodes = new JsonObject();
        boolean subflowInstanceModified = false;

        for (int i = 0; i < out.size(); i++) {
          final JsonArray wires = out.getAsJsonObject(i).getAsJsonArray("wires");
          for (int j = 0; j < wires.size(); j++) {
            final JsonObject wire_j = wires.getAsJsonObject(j);
            final String wireId = wire_j.getAsString("id");
            final int wirePort = wire_j.getAsInt("port");
            if (wireId.equals(subflowDef.getAsString("id"))) {
              final JsonArray subflowWires = subflowInstance.getAsJsonArray("wires");
              subflowWires.set(wirePort, JsonArray.concat(subflowWires.get(wirePort), newWires.get(i)));
              subflowInstanceModified = true;
            } else {
              final JsonObject node = node_map.getAsJsonObject(wireId);
              node.getAsJsonArray("wires").set(wirePort,
                  JsonArray.concat(node.getAsJsonArray("wires").get(wirePort), newWires.get(i)));
              modifiedNodes.set(node.getAsString("id"), node);
            }
          }
        }

        modifiedNodes.keySet().forEach((id) -> {
          final JsonObject node = modifiedNodes.getAsJsonObject(id);
          activeNodes.get(id).updateWires(node.getAsJsonArray("wires"));
        });

        if (subflowInstanceModified) {
          subflowNode._updateWires(subflowInstance.getAsJsonArray("wires"));
        }
      }
    });

    // Wire the subflow outputs
    if (subflowDef.has("out")) {
      final JsonObject modifiedNodes = new JsonObject();
      final JsonArray out = subflowDef.getAsJsonArray("out");
      for (int i = 0, iMax = out.size(); i < iMax; i++) {
        // i: the output index
        // This is what this Output is wired to
        final JsonArray wires = out.getAsJsonObject(i).getAsJsonArray("wires");
        for (int j = 0, jMax = wires.size(); j < jMax; j++) {
          final JsonObject wire_j = wires.getAsJsonObject(j);
          final int port = wire_j.getAsInt("port");
          if (wire_j.get("id").equals(subflowDef.get("id"))) {
            // A subflow input wired straight to a subflow output
            final JsonArray subflowInstanceConfigWires = subflowInstanceConfig.getAsJsonArray("wires");
            subflowInstanceConfigWires.set(port,
                JsonArray.concat(subflowInstanceConfig.getAsJsonArray("wires").get(port),
                    this.subflowInstance.getAsJsonArray("wires").get(i)));
            this.node._updateWires(subflowInstance.getAsJsonArray("wires"));
          } else {
            final JsonObject node = node_map.getAsJsonObject(wire_j.getAsString("id"));
            modifiedNodes.set(node.getAsString("id"), node);
            if (!node.has("_originalWires")) {
              node.set("_originalWires", node.get("wires").deepCopy());
            }
            final JsonArray nodeWires = node.getAsJsonArray("wires");
            nodeWires.set(port,
                JsonArray.concat(nodeWires.get(port), subflowInstance.getAsJsonArray("wires").get(i)));
          }
        }
      }
    }

    // TODO subflowDef.status

    super.start(diff);
  }

  private static JsonObject[] subflowInternalFlowConfig(Flow parent,
      JsonObject globalFlow, JsonObject subflowDef, JsonObject subflowInstance) {
    final JsonObject subflows = ((FlowImpl) parent).flow.getAsJsonObject("subflows");
    final JsonObject globalSubflows = ((FlowImpl) parent).global.getAsJsonObject("subflows");

    final JsonObject node_map = new JsonObject();

    final JsonObject subflowInternalFlowConfig = new JsonObject()
        .set("id", subflowInstance.get("id"))
        .set("configs", new JsonObject())
        .set("nodes", new JsonObject())
        .set("subflows", new JsonObject());

    if (subflowDef.has("configs")) {
      // Clone all of the subflow config node definitions and give them new IDs
      final JsonObject configs = subflowDef.getAsJsonObject("configs");
      for (String i : configs.keySet()) {
        final JsonObject node = createNodeInSubflow(subflowInstance.getAsString("id"), configs.getAsJsonObject(i));
        node_map.set(node.getAsString("_alias"), node);
        subflowInternalFlowConfig.getAsJsonObject("configs").set(node.getAsString("id"), node);
      }
    }
    if (subflowDef.has("nodes")) {
      // Clone all of the subflow node definitions and give them new IDs
      final JsonObject nodes = subflowDef.getAsJsonObject("nodes");
      for (String i : nodes.keySet()) {
        final JsonObject node = createNodeInSubflow(subflowInstance.getAsString("id"), nodes.getAsJsonObject(i));
        node_map.set(node.getAsString("_alias"), node);
        subflowInternalFlowConfig.getAsJsonObject("nodes").set(node.getAsString("id"), node);
      }
    }

    subflowInternalFlowConfig.getAsJsonObject("subflows")
        .putAll(subflowDef.getAsJsonObject("subflows", true).deepCopy());

    remapSubflowNodes(subflowInternalFlowConfig.getAsJsonObject("configs"), node_map);
    remapSubflowNodes(subflowInternalFlowConfig.getAsJsonObject("nodes"), node_map);

    return new JsonObject[] { subflowInternalFlowConfig, node_map };
  }

  /**
   * Get environment variable of subflow.
   * 
   * @param name name of env var
   * @return value of env var
   */
  @Override
  public JsonElement getSetting(String name) {
    if (env.has(name)) {
      final JsonObject val = env.getAsJsonObject(name);
      try {
        return JRedUtil.evaluateNodeProperty(val.getAsString("value"), val.getAsString("type"), this.node, null);
      } catch (RuntimeException e) {
        e.printStackTrace(); // TODO
        return JsonNull.INSTANCE;
      }
    }

    return (parent != null)
        ? parent.getSetting(name)
        : JsonNull.INSTANCE;
  }

  /**
   * Get a node instance from this subflow.
   * <p>
   * If the subflow has a status node, check for that, otherwise use the
   * super-class function.
   * 
   * @param id
   * @param cancelBubble if {@code true}, prevents the flow passing the request to
   *                     the parent. This stops infinite loops when the parent
   *                     asked this Flow for the node to begin with.
   */
  @Override
  public Node getNode(String id, boolean cancelBubble) {
    if (this.statusNode != null && this.statusNode.getId().equals(id)) { return statusNode; }
    return super.getNode(id, cancelBubble);
  }

  @Override
  public boolean handleStatus(Node node, JsonObject statusMessage, Node reportingNode, boolean muteStatusEvent) {
    logger.trace(">>> handleStatus: statusMessage={}, muteStatusEvent={}", statusMessage, muteStatusEvent);

    boolean handled = super.handleStatus(node, statusMessage, reportingNode, muteStatusEvent);
    if (!handled) {
      // TODO
    }
    return handled;
  }

  @Override
  public boolean handleError(Node node, String logMessage, JsonObject msg, Node reportingNode) {
    logger.trace(">>> handleError: logMessage={}, msg={}", logMessage, msg);

    boolean handled = super.handleError(node, logMessage, msg, reportingNode);
    if (!handled) {
      // TODO
    }
    return handled;
  }

  /**
   * Clone a node definition for use within a subflow instance. Give the node a
   * new id and set its _alias property to record its association with the
   * original definition.
   * 
   * @param subflowInstanceId
   * @param def
   * @return
   */
  private static JsonObject createNodeInSubflow(String subflowInstanceId, JsonObject def) {
    final JsonObject node = def.deepCopy();
    final String nid = UUID.randomUUID().toString();
    node.set("_alias", node.get("id"));
    node.set("id", nid);
    node.set("z", subflowInstanceId);
    return node;
  }

  /**
   * Given an object of {id:nodes} and a map of {old-id:node}, modify all
   * properties in the nodes object to reference the new node ids.
   * <p>
   * This handles:
   * <ul>
   * <li>node.wires</li>
   * <li>node.scope of Catch and Status nodes</li>
   * <li>node.XYZ for any property where XYZ is recognized as an old property</li>
   * </ul>
   * 
   * @param nodes
   * @param nodeMap
   */
  private static void remapSubflowNodes(JsonObject nodes, JsonObject nodeMap) {
    for (String id : nodes.keySet()) {
      final JsonObject node = nodes.getAsJsonObject(id);
      if (node.has("wires")) {
        final JsonArray outputs = node.getAsJsonArray("wires");
        for (int j = 0, jMax = outputs.size(); j < jMax; j++) {
          final JsonArray wires = outputs.getAsJsonArray(j);
          for (int k = 0, kMax = wires.size(); k < kMax; k++) {
            wires.set(k, nodeMap.getAsJsonObject(wires.get(k).asString()).get("id"));
          }
        }
      }
      final String type = node.getAsString("type");
      if (("catch".equals(type) || "status".equals(type)) && node.has("scope")) {
        throw new UnsupportedOperationException("TODO");
      } else {
        for (String prop : node.keySet()) {
          if (!"_alias".equals(prop)) {
            final String value = node.getAsString(prop, null);
            if (value != null && nodeMap.has(value)) {
              node.set(prop, nodeMap.getAsJsonObject(value).get("id"));
            }
          }
        }
      }
    }
  }
}
