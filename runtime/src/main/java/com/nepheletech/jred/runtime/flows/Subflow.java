/*
 * Copyright NepheleTech, http://www.nephelerech.com
 *
 * This file is part of J-RED Runtime project.
 *
 * J-RED Runtime is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * J-RED Runtime is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this J-RED Runtime; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package com.nepheletech.jred.runtime.flows;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nepheletech.jred.runtime.nodes.AbstractNode;
import com.nepheletech.jred.runtime.nodes.Node;
import com.nepheletech.jred.runtime.util.JRedUtil;
import com.nepheletech.jton.JtonArray;
import com.nepheletech.jton.JtonElement;
import com.nepheletech.jton.JtonNull;
import com.nepheletech.jton.JtonObject;

/**
 * This class represents a subflow - which is handled as a special type of
 * {@link FlowImpl}.
 */
public class Subflow extends FlowImpl {
  private static final Logger logger = LoggerFactory.getLogger(Subflow.class);

  private final JtonObject subflowDef;
  private final JtonObject subflowInstance;
  private final JtonObject node_map;

  protected Node statusNode;

  protected SubflowNode node;

  private final JtonObject env;

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
  public Subflow(Flow parent, JtonObject globalFlow, JtonObject subflowDef, JtonObject subflowInstance) {
    this(parent, globalFlow, subflowDef, subflowInstance,
        subflowInternalFlowConfig(parent, globalFlow, subflowDef, subflowInstance));
  }

  public Subflow(Flow parent, JtonObject globalFlow, JtonObject subflowDef, JtonObject subflowInstance,
      JtonObject[] data) {
    super(parent, globalFlow, data[0]);

    this.subflowDef = subflowDef;
    this.subflowInstance = subflowInstance;
    this.node_map = data[1];

    final JtonObject env = new JtonObject();
    if (this.subflowDef.has("env")) {
      this.subflowDef.getAsJtonArray("env").forEach(e -> {
        env.set(e.asJtonObject().getAsString("name"), e);
      });
    }
    if (this.subflowInstance.has("env")) {
      this.subflowInstance.getAsJtonArray("env").forEach(e -> {
        env.set(e.asJtonObject().getAsString("name"), e);
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
  public void start(JtonObject diff) {
    if (this.subflowDef.has("status")) {
      final String subflowInstanceId = this.subflowInstance.getAsString("id");
      final JtonObject subflowStatusConfig = new JtonObject()
          .set("id", subflowInstanceId + ":status")
          .set("type", "subflow-status")
          .set("z", subflowInstanceId);

      this.statusNode = new AbstractNode(this, subflowStatusConfig) {
        @Override
        protected JtonElement onMessage(JtonObject msg) {
          if (msg.has("payload")) {
            final JtonElement payload = msg.get("payload");
            if (payload.isJtonPrimitive() && payload.asJtonPrimitive().isString()) {
              // if msg.payload is a String, use it as status text
            } else if (payload.isJtonObject()) {
              final JtonObject _payload = payload.asJtonObject();
              if (_payload.has("text") || _payload.has("fill") || _payload.has("shape")
                  || _payload.keySet().size() == 0) {
                // msg.payload is an object that looks like a status object
                throw new UnsupportedOperationException("status");
                //return;
              }
            }

            // Anything else - inspect it and use as status text
            // TODO
          } else if (msg.has("status")) {
            // if msg.status exists
            throw new UnsupportedOperationException("if status");
          }
          
          return null; // FIXME
        }
      };
    }

    final JtonObject subflowInstanceConfig = new JtonObject()
        .set("id", subflowInstance.get("id"))
        .set("type", subflowInstance.get("type"))
        .set("z", subflowInstance.get("z"))
        .set("name", subflowInstance.get("name"))
        .set("wires", new JtonArray());

    if (subflowDef.has("in")) {
      final JtonArray subflowInstanceConfigWires = new JtonArray();
      final JtonArray in = subflowDef.getAsJtonArray("in");
      for (int i = 0, iMax = in.size(); i < iMax; i++) {
        final JtonArray wires = in.getAsJtonObject(i).getAsJtonArray("wires").deepCopy();
        for (int j = 0, jMax = wires.size(); j < jMax; j++) {
          final JtonObject w = wires.getAsJtonObject(j);
          final JtonObject node = node_map.getAsJtonObject(w.getAsString("id"));
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
        final JtonArray out = subflowDef.getAsJtonArray("out");
        for (int i = 0; i < out.size(); i++) {
          final JtonArray wires = out.getAsJtonObject(i).getAsJtonArray("wires");
          for (int j = 0; j < wires.size(); j++) {
            final JtonObject wire_j = wires.getAsJtonObject(j);
            if (!wire_j.get("id").equals(subflowDef.get("id"))) {
              final JtonObject node = node_map.getAsJtonObject(wire_j.getAsString("id"));
              if (node.has("_originalWires")) {
                node.set("wires", node.get("_originalWires").deepCopy());
              }
            }
          }
        }

        final JtonObject modifiedNodes = new JtonObject();
        boolean subflowInstanceModified = false;

        for (int i = 0; i < out.size(); i++) {
          final JtonArray wires = out.getAsJtonObject(i).getAsJtonArray("wires");
          for (int j = 0; j < wires.size(); j++) {
            final JtonObject wire_j = wires.getAsJtonObject(j);
            final String wireId = wire_j.getAsString("id");
            final int wirePort = wire_j.getAsInt("port");
            if (wireId.equals(subflowDef.getAsString("id"))) {
              final JtonArray subflowWires = subflowInstance.getAsJtonArray("wires");
              subflowWires.set(wirePort, JtonArray.concat(subflowWires.get(wirePort), newWires.get(i)));
              subflowInstanceModified = true;
            } else {
              final JtonObject node = node_map.getAsJtonObject(wireId);
              node.getAsJtonArray("wires").set(wirePort,
                  JtonArray.concat(node.getAsJtonArray("wires").get(wirePort), newWires.get(i)));
              modifiedNodes.set(node.getAsString("id"), node);
            }
          }
        }

        modifiedNodes.keySet().forEach((id) -> {
          final JtonObject node = modifiedNodes.getAsJtonObject(id);
          activeNodes.get(id).updateWires(node.getAsJtonArray("wires"));
        });

        if (subflowInstanceModified) {
          subflowNode._updateWires(subflowInstance.getAsJtonArray("wires"));
        }
      }
    });

    // Wire the subflow outputs
    if (subflowDef.has("out")) {
      final JtonObject modifiedNodes = new JtonObject();
      final JtonArray out = subflowDef.getAsJtonArray("out");
      for (int i = 0, iMax = out.size(); i < iMax; i++) {
        // i: the output index
        // This is what this Output is wired to
        final JtonArray wires = out.getAsJtonObject(i).getAsJtonArray("wires");
        for (int j = 0, jMax = wires.size(); j < jMax; j++) {
          final JtonObject wire_j = wires.getAsJtonObject(j);
          final int port = wire_j.getAsInt("port");
          if (wire_j.get("id").equals(subflowDef.get("id"))) {
            // A subflow input wired straight to a subflow output
            final JtonArray subflowInstanceConfigWires = subflowInstanceConfig.getAsJtonArray("wires");
            subflowInstanceConfigWires.set(port,
                JtonArray.concat(subflowInstanceConfig.getAsJtonArray("wires").get(port),
                    this.subflowInstance.getAsJtonArray("wires").get(i)));
            this.node._updateWires(subflowInstance.getAsJtonArray("wires"));
          } else {
            final JtonObject node = node_map.getAsJtonObject(wire_j.getAsString("id"));
            modifiedNodes.set(node.getAsString("id"), node);
            if (!node.has("_originalWires")) {
              node.set("_originalWires", node.get("wires").deepCopy());
            }
            final JtonArray nodeWires = node.getAsJtonArray("wires");
            nodeWires.set(port,
                JtonArray.concat(nodeWires.get(port), subflowInstance.getAsJtonArray("wires").get(i)));
          }
        }
      }
    }

    // TODO subflowDef.status

    super.start(diff);
  }

  private static JtonObject[] subflowInternalFlowConfig(Flow parent,
      JtonObject globalFlow, JtonObject subflowDef, JtonObject subflowInstance) {
    final JtonObject subflows = ((FlowImpl) parent).flow.getAsJtonObject("subflows");
    final JtonObject globalSubflows = ((FlowImpl) parent).global.getAsJtonObject("subflows");

    final JtonObject node_map = new JtonObject();

    final JtonObject subflowInternalFlowConfig = new JtonObject()
        .set("id", subflowInstance.get("id"))
        .set("configs", new JtonObject())
        .set("nodes", new JtonObject())
        .set("subflows", new JtonObject());

    if (subflowDef.has("configs")) {
      // Clone all of the subflow config node definitions and give them new IDs
      final JtonObject configs = subflowDef.getAsJtonObject("configs");
      for (String i : configs.keySet()) {
        final JtonObject node = createNodeInSubflow(subflowInstance.getAsString("id"), configs.getAsJtonObject(i));
        node_map.set(node.getAsString("_alias"), node);
        subflowInternalFlowConfig.getAsJtonObject("configs").set(node.getAsString("id"), node);
      }
    }
    if (subflowDef.has("nodes")) {
      // Clone all of the subflow node definitions and give them new IDs
      final JtonObject nodes = subflowDef.getAsJtonObject("nodes");
      for (String i : nodes.keySet()) {
        final JtonObject node = createNodeInSubflow(subflowInstance.getAsString("id"), nodes.getAsJtonObject(i));
        node_map.set(node.getAsString("_alias"), node);
        subflowInternalFlowConfig.getAsJtonObject("nodes").set(node.getAsString("id"), node);
      }
    }

    subflowInternalFlowConfig.getAsJtonObject("subflows")
        .putAll(subflowDef.getAsJtonObject("subflows", true).deepCopy());

    remapSubflowNodes(subflowInternalFlowConfig.getAsJtonObject("configs"), node_map);
    remapSubflowNodes(subflowInternalFlowConfig.getAsJtonObject("nodes"), node_map);

    return new JtonObject[] { subflowInternalFlowConfig, node_map };
  }

  /**
   * Get environment variable of subflow.
   * 
   * @param name name of env var
   * @return value of env var
   */
  @Override
  public JtonElement getSetting(String name) {
    if (!name.startsWith("$parent.")) {
      if (env.has(name)) {
        final JtonObject val = env.getAsJtonObject(name);
        // If this is an env type property we need to be careful not
        // to get into lookup loops.
        // 1. if the value to lookup is the same as this one, go straight to parent
        // 2. otherwise, check if it is a compound env var ("foo $(bar)")
        // and if so, substitute any instances of `name` with $parent.name
        // See https://github.com/node-red/node-red/issues/2099
        if (!"env".equals(val.getAsString("type")) || !name.equals(val.getAsString("value"))) {
          String value = val.getAsString("value");
          if ("env".equals(val.getAsString("type"))) {
            value = value.replaceAll("\\${" + name + "}", "${$parent." + name + "}"); // XXX ???
          }
          try {
            return JRedUtil.evaluateNodeProperty(value, val.getAsString("type"), this.node, null);
          } catch (RuntimeException e) {
            e.printStackTrace(); // TODO
            return JtonNull.INSTANCE;
          }
        } else {
          // This _is_ an env property pointing at itself - go to parent
        }
      }
    } else {
      // name start $parent. ... so delegate to parent automatically
      name = name.substring(8);
    }

    return (parent != null)
        ? parent.getSetting(name)
        : JtonNull.INSTANCE;
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
  public boolean handleStatus(Node node, JtonObject statusMessage, Node reportingNode, boolean muteStatusEvent) {
    logger.trace(">>> handleStatus: statusMessage={}, muteStatusEvent={}", statusMessage, muteStatusEvent);

    boolean handled = super.handleStatus(node, statusMessage, reportingNode, muteStatusEvent);
    if (!handled) {
      // TODO
    }
    return handled;
  }

  @Override
  public boolean handleError(Node node, Throwable logMessage, JtonObject msg, Node reportingNode) {
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
  private static JtonObject createNodeInSubflow(String subflowInstanceId, JtonObject def) {
    final JtonObject node = def.deepCopy();
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
  private static void remapSubflowNodes(JtonObject nodes, JtonObject nodeMap) {
    for (String id : nodes.keySet()) {
      final JtonObject node = nodes.getAsJtonObject(id);
      if (node.has("wires")) {
        final JtonArray outputs = node.getAsJtonArray("wires");
        for (int j = 0, jMax = outputs.size(); j < jMax; j++) {
          final JtonArray wires = outputs.getAsJtonArray(j);
          for (int k = 0, kMax = wires.size(); k < kMax; k++) {
            wires.set(k, nodeMap.getAsJtonObject(wires.get(k).asString()).get("id"));
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
              node.set(prop, nodeMap.getAsJtonObject(value).get("id"));
            }
          }
        }
      }
    }
  }
}
