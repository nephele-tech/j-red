package com.nepheletech.jred.runtime.flows;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.text.WordUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nepheletech.jred.runtime.nodes.Node;
import com.nepheletech.json.JsonArray;
import com.nepheletech.json.JsonElement;
import com.nepheletech.json.JsonNull;
import com.nepheletech.json.JsonObject;
import com.nepheletech.json.JsonPrimitive;
import com.nepheletech.messagebus.MessageBus;

public final class FlowUtil {
  private static final Logger logger = LoggerFactory.getLogger(FlowUtil.class);

  private static Pattern subflowInstanceRE = Pattern.compile("^subflow:(.+)$", Pattern.MULTILINE);

  public static JsonObject parseConfig(JsonArray config) {
    final JsonObject allNodes = new JsonObject();
    final JsonObject subflows = new JsonObject();
    final JsonObject configs = new JsonObject();
    final JsonObject flows = new JsonObject();
    final JsonArray missingTypes = new JsonArray();

    final JsonObject flow = new JsonObject()
        .set("allNodes", allNodes)
        .set("subflows", subflows)
        .set("configs", configs)
        .set("flows", flows)
        .set("missingTypes", missingTypes);

    config.forEach(_n -> {
      final JsonObject n = _n.asJsonObject();
      final String nId = n.get("id").asString();
      final String nType = n.get("type").asString();
      allNodes.set(nId, n.deepCopy());
      if ("tab".equals(nType)) {
        flows.set(nId, n);
        n.set("subflows", new JsonObject());
        n.set("configs", new JsonObject());
        n.set("nodes", new JsonObject());
      }
    });

    config.forEach(_n -> {
      final JsonObject n = _n.asJsonObject();
      final String nId = n.get("id").asString();
      final String nType = n.get("type").asString();
      if ("subflow".equals(nType)) {
        subflows.set(nId, n);
        n.set("configs", new JsonObject());
        n.set("nodes", new JsonObject());
        n.set("instances", new JsonArray());
      }
    });

    final JsonObject linkWires = new JsonObject();
    final JsonArray linkOutNodes = new JsonArray();
    config.forEach(_n -> {
      final JsonObject n = _n.asJsonObject();
      final String nType = n.get("type").asString();
      if (!"subflow".equals(nType) && !"tab".equals(nType)) {
        final String nId = n.get("id").asString();
        final String nZ = StringUtils.trimToNull(n.get("z").asString(null));

        final Matcher m = subflowInstanceRE.matcher(nType);
        final boolean subflowDetails = m.find();
        if ((subflowDetails && !subflows.has(m.group(1))) /* || (!subflowDetails && !true) */) {
          if (missingTypes.indexOf(n.get("type")) == -1) {
            missingTypes.push(n.get("type"));
          }
        }

        JsonObject container = null;
        if (flows.has(nZ)) {
          container = flows.get(nZ).asJsonObject();
        } else if (subflows.has(nZ)) {
          container = subflows.get(nZ).asJsonObject();
        }

        if (n.has("x") && n.has("y")) {
          if (subflowDetails) {
            final String subflowType = m.group(1);
            n.set("subflow", subflowType);
            subflows.get(subflowType).asJsonObject()
                .get("instances").asJsonArray().push(n);
          }
          if (container != null) {
            container.get("nodes").asJsonObject()
                .set(nId, n);
          }
        } else {
          if (container != null) {
            container.get("nodes").asJsonObject()
                .set(nId, n);
          } else {
            configs.set(nId, n);
            n.set("_users", new JsonArray());
          }
        }

        if ("link in".equals(nType) && n.has("links")) {
          // Ensure wires are present in corresponding link out nodes
          n.get("links").asJsonArray().forEach(_id -> {
            final String id = _id.asString();
            if (!linkWires.has(id)) {
              linkWires.set(id, new JsonObject());
            }
            final JsonObject linksMap = linkWires.get(id).asJsonObject();
            linksMap.set(nId, true);
          });
        } else if ("link out".equals(nType) && n.has("links")) {
          if (!linkWires.has(nId)) {
            linkWires.set(nId, new JsonObject());
          }
          n.get("links").asJsonArray().forEach(_id -> {
            final String id = _id.asString();
            linkWires.get(nId).asJsonObject().set(id, true);
          });
          linkOutNodes.push(n);
        }
      }
    });

    linkOutNodes.forEach(_n -> {
      final JsonObject n = _n.asJsonObject();
      final String nId = n.get("id").asString();
      final JsonObject links = linkWires.get(nId).asJsonObject();
      n.set("wires", new JsonArray().push(links.keys()));
    });

    final JsonObject addedTabs = new JsonObject();
    config.forEach(_n -> {
      final JsonObject n = _n.asJsonObject();
      final String nType = n.get("type").asString();
      if (!"subflow".equals(nType) && !"tab".equals(nType)) {
        final String nId = n.get("id").asString();
        for (Entry<String, JsonElement> entry : n.entrySet()) {
          final String prop = entry.getKey();
          final String value = entry.getValue().asString(null);
          if (n.has(prop) && !"id".equals(prop)
              && !"wires".equals(prop)
              && !"type".equals(prop)
              && !"_users".equals(prop)
              && configs.has(value)) {
            // This property references a global config node
            configs.get(value).asJsonObject().set("_users", nId);
          }
        }
        final String nZ = StringUtils.trimToNull(n.get("z").asString(null));
        if (nZ != null && !subflows.has(nZ)) {
          if (!flows.has(nZ)) {
            final JsonObject tab = new JsonObject().set("type", "tab").set("id", nZ);
            flows.set(nZ, tab);
            tab.set("subflows", new JsonObject());
            tab.set("configs", new JsonObject());
            tab.set("nodes", new JsonObject());
            addedTabs.set(nZ, tab);
          }
          if (addedTabs.has(nZ)) {
            final JsonObject tab = addedTabs.get(nZ).asJsonObject();
            if (n.has("x") && n.has("y")) {
              final JsonObject tabNodes = tab.get("nodes").asJsonObject();
              tabNodes.set(nId, n);
            } else {
              final JsonObject tabConfigs = tab.get("configs").asJsonObject();
              tabConfigs.set(nId, n);
            }
          }
        }
      }
    });

    return flow;
  }

  public static JsonObject diffConfigs(JsonObject oldConfig, JsonObject newConfig) {
    logger.trace(">>> diffConfigs: oldConfig={}, newConfig={}", oldConfig, newConfig);

    if (oldConfig == null) {
      oldConfig = new JsonObject()
          .set("flows", new JsonObject())
          .set("allNodes", new JsonObject());
    }

    final JsonObject changedSubflows = new JsonObject();

    final JsonObject added = new JsonObject();
    final JsonObject removed = new JsonObject();
    final JsonObject changed = new JsonObject();
    final JsonObject wiringChanged = new JsonObject();

    final JsonObject linkMap = new JsonObject();

    final HashSet<String> changedTabs = new HashSet<>();

    final JsonObject oldFlows = oldConfig.get("flows").asJsonObject();
    final JsonObject allOldNodes = oldConfig.get("allNodes").asJsonObject();

    final JsonObject newFlows = newConfig.get("flows").asJsonObject();
    final JsonObject allNewNodes = newConfig.get("allNodes").asJsonObject();

    // Look for tabs that have been removed
    for (String id : oldFlows.keySet()) {
      if (!newFlows.has(id)) {
        removed.set(id, allOldNodes.get(id));
      }
    }

    // Look for tabs that have been disabled
    for (String id : oldFlows.keySet()) {
      if (newFlows.has(id)) {
        final boolean originalState = oldFlows.get(id).asJsonObject()
            .get("disabled").asBoolean(false);
        final boolean newState = newFlows.get(id).asJsonObject()
            .get("disabled").asBoolean(false);
        if (originalState != newState) {
          changedTabs.add(id);
          if (originalState) {
            added.set(id, allOldNodes.get(id));
          } else {
            removed.set(id, allOldNodes.get(id));
          }
        }
      }
    }

    for (Entry<String, JsonElement> entry : allOldNodes.entrySet()) {
      final String id = entry.getKey();
      final JsonObject node = entry.getValue().asJsonObject();
      final String nodeType = node.get("type").asString();
      if (!"tab".equals(nodeType)) {
        // build the map of what this node was previously wired to
        if (node.has("wires")) {
          if (!linkMap.has(id)) {
            linkMap.set(id, new JsonArray());
          }
          final JsonArray nodeWires = node.get("wires").asJsonArray();
          for (int j = 0, jMax = nodeWires.size(); j < jMax; j++) {
            final JsonArray wires = nodeWires.get(j).asJsonArray();
            for (int k = 0, kMax = wires.size(); k < kMax; k++) {
              linkMap.get(id).asJsonArray().push(wires.get(k));
              final String wire = wires.get(k).asString();
              final JsonObject nn = allOldNodes.get(wire).asJsonObject(null);
              if (nn != null) {
                final String nnId = nn.get("id").asString();
                if (!linkMap.has(nnId)) {
                  linkMap.set(nnId, new JsonArray());
                }
                linkMap.get(nnId).asJsonArray().push(id);
              }
            }
          }
        }
        // This node has been removed
        final String nodeZ = StringUtils.trimToNull(node.getAsString("z", null));
        if (removed.has(nodeZ) || !allNewNodes.has(id)) {
          removed.set(id, node);
          // Mark the container as changed
          if (!removed.has(nodeZ) && allNewNodes.has(nodeZ)) {
            final JsonObject container = allNewNodes.getAsJsonObject(nodeZ);
            final String containerType = container.get("type").asString();
            changed.set(nodeZ, container);
            if ("subflow".equals(containerType)) {
              changedSubflows.set(nodeZ, container);
              // removed.remove(id);
            }
          }
        } else {
          if (added.has(nodeZ)) {
            added.set(id, node);
          } else {
            // This node has a material configuration change
            final JsonObject newNode = allNewNodes.get(id).asJsonObject();
            if (diffNodes(node, newNode) || newNode.has("credentials")) {
              changed.set(id, newNode);
              final String newNodeType = newNode.get("type").asString();
              if ("subflow".equals(newNodeType)) {
                changedSubflows.set(id, newNode);
              }
              // Mark the container as changed
              final String newNodeZ = newNode.getAsString("z", null);
              if (allNewNodes.has(newNodeZ)) {
                final JsonObject container = allNewNodes.get(newNodeZ).asJsonObject();
                changed.set(newNodeZ, container);
                final String containerType = container.get("type").asString();
                if ("subflow".equals(containerType)) {
                  changedSubflows.set(newNodeZ, container);
                  changed.remove(newNodeZ);
                }
              }
            }
            // This node's wiring has changed
            if (!Objects.equals(node.get("wires"), newNode.get("wires"))) {
              wiringChanged.set(id, newNode);
              // Mark the container as changed
              final String newNodeZ = newNode.get("z").asString();
              if (allNewNodes.has(newNodeZ)) {
                final JsonObject container = allNewNodes.get(newNodeZ).asJsonObject();
                changed.set(newNodeZ, container);
                final String containerType = container.get("type").asString();
                if ("subflow".equals(containerType)) {
                  changedSubflows.set(newNodeZ, container);
                  changed.remove(newNodeZ);
                }
              }
            }
          }
        }
      }
    }

    // Look for added nodes
    for (Entry<String, JsonElement> entry : allNewNodes.entrySet()) {
      final String id = entry.getKey();
      final JsonObject node = entry.getValue().asJsonObject();
      // build the map of what this node is now wired to
      if (node.has("wires")) {
        if (!linkMap.has(id)) {
          linkMap.set(id, new JsonArray());
        }
        final JsonArray nodeWires = node.get("wires").asJsonArray();
        for (int j = 0, jMax = nodeWires.size(); j < jMax; j++) {
          final JsonArray wires = nodeWires.get(j).asJsonArray();
          for (int k = 0, kMax = wires.size(); k < kMax; k++) {
            if (linkMap.get(id).asJsonArray().indexOf(wires.get(k)) == -1) {
              linkMap.get(id).asJsonArray().push(wires.get(k));
            }
            final String wire = wires.get(k).asString();
            final JsonObject nn = allNewNodes.get(wire).asJsonObject(null);
            if (nn != null) {
              if (!linkMap.has(wire)) {
                linkMap.set(wire, new JsonArray());
              }
              final String nnId = nn.get("id").asString();
              if (linkMap.get(nnId).asJsonArray().indexOf(node.get("id")) == -1) {
                linkMap.get(nnId).asJsonArray().push(node.get("id"));
              }
            }
          }
        }
      }
      // This node has been added
      if (!allOldNodes.has(id)) {
        added.set(id, node);
        // Mark the container as changed
        final String nodeZ = StringUtils.trimToNull(node.get("z").asString(null));
        if (allNewNodes.has(nodeZ)) {
          final JsonObject container = allNewNodes.get(nodeZ).asJsonObject();
          changed.set(nodeZ, container);
          final String containerType = container.get("type").asString();
          if ("subflow".equals(containerType)) {
            changedSubflows.set(nodeZ, container);
            changed.remove(nodeZ);
          }
        }
      }
    }

    boolean madeChange;
    // Look through the nodes looking for references to changed config nodes.
    // Repeat the loop if anything is marked as changed as it may need to be
    // propagated to parent nodes.
    // TODO: looping through all nodes every time is a bit inneficient - could be
    // more targeted
    do {
      madeChange = false;
      for (Entry<String, JsonElement> entry : allNewNodes.entrySet()) {
        final String id = entry.getKey();
        final JsonObject node = entry.getValue().asJsonObject();
        for (String prop : node.keySet()) {
          if (!"z".equals(prop) && !"id".equals(prop) && !"wires".equals(prop)
              && node.get(prop).isJsonPrimitive()) {
            // This node may have a property that references a changed/removed
            // node. Assume it is a config node change and mark this node as
            // changed.
            final String nodeRef = node.get(prop).asString(null);
            if (nodeRef != null && (changed.has(nodeRef) || removed.has(nodeRef))) {
              if (!changed.has(id)) {
                madeChange = true;
                changed.set(id, node);
                // This node exists within subflow template. Mark the
                // template as having changed.
                final String nodeZ = StringUtils.trimToNull(node.get("z").asString(null));
                if (allNewNodes.has(nodeZ)) {
                  final JsonObject zNode = allNewNodes.get(nodeZ).asJsonObject();
                  changed.set(nodeZ, zNode);
                  final String zNodeType = zNode.get("type").asString();
                  if ("subflow".equals(zNodeType)) {
                    changedSubflows.set(nodeZ, zNode);
                  }
                }
              }
            }
          }
        }
      }
    } while (madeChange);

    // Find any nodes that exists on a subflow template and remove from changed
    // list as the parent subflow will now be marked as containing a change.
    for (Entry<String, JsonElement> entry : allNewNodes.entrySet()) {
      final String id = entry.getKey();
      final JsonObject node = entry.getValue().asJsonObject(null);
      final String nodeZ = StringUtils.trimToNull(node.get("z").asString(null));
      if (nodeZ != null && allNewNodes.has(nodeZ)) {
        final JsonObject container = allNewNodes.get(nodeZ).asJsonObject();
        if ("subflow".equals(container.get("type").asString())) {
          changed.remove(id);
        }
      }
    }

    // Recursively mark all instances of changed subflows as changed
    final JsonArray changedSubflowStack = changedSubflows.keys();
    while (changedSubflowStack.size() > 0) {
      final String subflowId = changedSubflowStack.remove(0).asString();
      if (allNewNodes != null) {
        for (Entry<String, JsonElement> entry : allNewNodes.entrySet()) {
          final String id = entry.getKey();
          final JsonObject node = entry.getValue().asJsonObject(null);
          if (("subflow:" + subflowId).equals(node.get("type").asString())) {
            if (!changed.has(id)) {
              changed.set(id, node);
              final String z = node.get("z").asString();
              if (!changed.has(z) && allNewNodes.has(z)) {
                final JsonObject zNode = allNewNodes.get(z).asJsonObject();
                changed.set(z, zNode);
                if ("subflow".equals(zNode.get("type").asString())) {
                  // This subflow instance is inside a subflow. Add the
                  // containing subflow to the stack to mark
                  changedSubflowStack.push(z);
                  changed.remove(id);
                }
              }
            }
          }
        }
      }
    }

    final JsonArray addedKeys = added.keys();
    final JsonArray changedKeys = changed.keys();
    final JsonArray removedKeys = removed.keys();
    final JsonArray rewiredKeys = wiringChanged.keys();
    final JsonArray linked = new JsonArray();
    final JsonObject diff = new JsonObject()
        .set("added", addedKeys)
        .set("changed", changedKeys)
        .set("removed", removedKeys)
        .set("rewired", rewiredKeys)
        .set("linked", linked);

    // Traverse the links of all modified nodes to mark the connected nodes
    final JsonArray modifiedNodes = JsonArray.concat(addedKeys, changedKeys, removedKeys, rewiredKeys);
    final HashSet<String> visited = new HashSet<>();
    while (modifiedNodes.size() > 0) {
      final JsonElement elem = modifiedNodes.remove(0);
      final String node = elem.asString();
      if (!visited.contains(node)) {
        visited.add(node);
        if (linkMap.has(node)) {
          if (!changed.has(node) && !added.has(node) && !removed.has(node) && !wiringChanged.has(node)) {
            linked.push(node);
          }
          modifiedNodes.addAll(linkMap.get(node).asJsonArray());
        }
      }
    }

    return diff;
  }

  private static boolean diffNodes(JsonObject oldNode, JsonObject newNode) {
    return !Objects.equals(oldNode, newNode);
  }

  private static void mapEnvVarProperties(JsonObject obj, String prop, Flow flow) {
    final JsonElement v = obj.get(prop);
    if (v.isJsonPrimitive()) {
      final JsonPrimitive p = v.asJsonPrimitive();
      if (p.isString()) {
        final String value = p.asString();
        if (value.startsWith("$") && value.matches("^\\$\\{(\\S+)\\}$")) {
          final String envVar = value.substring(2, value.length() - 1);
          final JsonElement r = flow.getSetting(envVar);
          obj.set(prop, r != JsonNull.INSTANCE ? r : v);
        }
      }
    } else if (v.isJsonArray()) {
      final JsonArray a = v.asJsonArray();
      for (int i = 0, iMax = a.size(); i < iMax; i++) {
        mapEnvVarProperties(a, i, flow);
      }
    } else if (v.isJsonObject()) {
      final JsonObject o = v.asJsonObject();
      for (String p : o.keySet()) {
        mapEnvVarProperties(o, p, flow);
      }
    }
  }

  private static void mapEnvVarProperties(JsonArray arr, int index, Flow flow) {
    final JsonElement v = arr.get(index);
    if (v.isJsonPrimitive()) {
      final JsonPrimitive p = v.asJsonPrimitive();
      if (p.isString()) {
        final String value = p.asString();
        if (value.startsWith("$") && value.matches("^\\$\\{(\\S+)\\}$")) {
          final String envVar = value.substring(2, value.length() - 1);
          final JsonElement r = flow.getSetting(envVar);
          arr.set(index, r != JsonNull.INSTANCE ? r : v);
        }
      }
    } else if (v.isJsonArray()) {
      final JsonArray a = v.asJsonArray();
      for (int i = 0, iMax = a.size(); i < iMax; i++) {
        mapEnvVarProperties(a, i, flow);
      }
    } else if (v.isJsonObject()) {
      final JsonObject o = v.asJsonObject();
      for (String p : o.keySet()) {
        mapEnvVarProperties(o, p, flow);
      }
    }
  }

  // TODO should be moved to a JSON helper class.
  private static JsonArray stackTrace(Throwable t) {
    final JsonArray stackTrace = new JsonArray();
    StackTraceElement elements[] = t.getStackTrace();
    for (StackTraceElement e : elements) {
      stackTrace.push(new JsonObject()
          .set("className", e.getClassName())
          .set("methodName", e.getMethodName())
          .set("fileName", e.getFileName())
          .set("lineNumber", e.getLineNumber()));
    }
    return stackTrace;
  }

  protected static void publish(String topic, JsonObject data) {
    publish(topic, topic, data);
  }

  protected static void publish(String localTopic, String topic, JsonObject data) {
    MessageBus.sendMessage(localTopic, new JsonObject()
        .set("topic", topic)
        .set("data", data));
  }

  /**
   * Create a new instance of a node.
   * 
   * @param flow   the containing flow
   * @param config the node configuration object
   * @return the instance of the node
   */
  public static Node createNode(Flow flow, JsonObject config) {
    Node newNode = null;

    try {
      final String type = config.getAsString("type");
      final Constructor<?> nodeTypeConstructor = getConstructor(type);
      if (nodeTypeConstructor != null) {
        final JsonObject conf = config.deepCopy();
        conf.remove("credentials");
        for (String p : conf.keySet()) {
          mapEnvVarProperties(conf, p, flow);
        }
        newNode = (Node) nodeTypeConstructor.newInstance(flow, conf);
      } else {
        // TODO Log.error(Log._("nodes.flow.unknown-type", {type:type}));
      }
    } catch (Exception e) {
      e.printStackTrace();

      Throwable rootCause = ExceptionUtils.getRootCause(e);
      if (rootCause == null) {
        rootCause = e;
      }

      final StringBuilder sb = new StringBuilder()
          .append(rootCause.getClass())
          .append(": ")
          .append(rootCause.getMessage());

      publish("debug", new JsonObject()
          .set("id", config.get("id"))
          .set("name", config.get("name"))
          .set("type", config.get("type"))
          .set("level", 20)
          .set("msg", new JsonObject()
              .set("message", sb.toString())
              .set("stackTrace", stackTrace(e))
              .set("config", config))
          .set("timestamp", System.currentTimeMillis())
          .set("format", "string[" + sb.toString().length() + "]"));
    }

    return newNode;
  }

  private static final String NODE_PKG_NAME = Node.class.getPackage().getName();
  private static final Map<String, Constructor<? extends Node>> constructors = new HashMap<>();

  private static Constructor<? extends Node> getConstructor(String type)
      throws ClassNotFoundException, NoSuchMethodException, SecurityException {
    Constructor<? extends Node> constructor = constructors.get(type);

    if (constructor == null) {
      final String cn = String.format("%s.%sNode", NODE_PKG_NAME, 
          WordUtils.capitalize(WordUtils.capitalizeFully(type), '-').replaceAll("\\s+|\\-", ""));
      @SuppressWarnings("unchecked")
      final Class<? extends Node> nodeClass = (Class<? extends Node>) FlowUtil.class.getClassLoader().loadClass(cn);
      constructor = nodeClass.getConstructor(Flow.class, JsonObject.class);
      constructors.put(type, constructor);
    }

    return constructor;
  }

  private FlowUtil() {}
}
