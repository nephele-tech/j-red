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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import org.apache.camel.CamelContext;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nepheletech.jred.runtime.nodes.CatchNode;
import com.nepheletech.jred.runtime.nodes.Node;
import com.nepheletech.jred.runtime.nodes.StatusNode;
import com.nepheletech.jred.runtime.util.JRedUtil;
import com.nepheletech.jton.JtonArray;
import com.nepheletech.jton.JtonElement;
import com.nepheletech.jton.JtonObject;
import com.nepheletech.messagebus.MessageBus;

/**
 * This class represents a flow within the runtime. It is responsible for
 * creating, starting and stopping all nodes within the flow.a
 */
public class FlowImpl implements Flow {
  private static final Logger logger = LoggerFactory.getLogger(FlowImpl.class);

  protected final Flow parent;

  protected JtonObject global;
  protected JtonObject flow;

  @SuppressWarnings("unused")
  private final boolean isGlobalFlow;

  private final String id;

  protected final Map<String, Node> activeNodes;
  private final Map<String, Subflow> subflowInstanceNodes;
  private final List<CatchNode> catchNodes;
  private final List<StatusNode> statusNodes;

  private final JtonObject context;

  /**
   * Create a {@code Flow} object.
   * 
   * @param flowsRuntime
   * @param globalFlow   The global flow definition
   */
  public FlowImpl(Flow parent, JtonObject globalFlow) {
    this(parent, globalFlow, null);
  }

  /**
   * Create a {@code Flow} object.
   * 
   * @param flowsRuntime
   * @param globalFlow   The global flow definition.
   * @param flow         This flow's definition.
   */
  public FlowImpl(Flow parent, JtonObject globalFlow, JtonObject flow) {
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
    this.catchNodes = new ArrayList<>();
    this.statusNodes = new ArrayList<>();
    this.context = new JtonObject();
  }
  
  @Override
  public String getPath() {
    return id;
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
  public void start(final JtonObject diff) {
    logger.trace(">>> start: id={}, diff={}", this.id, diff);

    Node newNode = null;

    catchNodes.clear();
    statusNodes.clear();

    final JtonObject configs = flow.getAsJtonObject("configs", true);
    final List<String> configNodes = new ArrayList<>(configs.keySet());
    final JtonObject configNodesAttempts = new JtonObject();
    while (configNodes.size() > 0) {
      final String id = configNodes.remove(0);
      final JtonObject node = configs.getAsJtonObject(id);
      if (!activeNodes.containsKey(id)) {
        if (!node.getAsBoolean("d", false)) {
          boolean readyToCreate = true;
          // This node doesn't exist.
          // Check it doesn't reference another non-existing config node.
          for (String prop : node.keySet()) {
            if (!"id".equals(prop) && !"wires".equals(prop) && !"_users".equals(prop)) {
              final String configRef = node.getAsString(prop, null);
              if (configRef != null && configs.has(configRef)
                  && !configs.getAsJtonObject(configRef).getAsBoolean("d", false)) {
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
        } else {
          logger.debug("not starting disabled config node : {}", id);
        }
      }
    }

    if (diff != null && diff.has("rewired")) {
      final JtonArray rewired = diff.getAsJtonArray("rewired");
      for (JtonElement rewireNodeRef : rewired) {
        final Node rewireNode = activeNodes.get(rewireNodeRef.asString());
        if (rewireNode != null) {
          final JtonObject flowNodes = flow.getAsJtonObject("nodes");
          final JtonObject node = flowNodes.getAsJtonObject(rewireNode.getId());
          rewireNode.updateWires(node.getAsJtonArray("wires"));
        }
      }
    }

    final JtonObject flowNodes = flow.getAsJtonObject("nodes", false);
    if (flowNodes != null) {
      for (String id : flowNodes.keySet()) {
        final JtonObject node = flowNodes.getAsJtonObject(id);
        if (!node.getAsBoolean("d", false)) {
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
                final JtonObject flowSubflows = flow.getAsJtonObject("subflows");
                final JtonObject globalSubflows = global.getAsJtonObject("subflows");
                final JtonObject subflowDefinition = flowSubflows.has(subflowRef)
                    ? flowSubflows.getAsJtonObject(subflowRef)
                    : globalSubflows.getAsJtonObject(subflowRef);
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
        } else {
          logger.debug("not starting disabled node: {}", id);
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
        catchNodes.add((CatchNode) node);
      } else if (node instanceof StatusNode) {
        statusNodes.add((StatusNode) node);
      }
    }

    catchNodes.sort(new Comparator<CatchNode>() {
      @Override
      public int compare(CatchNode o1, CatchNode o2) {
        if (o1.getScope() != null && o2.getScope() == null) {
          return -1;
        } else if (o1.getScope() == null && o2.getScope() != null) {
          return 1;
        } else if (o1.isUncaught() && !o2.isUncaught()) {
          return 1;
        } else if (!o1.isUncaught() && o2.isUncaught()) { return -1; }
        return 0;
      }
    });

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
  public void stop(JtonArray stopList, JtonArray removedList) {
    logger.trace(">>> stop: stopList: {}, removedList: {}", stopList, removedList);

    if (stopList == null) {
      stopList = JtonObject.keys(this.activeNodes);
      logger.debug("stopList=", stopList);
    }

    if (removedList == null) {
      removedList = new JtonArray();
      logger.debug("removedList=", removedList);
    }

    // Convert the list to a map to avoid multiple scans of the list
    final HashSet<String> removedMap = new HashSet<>();
    for (JtonElement e : removedList) {
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
      node.close(removed);
      logger.trace("Stopped node: {}:{}", node.getType(), node.getId());
    } catch (RuntimeException e) {
      logger.error("Error stopping node: {}:{}", node.getType(), node.getId());
      logger.debug(e.getMessage(), e);
    }
  }

  /**
   * Update the flow definition. This doesn't change anything that is running.
   * This should be called after {@link #stop(JtonArray, JtonArray)} and before
   * {@link #start(JtonObject)}.
   * 
   * @param global
   * @param flow
   */
  @Override
  public void update(JtonObject global, JtonObject flow) {
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

    final JtonObject configs = flow.getAsJtonObject("configs", false);
    final JtonObject nodes = flow.getAsJtonObject("nodes", false);
    if ((configs != null && configs.has(id)) || (nodes != null && nodes.has(id))) {
      // This is a node owned by this flow, so return whatever we have got. 
      // During a stop/restart, activeNodes could be null for this id
      return activeNodes.get(id);
    } else if (activeNodes.containsKey(id)) {
      // TEMP: this is a subflow internal node within this flow
      return activeNodes.get(id);
    } else if (subflowInstanceNodes.containsKey(id)) {
      throw new UnsupportedOperationException("TODO");
      // TODO return subflowInstanceNodes.get(id);
    } else if (cancelBubble) {
      // The node could be inside one of this flow's subflows
      for (String sfId : this.subflowInstanceNodes.keySet()) {
        var node = this.subflowInstanceNodes.get(sfId).getNode(id, cancelBubble);
        if (node != null) {
          return node;
        }
      }
    } else {
      // Node not found inside this flow - ask the parent
      return parent.getNode(id);
    }
    
    return null;
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
  public JtonElement getSetting(String key) {
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
  public boolean handleStatus(final Node node, final JtonObject statusMessage, Node reportingNode,
      final boolean muteStatusEvent) {
    logger.trace(">>> handleStatus: node={}, statusMessage={}, reportingNode={}, muteStatusEvent={}",
        node, statusMessage, reportingNode, muteStatusEvent);
    
    if (reportingNode == null) {
      reportingNode = node;
    }
    if (!muteStatusEvent) {
      MessageBus.sendMessage("node-status", new JtonObject()
          .set("id", Optional.ofNullable(node.getAlias()).orElse(node.getId()))
          .set("status", statusMessage));
    }

    boolean handled = false;

    if ("global".equals(this.id)) {
      // This is a global config node
      // Delegate status to any nodes using this config node

      throw new UnsupportedOperationException("global");
      
    } else {
      for (StatusNode targetStatusNode : statusNodes) {
        //@formatter:off
//        final Set<String> scope = targetStatusNode.getScope();
//        if (scope != null && !scope.contains(reportingNode.getId())) {
//          break;
//        }
        //@formatter:on
        
        final JtonObject message = new JtonObject()
            .set("status", statusMessage.deepCopy());
        
        if (statusMessage.has("text")) {
          message.getAsJtonObject("status").set("text", statusMessage.getAsString("text"));
        }

        message.getAsJtonObject("status").set("source", new JtonObject()
            .set("id", node.getAlias() != null ? node.getAlias() : node.getId())
            .set("type", node.getType())
            .set("name", node.getName()));
        
        targetStatusNode.receive(message);
        handled = true;
      }
    }

    return handled;
  }

  /**
   * Handle an error event from a node within this flow. If there are no
   * {@link CatchNode}s within this flow, pass the event to the parent flow.
   */
  @Override
  public boolean handleError(final Node node, final Throwable logMessage, final JtonObject msg, Node reportingNode) {
    logger.error(">>> handleError: {}", logMessage);

    if (reportingNode == null) {
      reportingNode = node;
    }

    int count = 1;
    if (msg != null && msg.isJtonObject("error")) {
      final JtonObject error = msg.getAsJtonObject("error");
      if (error.isJtonObject("source")) {
        final JtonObject source = error.getAsJtonObject("source");
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

      throw new UnsupportedOperationException("global");

    } else {
      boolean handledByUncaught = false;

      for (CatchNode targetCatchNode : catchNodes) {
        final Set<String> scope = targetCatchNode.getScope();
        if (scope != null && !scope.contains(reportingNode.getId())) {
          break;
        }
        if (scope == null && targetCatchNode.isUncaught() && !handledByUncaught) {
          if (handled) {
            // This has been handled by a !uncaught catch node
            break;
          }
          // This is an uncaught error
          handledByUncaught = true;
        }
        final JtonObject errorMessage = msg != null ? msg.deepCopy() : new JtonObject();
        if (errorMessage.has("error")) {
          errorMessage.set("_error", errorMessage.get("error"));
        }

        Throwable rootCause = ExceptionUtils.getRootCause(logMessage);
        if (rootCause == null) {
          rootCause = logMessage;
        }

        errorMessage.set("error", new JtonObject()
            .set("message", rootCause.toString())
            .set("source", new JtonObject()
                .set("id", node.getId())
                .set("type", node.getType())
                .set("name", node.getName())
                .set("count", count))
            .set("stack", JRedUtil.stackTrace(rootCause)));
        
        targetCatchNode.receive(errorMessage);
        handled = true;
      }
    }

    return handled;
  }

  @Override
  public String toString() {
    return "Flow [flow=" + id + "]";
  }

  @Override
  public JtonObject getContext(String type) {
    if ("flow".equalsIgnoreCase(type)) {
      return context;
    } else {
      return parent.getContext(type);
    }
  }
  
  @Override
  public void setup(Node node) {
    parent.setup(node);
  }

  @Override
  public CamelContext getCamelContext() {
    return parent.getCamelContext();
  }
}
