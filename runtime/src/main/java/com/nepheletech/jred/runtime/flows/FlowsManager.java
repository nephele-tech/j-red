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

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.camel.CamelContext;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nepheletech.jred.runtime.FlowsRuntime;
import com.nepheletech.jred.runtime.events.NodesStartedEvent;
import com.nepheletech.jred.runtime.events.NodesStoppedEvent;
import com.nepheletech.jred.runtime.events.RuntimeDeployEvent;
import com.nepheletech.jred.runtime.nodes.HasCredentials;
import com.nepheletech.jred.runtime.nodes.Node;
import com.nepheletech.jton.JtonArray;
import com.nepheletech.jton.JtonElement;
import com.nepheletech.jton.JtonObject;
import com.nepheletech.jton.JtonPrimitive;
import com.nepheletech.messagebus.MessageBus;

public final class FlowsManager {
  private static final Logger logger = LoggerFactory.getLogger(FlowsManager.class);

  private final FlowsRuntime flowsRuntime;

  private JtonObject activeConfig = null;
  private JtonObject activeFlowConfig = null;

  private final Map<String, Flow> activeFlows = new HashMap<>();
  private boolean started = false;
  private boolean credentialsPendingReset = false;

  private final Map<String, String> activeNodesToFlow = new HashMap<>();

  private final Credentials credentials;

  // TODO missing implementation
  private boolean settings_safeMode = false;

  public FlowsManager(FlowsRuntime flowsRuntime) {
    this.flowsRuntime = flowsRuntime;
    this.credentials = new Credentials(flowsRuntime);
  }

  public FlowsRuntime getFlowsRuntime() { return flowsRuntime; }

  public Credentials getCredentials() { return credentials; }

  private JtonObject loadFlows() {
    logger.trace(">>> loadFlows");

    try {
      final JtonObject config = flowsRuntime.getStorage().getFlows();
      // ---
      logger.debug("loaded flow revision: {}", config.getAsString("rev"));
      credentials.load(config.getAsJtonObject("credentials"));
      // TODO events.emit("runtime-event",{id:"runtime-state",retain:true});
      return config;
      // ---
    } catch (Exception err) {

      err.printStackTrace(); // TODO

      throw err;
    }
  }

  /**
   * Load the current flow configuration from storage.
   * 
   * @param forceStart
   * @return the revision of the flow
   */
  public String load(boolean forceStart) {
    logger.trace(">>> load: {}", forceStart);

    if (forceStart && settings_safeMode) {
      // This is a force reload from the API - disable safeMode
      settings_safeMode = false;
    }
    return setFlows(null, "load", forceStart);
  }

  /**
   * Sets the current active configuration.
   * 
   * @param _config new node array configuration.
   * @param type    the type of deployment to do: full (default), nodes, flows,
   *                load
   * @return the revision of the new flow.
   */
  public String setFlows(final JtonArray _config, final String type) {
    logger.trace(">>> setFlows: _config={}, type={}", _config, type);

    return setFlows(_config, type, false);
  }

  /**
   * Sets the current active configuration.
   * 
   * @param _config    new node array configuration.
   * @param type       the type of deployment to do: full (default), nodes, flows,
   *                   load
   * @param forceStart
   * @return the revision of the new flow.
   */
  public String setFlows(final JtonArray _config, String type, boolean forceStart) {
    logger.trace(">>> setFlows: _config={}, type={}, forceStart={}", _config, type, forceStart);

    type = (StringUtils.trimToNull(type) != null) ? type : "full";

    if (settings_safeMode) {
      if (!"load".equals(type)) {
        // If in safeMode, the flows are stopped. We cannot do a modified nodes/flows
        // type deploy as nothing is running. Can only do a "load" or "full" deploy.
        // The "load" case is already handled in `load()` to distinguish between
        // startup-load and api-request-load.
        type = "full";
        settings_safeMode = false;
      }
    }

    JtonArray config = null;
    JtonObject diff = null;
    JtonObject newFlowConfig;
    String flowRevision = null;
    boolean isLoad = false;
    if ("load".equals(type)) {
      isLoad = true;
      final JtonObject __config = loadFlows();
      // --- Future
      config = __config.getAsJtonArray("flows").deepCopy();
      newFlowConfig = FlowUtil.parseConfig(config.deepCopy());
      type = "full";
      flowRevision = __config.asString("rev"); // Future return
      // ---
    } else {
      // Clone the provided config so it can be manipulated
      config = _config.deepCopy();
      // Parse the configuration
      newFlowConfig = FlowUtil.parseConfig(config.deepCopy());
      // Generate a diff to identify what has changed
      diff = FlowUtil.diffConfigs(activeFlowConfig, newFlowConfig);

      // Now the flows have been compared, remove any credentials from newFlowConfig
      // so they don't cause false-positive diffs the next time a flow is deployed
      final JtonObject allNewNodes = newFlowConfig.getAsJtonObject("allNodes");
      for (JtonElement value : allNewNodes.values()) {
        if (value.asJtonObject().has("credentials")) {
          value.asJtonObject().remove("credentials");
        }
      }

      // Allow the credential store to remove anything no longer needed
      credentials.clean(config);

      // Remember whether credentials need saving or not
      final boolean credsDirty = credentials.isDirty();

      // Get the latest credentials and ask storage to save them (if needed)
      // as well as the new flow configuration.
      final JtonObject creds = credentials.export();
      // --- Future
      final JtonObject saveConfig = new JtonObject()
          .set("flows", config)
          .set("credentialsDirty", credsDirty)
          .set("credentials", creds);

      try {
        flowRevision = flowsRuntime.getStorage().saveFlows(saveConfig);
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
      // ---
    }

    // --- Future
    if (!isLoad) {
      logger.debug("saved flow revision: {}", flowRevision);
    }

    activeConfig = new JtonObject()
        .set("flows", config)
        .set("rev", flowRevision);

    activeFlowConfig = newFlowConfig;

    if (forceStart || started) {
      // Flows are running (or should be)

      // Stop the active flows (according to deploy type and the diff)
      stopFlows(type, diff);
      // TODO Once stopped allow context to remove anything no longer needed
      // context.clean(activeFlowConfig)

      // Start the active flows
      startFlows(type, diff);
    }

    MessageBus.sendMessage(new RuntimeDeployEvent(this, flowRevision));

    return flowRevision;
    // ---
  }

  public Node getNode(String id) {
    Node node = null;
    if (activeNodesToFlow.containsKey(id) && activeFlows.containsKey(activeNodesToFlow.get(id))) {
      Flow flow = activeFlows.get(activeNodesToFlow.get(id));
      node = flow.getNode(id, true);
    } else {
      for (final String flowId : activeFlows.keySet()) {
        node = activeFlows.get(flowId).getNode(id, true);
        if (node != null) {
          break;
        }
      }
    }
    return node;
  }

  /**
   * Gets the current flow configuration.
   * 
   * @return the active flow configuration
   */
  public JtonObject getFlows() { return activeConfig; }

  /**
   * Start the current flow configuration.
   */
  public void startFlows() {
    logger.trace(">>> startFlows:");
    
    try {
      startFlows("full", null);
    } catch (RuntimeException e) {
      e.printStackTrace();
    }
  }

  private void startFlows(String type, JtonObject diff) {
    logger.trace(">>> startFlows: type={}, diff={}", type, diff);

    type = StringUtils.trimToNull(type) != null ? type : "full";

    started = true;

    // If there are missing types, report them, emit the necessary runtime event
    // and return
    final JtonElement missingTypes = activeFlowConfig.get("missingTypes");
    if (missingTypes.isJtonArray() && missingTypes.asJtonArray().size() > 0) {
      throw new UnsupportedOperationException("TODO: missing types");
    }

    // In safe mode, don't actually start anything, emit the necessary runtime event
    // and return
    if (settings_safeMode) { throw new UnsupportedOperationException("safe mode"); }

    if (logger.isInfoEnabled()) {
      if (!"full".equals(type)) {
        logger.info("Starting modified {}", type);
      } else {
        logger.info("Starting flows");
      }
    }

    if ("full".equals(type)) {
      // A full start means everything should

      // Check the 'global' flow is running
      if (!activeFlows.containsKey("global")) {
        logger.debug("starting flow : global");
        activeFlows.put("global", new FlowImpl(flowAPI, activeFlowConfig));
      }

      // Check each flow in the active configuration
      final JtonObject flows = activeFlowConfig.getAsJtonObject("flows", false);
      if (flows != null) {
        for (Entry<String, JtonElement> entry : flows.entrySet()) {
          final String id = entry.getKey();
          final JtonObject flow = entry.getValue().asJtonObject();
          if (!flow.getAsBoolean("disabled", false) && !activeFlows.containsKey(id)) {
            // This flow is not disabled, nor is it currently active, so create it
            activeFlows.put(id, new FlowImpl(flowAPI, activeFlowConfig, flow));
            logger.debug("starting flow : {}", id);
          } else {
            logger.debug("not starting disabled flow : {}", id);
          }
        }
      }
    } else {
      // A modified-type deploy means restarting things that have changed

      // Update the global flow
      activeFlows.get("global").update(activeFlowConfig, activeFlowConfig);
      final JtonObject flows = activeFlowConfig.getAsJtonObject("flows", false);
      if (flows != null) {
        for (String id : flows.keySet()) {
          final JtonObject flow = flows.get(id).asJtonObject();
          if (!flow.getAsBoolean("disabled", false)) {
            if (activeFlows.containsKey(id)) {
              // This flow exists and is not disabled, so update it
              activeFlows.get(id).update(activeFlowConfig, flow);
            } else {
              // This flow didn't previously exist, so create it
              activeFlows.put(id, new FlowImpl(flowAPI, activeFlowConfig, flow));
              logger.debug("starting flow: {}", id);
            }
          } else {
            logger.debug("not starting disabled flow : {}", id);
          }
        }
      }
    }

    // Having created or updated all flows, now start them.
    for (Entry<String, Flow> activeFlow : activeFlows.entrySet()) {
      try {
        final String id = activeFlow.getKey();
        final Flow flow = activeFlow.getValue();

        flow.start(diff);

        // Create a map of node id to flow id
        final Map<String, Node> activeNodes = flow.getActiveNodes();
        activeNodes.keySet().forEach(nid -> {
          activeNodesToFlow.put(nid, id);
        });
      } catch (RuntimeException e) {
        e.printStackTrace();
      }
    }

    // See: events.emit("nodes-started");
    MessageBus.sendMessage(new NodesStartedEvent(this));

    if (credentialsPendingReset) {
      credentialsPendingReset = false;
    } else {
      // TODO events.emit("runtime-event",{id:"runtime-state",retain:true});
      MessageBus.sendMessage("runtime-event", new JtonObject()
          .set("id", "runtime-state")
          .set("retain", true));
    }

    if (logger.isInfoEnabled()) {
      if (!"full".equals(type)) {
        logger.info("Started modified {}", type);
      } else {
        logger.info("Started flows");
      }
    }
  }

  public void stopFlows() {
    stopFlows("full", null);
  }

  private void stopFlows(String type, JtonObject diff) {
    if (!started) { return; }

    type = StringUtils.trimToNull(type) != null ? type : "full";

    diff = diff != null ? diff
        : new JtonObject()
            .set("added", new JtonArray())
            .set("changed", new JtonArray())
            .set("removed", new JtonArray())
            .set("rewired", new JtonArray())
            .set("linked", new JtonArray());

    if (logger.isInfoEnabled()) {
      if (!"full".equals(type)) {
        logger.info("Stopping modified {}", type);
      } else {
        logger.info("Stopping flows");
      }
    }

    started = false;

    final JtonArray addedList = diff.get("added").asJtonArray();
    final JtonArray changedList = diff.get("changed").asJtonArray();
    final JtonArray removedList = diff.get("removed").asJtonArray();
    // final JtonArray rewiredList = diff.get("rewired").asJsonArray();
    final JtonArray linkedList = diff.get("linked").asJtonArray();

    JtonArray stopList = null;

    if ("nodes".equals(type)) {
      stopList = changedList.concat(removedList);
    } else if ("flows".equals(type)) {
      stopList = changedList.concat(removedList).concat(linkedList); // XXX
    }

    for (Iterator<Entry<String, Flow>> it = activeFlows.entrySet().iterator(); it.hasNext();) {
      final Entry<String, Flow> activeFlow = it.next();
      final String id = activeFlow.getKey();
      final Flow flow = activeFlow.getValue();
      final JtonPrimitive _id = new JtonPrimitive(id);
      final boolean flowStateChanged = addedList.indexOf(_id) != -1 || removedList.indexOf(_id) != -1;
      logger.debug("stopping flow: {}", id);
      flow.stop(flowStateChanged ? null : stopList, removedList);
      if ("full".equals(type) || flowStateChanged || removedList.indexOf(_id) != -1) {
        it.remove();
      }
    }

    for (Iterator<String> it = activeNodesToFlow.values().iterator(); it.hasNext();) {
      if (!activeFlows.containsKey(it.next())) {
        it.remove();
      }
    }

    if (stopList != null) {
      stopList.forEach(id -> {
        activeNodesToFlow.remove(id.asString());
      });
    }

    if (logger.isInfoEnabled()) {
      if (!"full".equals(type)) {
        logger.info("Stopping modified {}", type);
      } else {
        logger.info("Stopping flows");
      }
    }

    // events.emit("nodes-stopped");
    MessageBus.sendMessage(new NodesStoppedEvent(this));
  }

  // -----

  private Flow flowAPI = new Flow() {
    private final JtonObject context = new JtonObject();
    //=Settings.get().globalContext(); TODO use seed for global context

    @Override
    public Node getNode(String id) {
      return FlowsManager.this.getNode(id);
    }

    @Override
    public Node getNode(String id, boolean cancelBubble) {
      return FlowsManager.this.getNode(id);
    }

    @Override
    public Map<String, Node> getActiveNodes() {
      throw new UnsupportedOperationException();
    }

    @Override
    public void update(JtonObject global, JtonObject flow) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void start(JtonObject diff) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void stop(JtonArray stopList, JtonArray removedList) {
      throw new UnsupportedOperationException();
    }

    @Override
    public JtonElement getSetting(String key) {
      return new JtonPrimitive(System.getenv(key));
    }

    @Override
    public boolean handleStatus(Node node, JtonObject statusMessage, Node reportingNode, boolean muteStatusEvent) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean handleError(Node node, Throwable logMessage, JtonObject msg, Node reportingNode) {
      throw new UnsupportedOperationException();
    }

    @Override
    public JtonObject getContext(String type) {
      return context; // global context
    }

    @Override
    public void setup(Node node) {
      if (node instanceof HasCredentials) {
        ((HasCredentials) node).setCredentials(credentials.get(node.getId()));
      }
    }

    @Override
    public CamelContext getCamelContext() {
      return flowsRuntime.getCamelContext();
    }
  };
}
