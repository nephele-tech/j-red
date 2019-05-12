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
import com.nepheletech.jton.JtonArray;
import com.nepheletech.jton.JtonElement;
import com.nepheletech.jton.JtonNull;
import com.nepheletech.jton.JtonObject;
import com.nepheletech.jton.JtonPrimitive;
import com.nepheletech.messagebus.MessageBus;

public final class FlowUtil {
  private static final Logger logger = LoggerFactory.getLogger(FlowUtil.class);

  private static Pattern subflowInstanceRE = Pattern.compile("^subflow:(.+)$", Pattern.MULTILINE);

  public static JtonObject parseConfig(JtonArray config) {
    final JtonObject allNodes = new JtonObject();
    final JtonObject subflows = new JtonObject();
    final JtonObject configs = new JtonObject();
    final JtonObject flows = new JtonObject();
    final JtonArray missingTypes = new JtonArray();

    final JtonObject flow = new JtonObject()
        .set("allNodes", allNodes)
        .set("subflows", subflows)
        .set("configs", configs)
        .set("flows", flows)
        .set("missingTypes", missingTypes);

    config.forEach(_n -> {
      final JtonObject n = _n.asJtonObject();
      final String nId = n.get("id").asString();
      final String nType = n.get("type").asString();
      allNodes.set(nId, n.deepCopy());
      if ("tab".equals(nType)) {
        flows.set(nId, n);
        n.set("subflows", new JtonObject());
        n.set("configs", new JtonObject());
        n.set("nodes", new JtonObject());
      }
    });

    config.forEach(_n -> {
      final JtonObject n = _n.asJtonObject();
      final String nId = n.get("id").asString();
      final String nType = n.get("type").asString();
      if ("subflow".equals(nType)) {
        subflows.set(nId, n);
        n.set("configs", new JtonObject());
        n.set("nodes", new JtonObject());
        n.set("instances", new JtonArray());
      }
    });

    final JtonObject linkWires = new JtonObject();
    final JtonArray linkOutNodes = new JtonArray();
    config.forEach(_n -> {
      final JtonObject n = _n.asJtonObject();
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

        JtonObject container = null;
        if (flows.has(nZ)) {
          container = flows.get(nZ).asJtonObject();
        } else if (subflows.has(nZ)) {
          container = subflows.get(nZ).asJtonObject();
        }

        if (n.has("x") && n.has("y")) {
          if (subflowDetails) {
            final String subflowType = m.group(1);
            n.set("subflow", subflowType);
            subflows.get(subflowType).asJtonObject()
                .get("instances").asJtonArray().push(n);
          }
          if (container != null) {
            container.get("nodes").asJtonObject()
                .set(nId, n);
          }
        } else {
          if (container != null) {
            container.get("nodes").asJtonObject()
                .set(nId, n);
          } else {
            configs.set(nId, n);
            n.set("_users", new JtonArray());
          }
        }

        if ("link in".equals(nType) && n.has("links")) {
          // Ensure wires are present in corresponding link out nodes
          n.get("links").asJtonArray().forEach(_id -> {
            final String id = _id.asString();
            if (!linkWires.has(id)) {
              linkWires.set(id, new JtonObject());
            }
            final JtonObject linksMap = linkWires.get(id).asJtonObject();
            linksMap.set(nId, true);
          });
        } else if ("link out".equals(nType) && n.has("links")) {
          if (!linkWires.has(nId)) {
            linkWires.set(nId, new JtonObject());
          }
          n.get("links").asJtonArray().forEach(_id -> {
            final String id = _id.asString();
            linkWires.get(nId).asJtonObject().set(id, true);
          });
          linkOutNodes.push(n);
        }
      }
    });

    linkOutNodes.forEach(_n -> {
      final JtonObject n = _n.asJtonObject();
      final String nId = n.get("id").asString();
      final JtonObject links = linkWires.get(nId).asJtonObject();
      n.set("wires", new JtonArray().push(links.keys()));
    });

    final JtonObject addedTabs = new JtonObject();
    config.forEach(_n -> {
      final JtonObject n = _n.asJtonObject();
      final String nType = n.get("type").asString();
      if (!"subflow".equals(nType) && !"tab".equals(nType)) {
        final String nId = n.get("id").asString();
        for (Entry<String, JtonElement> entry : n.entrySet()) {
          final String prop = entry.getKey();
          final String value = entry.getValue().asString(null);
          if (n.has(prop) && !"id".equals(prop)
              && !"wires".equals(prop)
              && !"type".equals(prop)
              && !"_users".equals(prop)
              && configs.has(value)) {
            // This property references a global config node
            configs.get(value).asJtonObject().set("_users", nId);
          }
        }
        final String nZ = StringUtils.trimToNull(n.get("z").asString(null));
        if (nZ != null && !subflows.has(nZ)) {
          if (!flows.has(nZ)) {
            final JtonObject tab = new JtonObject().set("type", "tab").set("id", nZ);
            flows.set(nZ, tab);
            tab.set("subflows", new JtonObject());
            tab.set("configs", new JtonObject());
            tab.set("nodes", new JtonObject());
            addedTabs.set(nZ, tab);
          }
          if (addedTabs.has(nZ)) {
            final JtonObject tab = addedTabs.get(nZ).asJtonObject();
            if (n.has("x") && n.has("y")) {
              final JtonObject tabNodes = tab.get("nodes").asJtonObject();
              tabNodes.set(nId, n);
            } else {
              final JtonObject tabConfigs = tab.get("configs").asJtonObject();
              tabConfigs.set(nId, n);
            }
          }
        }
      }
    });

    return flow;
  }

  public static JtonObject diffConfigs(JtonObject oldConfig, JtonObject newConfig) {
    logger.trace(">>> diffConfigs: oldConfig={}, newConfig={}", oldConfig, newConfig);

    if (oldConfig == null) {
      oldConfig = new JtonObject()
          .set("flows", new JtonObject())
          .set("allNodes", new JtonObject());
    }

    final JtonObject changedSubflows = new JtonObject();

    final JtonObject added = new JtonObject();
    final JtonObject removed = new JtonObject();
    final JtonObject changed = new JtonObject();
    final JtonObject wiringChanged = new JtonObject();

    final JtonObject linkMap = new JtonObject();

    final HashSet<String> changedTabs = new HashSet<>();

    final JtonObject oldFlows = oldConfig.get("flows").asJtonObject();
    final JtonObject allOldNodes = oldConfig.get("allNodes").asJtonObject();

    final JtonObject newFlows = newConfig.get("flows").asJtonObject();
    final JtonObject allNewNodes = newConfig.get("allNodes").asJtonObject();

    // Look for tabs that have been removed
    for (String id : oldFlows.keySet()) {
      if (!newFlows.has(id)) {
        removed.set(id, allOldNodes.get(id));
      }
    }

    // Look for tabs that have been disabled
    for (String id : oldFlows.keySet()) {
      if (newFlows.has(id)) {
        final boolean originalState = oldFlows.get(id).asJtonObject()
            .get("disabled").asBoolean(false);
        final boolean newState = newFlows.get(id).asJtonObject()
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

    for (Entry<String, JtonElement> entry : allOldNodes.entrySet()) {
      final String id = entry.getKey();
      final JtonObject node = entry.getValue().asJtonObject();
      final String nodeType = node.get("type").asString();
      if (!"tab".equals(nodeType)) {
        // build the map of what this node was previously wired to
        if (node.has("wires")) {
          if (!linkMap.has(id)) {
            linkMap.set(id, new JtonArray());
          }
          final JtonArray nodeWires = node.get("wires").asJtonArray();
          for (int j = 0, jMax = nodeWires.size(); j < jMax; j++) {
            final JtonArray wires = nodeWires.get(j).asJtonArray();
            for (int k = 0, kMax = wires.size(); k < kMax; k++) {
              linkMap.get(id).asJtonArray().push(wires.get(k));
              final String wire = wires.get(k).asString();
              final JtonObject nn = allOldNodes.get(wire).asJtonObject(null);
              if (nn != null) {
                final String nnId = nn.get("id").asString();
                if (!linkMap.has(nnId)) {
                  linkMap.set(nnId, new JtonArray());
                }
                linkMap.get(nnId).asJtonArray().push(id);
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
            final JtonObject container = allNewNodes.getAsJtonObject(nodeZ);
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
            final JtonObject newNode = allNewNodes.get(id).asJtonObject();
            if (diffNodes(node, newNode) || newNode.has("credentials")) {
              changed.set(id, newNode);
              final String newNodeType = newNode.get("type").asString();
              if ("subflow".equals(newNodeType)) {
                changedSubflows.set(id, newNode);
              }
              // Mark the container as changed
              final String newNodeZ = newNode.getAsString("z", null);
              if (allNewNodes.has(newNodeZ)) {
                final JtonObject container = allNewNodes.get(newNodeZ).asJtonObject();
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
                final JtonObject container = allNewNodes.get(newNodeZ).asJtonObject();
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
    for (Entry<String, JtonElement> entry : allNewNodes.entrySet()) {
      final String id = entry.getKey();
      final JtonObject node = entry.getValue().asJtonObject();
      // build the map of what this node is now wired to
      if (node.has("wires")) {
        if (!linkMap.has(id)) {
          linkMap.set(id, new JtonArray());
        }
        final JtonArray nodeWires = node.get("wires").asJtonArray();
        for (int j = 0, jMax = nodeWires.size(); j < jMax; j++) {
          final JtonArray wires = nodeWires.get(j).asJtonArray();
          for (int k = 0, kMax = wires.size(); k < kMax; k++) {
            if (linkMap.get(id).asJtonArray().indexOf(wires.get(k)) == -1) {
              linkMap.get(id).asJtonArray().push(wires.get(k));
            }
            final String wire = wires.get(k).asString();
            final JtonObject nn = allNewNodes.get(wire).asJtonObject(null);
            if (nn != null) {
              if (!linkMap.has(wire)) {
                linkMap.set(wire, new JtonArray());
              }
              final String nnId = nn.get("id").asString();
              if (linkMap.get(nnId).asJtonArray().indexOf(node.get("id")) == -1) {
                linkMap.get(nnId).asJtonArray().push(node.get("id"));
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
          final JtonObject container = allNewNodes.get(nodeZ).asJtonObject();
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
      for (Entry<String, JtonElement> entry : allNewNodes.entrySet()) {
        final String id = entry.getKey();
        final JtonObject node = entry.getValue().asJtonObject();
        for (String prop : node.keySet()) {
          if (!"z".equals(prop) && !"id".equals(prop) && !"wires".equals(prop)
              && node.get(prop).isJtonPrimitive()) {
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
                  final JtonObject zNode = allNewNodes.get(nodeZ).asJtonObject();
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
    for (Entry<String, JtonElement> entry : allNewNodes.entrySet()) {
      final String id = entry.getKey();
      final JtonObject node = entry.getValue().asJtonObject(null);
      final String nodeZ = StringUtils.trimToNull(node.get("z").asString(null));
      if (nodeZ != null && allNewNodes.has(nodeZ)) {
        final JtonObject container = allNewNodes.get(nodeZ).asJtonObject();
        if ("subflow".equals(container.get("type").asString())) {
          changed.remove(id);
        }
      }
    }

    // Recursively mark all instances of changed subflows as changed
    final JtonArray changedSubflowStack = changedSubflows.keys();
    while (changedSubflowStack.size() > 0) {
      final String subflowId = changedSubflowStack.remove(0).asString();
      if (allNewNodes != null) {
        for (Entry<String, JtonElement> entry : allNewNodes.entrySet()) {
          final String id = entry.getKey();
          final JtonObject node = entry.getValue().asJtonObject(null);
          if (("subflow:" + subflowId).equals(node.get("type").asString())) {
            if (!changed.has(id)) {
              changed.set(id, node);
              final String z = node.get("z").asString();
              if (!changed.has(z) && allNewNodes.has(z)) {
                final JtonObject zNode = allNewNodes.get(z).asJtonObject();
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

    final JtonArray addedKeys = added.keys();
    final JtonArray changedKeys = changed.keys();
    final JtonArray removedKeys = removed.keys();
    final JtonArray rewiredKeys = wiringChanged.keys();
    final JtonArray linked = new JtonArray();
    final JtonObject diff = new JtonObject()
        .set("added", addedKeys)
        .set("changed", changedKeys)
        .set("removed", removedKeys)
        .set("rewired", rewiredKeys)
        .set("linked", linked);

    // Traverse the links of all modified nodes to mark the connected nodes
    final JtonArray modifiedNodes = JtonArray.concat(addedKeys, changedKeys, removedKeys, rewiredKeys);
    final HashSet<String> visited = new HashSet<>();
    while (modifiedNodes.size() > 0) {
      final JtonElement elem = modifiedNodes.remove(0);
      final String node = elem.asString();
      if (!visited.contains(node)) {
        visited.add(node);
        if (linkMap.has(node)) {
          if (!changed.has(node) && !added.has(node) && !removed.has(node) && !wiringChanged.has(node)) {
            linked.push(node);
          }
          modifiedNodes.addAll(linkMap.get(node).asJtonArray());
        }
      }
    }

    return diff;
  }

  private static boolean diffNodes(JtonObject oldNode, JtonObject newNode) {
    return !Objects.equals(oldNode, newNode);
  }

  private static void mapEnvVarProperties(JtonObject obj, String prop, Flow flow) {
    final JtonElement v = obj.get(prop);
    if (v.isJtonPrimitive()) {
      final JtonPrimitive p = v.asJtonPrimitive();
      if (p.isString()) {
        final String value = p.asString();
        if (value.startsWith("$") && value.matches("^\\$\\{(\\S+)\\}$")) {
          final String envVar = value.substring(2, value.length() - 1);
          final JtonElement r = flow.getSetting(envVar);
          obj.set(prop, r != JtonNull.INSTANCE ? r : v);
        }
      }
    } else if (v.isJtonArray()) {
      final JtonArray a = v.asJtonArray();
      for (int i = 0, iMax = a.size(); i < iMax; i++) {
        mapEnvVarProperties(a, i, flow);
      }
    } else if (v.isJtonObject()) {
      final JtonObject o = v.asJtonObject();
      for (String p : o.keySet()) {
        mapEnvVarProperties(o, p, flow);
      }
    }
  }

  private static void mapEnvVarProperties(JtonArray arr, int index, Flow flow) {
    final JtonElement v = arr.get(index);
    if (v.isJtonPrimitive()) {
      final JtonPrimitive p = v.asJtonPrimitive();
      if (p.isString()) {
        final String value = p.asString();
        if (value.startsWith("$") && value.matches("^\\$\\{(\\S+)\\}$")) {
          final String envVar = value.substring(2, value.length() - 1);
          final JtonElement r = flow.getSetting(envVar);
          arr.set(index, r != JtonNull.INSTANCE ? r : v);
        }
      }
    } else if (v.isJtonArray()) {
      final JtonArray a = v.asJtonArray();
      for (int i = 0, iMax = a.size(); i < iMax; i++) {
        mapEnvVarProperties(a, i, flow);
      }
    } else if (v.isJtonObject()) {
      final JtonObject o = v.asJtonObject();
      for (String p : o.keySet()) {
        mapEnvVarProperties(o, p, flow);
      }
    }
  }

  // TODO should be moved to a JSON helper class.
  private static JtonArray stackTrace(Throwable t) {
    final JtonArray stackTrace = new JtonArray();
    StackTraceElement elements[] = t.getStackTrace();
    for (StackTraceElement e : elements) {
      stackTrace.push(new JtonObject()
          .set("className", e.getClassName())
          .set("methodName", e.getMethodName())
          .set("fileName", e.getFileName())
          .set("lineNumber", e.getLineNumber()));
    }
    return stackTrace;
  }

  protected static void publish(String topic, JtonObject data) {
    publish(topic, topic, data);
  }

  protected static void publish(String localTopic, String topic, JtonObject data) {
    MessageBus.sendMessage(localTopic, new JtonObject()
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
  public static Node createNode(Flow flow, JtonObject config) {
    Node newNode = null;

    try {
      final String type = config.getAsString("type");
      final Constructor<?> nodeTypeConstructor = getConstructor(type);
      if (nodeTypeConstructor != null) {
        final JtonObject conf = config.deepCopy();
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

      publish("debug", new JtonObject()
          .set("id", config.get("id"))
          .set("name", config.get("name"))
          .set("type", config.get("type"))
          .set("level", 20)
          .set("msg", new JtonObject()
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
      constructor = nodeClass.getConstructor(Flow.class, JtonObject.class);
      constructors.put(type, constructor);
    }

    return constructor;
  }

  private FlowUtil() {}
}
