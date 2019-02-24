package com.nepheletech.flows.runtime.flows;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nepheletech.flows.runtime.FlowsRuntime;
import com.nepheletech.flows.runtime.events.NodesStartedEvent;
import com.nepheletech.flows.runtime.events.RuntimeDeployEvent;
import com.nepheletech.flows.runtime.nodes.Node;
import com.nepheletech.json.JsonArray;
import com.nepheletech.json.JsonElement;
import com.nepheletech.json.JsonObject;
import com.nepheletech.json.JsonPrimitive;
import com.nepheletech.messagebus.MessageBus;

public final class FlowsManager {
  private static final Logger logger = LoggerFactory.getLogger(FlowsManager.class);

  private final FlowsRuntime flowsRuntime;

  private JsonObject activeConfig = null;
  private JsonObject activeFlowConfig = null;

  private final Map<String, FlowImpl> activeFlows = new HashMap<>();
  private boolean started = false;
  private boolean credentialsPendingReset = false;

  private final Map<String, String> activeNodesToFlow = new HashMap<>();
  private final JsonObject subflowInstanceNodeMap = new JsonObject();

  private final Credentials credentials;

  public FlowsManager(FlowsRuntime flowsRuntime) {
    this.flowsRuntime = flowsRuntime;
    this.credentials = new Credentials(flowsRuntime);
  }

  public FlowsRuntime getFlowsRuntime() { return flowsRuntime; }

  private JsonObject loadFlows() {
    logger.trace(">>> loadFlows");

    final JsonObject config = flowsRuntime.getStorage().getFlows();
    logger.debug("loaded flow revision: {}", config.get("rev").asString());

    // TODO credentials
    // TODO error handling

    return config;
  }

  /**
   * Load the current flow configuration from storage.
   * 
   * @param forceStart
   * @return the revision of the flow
   */
  public String load(boolean forceStart) {
    logger.trace(">>> load: {}", forceStart);

    if (forceStart /* settings.safeMode */) {
      // This is a force reload from the API - disable safeMode
      // TODO delete settings.safeMode
    }
    return setFlows(null, "load", forceStart);
  }

  /**
   * Sets the current active config.
   * 
   * @param _config the configuration to enable.
   * @param type    the type of deployment to do: full (default), nodes, flows,
   *                load
   * @return the revision of the new flow
   */
  public String setFlows(final JsonArray _config, final String type) {
    logger.trace(">>> setFlows: _config={}, type={}", _config, type);

    return setFlows(_config, type, false);
  }

  public String setFlows(final JsonArray _config, String type, boolean forceStart) {
    type = (StringUtils.trimToNull(type) != null) ? type : "full";

    logger.trace(">>> setFlows: _config={}, type={}, forceStart={}", _config, type, forceStart);

    // if (settings.safeMode) {
    if (!"load".equals(type)) {
      // If in safeMode, the flows are stopped. We cannont do a modified nodes/flows
      // type deploy as nothing is running. Can only do a "load" or "full" deploy.
      // The "load" case is already handled in `load()` to distinguish between
      // startup-load and api-request-load.
      type = "full";
      // TODO delete settings.safeMode
    }
    // }

    JsonArray config = null;
    JsonObject diff = null;
    JsonObject newFlowConfig;
    String flowRevision = null;
    boolean isLoad = false;
    if ("load".equals(type)) {
      isLoad = true;
      final JsonObject __config = loadFlows();
      config = __config.get("flows").asJsonArray();
      newFlowConfig = FlowUtil.parseConfig(config.deepCopy());
      type = "full";
      flowRevision = __config.get("rev").asString();
    } else {
      // Clone the provided config so it can be manipulated
      config = _config.deepCopy();
      // Parse the configuration
      newFlowConfig = FlowUtil.parseConfig(config.deepCopy());
      // Generate a diff to identify what has changed
      diff = FlowUtil.diffConfigs(activeFlowConfig, newFlowConfig);

      // Now the flows have been compared, remove any credentials from newFlowConfig
      // so they don't cause false-positive diffs the next time a flow is deployed
      final JsonObject allNewNodes = newFlowConfig.get("allNodes").asJsonObject();
      for (JsonElement value : allNewNodes.values()) {
        value.asJsonObject().remove("credentials");
      }

      // Allow the credential store to remove anything no longer needed
      credentials.clean(config);

      // Remember whether credentials need saving or not
      final boolean credsDirty = credentials.isDirty();

      // Get the latest credentials and ask storage to save them (if needed)
      // as well as the new flow configuration.
      final JsonObject creds = credentials.export();
      final JsonObject saveConfig = new JsonObject()
          .set("flows", config)
          .set("credentialsDirty", credsDirty)
          .set("credentials", creds);

      try {
        flowRevision = flowsRuntime.getStorage().saveFlows(saveConfig);
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }

    if (!isLoad) {
      logger.debug("saved flow revision: {}", flowRevision);
    }

    activeConfig = new JsonObject()
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
  }

  public Node getNode(String id) {
    Node node = null;
    if (activeNodesToFlow.containsKey(id) && activeFlows.containsKey(activeNodesToFlow.get(id))) {
      FlowImpl flow = activeFlows.get(activeNodesToFlow.get(id));
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
  public JsonObject getFlows() { return activeConfig; }

  /**
   * Start the current flow configuration.
   */
  public void startFlows() {
    try {
      startFlows("full", null);
    } catch (RuntimeException e) {
      e.printStackTrace();
    }
  }

  private void startFlows(String type, JsonObject diff) {
    type = StringUtils.trimToNull(type) != null ? type : "full";

    started = true;

    // If there are missing types, report them, emit the necessary runtime event and
    // return
    final JsonElement missingTypes = activeFlowConfig.get("missingTypes");
    if (missingTypes.isJsonArray() && missingTypes.asJsonArray().size() > 0) {
      throw new UnsupportedOperationException("missing types");
    }

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
        activeFlows.put("global", new FlowImpl(flowsRuntime, activeFlowConfig));
      }

      // Check each flow in the active configuration
      final JsonObject flows = activeFlowConfig.get("flows").asJsonObject(null);
      if (flows != null) {
        for (Entry<String, JsonElement> entry : flows.entrySet()) {
          final String id = entry.getKey();
          final JsonObject flow = entry.getValue().asJsonObject();
          if (!flow.get("disabled").asBoolean(false) && !activeFlows.containsKey(id)) {
            // This flow is not disabled, nor is it currently active, so create it
            activeFlows.put(id, new FlowImpl(flowsRuntime, activeFlowConfig, flow));
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
      final JsonObject flows = activeFlowConfig.get("flows").asJsonObject(null);
      if (flows != null) {
        for (String id : flows.keySet()) {
          final JsonObject flow = flows.get(id).asJsonObject();
          if (!flow.get("disabled").asBoolean(false)) {
            if (activeFlows.containsKey(id)) {
              // This flow exists and is not disabled, so update it
              activeFlows.get(id).update(activeFlowConfig, flow);
            } else {
              // This flow didn't previously exist, so create it
              activeFlows.put(id, new FlowImpl(flowsRuntime, activeFlowConfig, flow));
              logger.debug("starting flow: {}", id);
            }
          } else {
            logger.debug("not starting disabled flow : {}", id);
          }
        }
      }
    }

    // Having created or updated all flows, now start them.
    for (Entry<String, FlowImpl> activeFlow : activeFlows.entrySet()) {
      final String id = activeFlow.getKey();
      final FlowImpl flow = activeFlow.getValue();

      flow.start(diff);

      // Create a map of node id and also a subflowInstance lookup map
      final Map<String, Node> activeNodes = flow.getActiveNodes();
      for (Entry<String, Node> activeNodesEntry : activeNodes.entrySet()) { // FIXME new code
        final String activeNodeId = activeNodesEntry.getKey();
        activeNodesToFlow.put(activeNodeId, id);

        //@formatter:off
//        final Node activeNode = activeNodesEntry.getValue();
//        if (activeNode.getAlias() != null) {
//          if (!subflowInstanceNodeMap.has(activeNode.getAlias())) {
//            subflowInstanceNodeMap.set(activeNode.getAlias(), new JsonArray());
//          }
//          subflowInstanceNodeMap.get(activeNode.getAlias()).asJsonArray().push(activeNodeId);
//        }
        //@formatter:on

      }
    }

    // See: events.emit("nodes-started");
    MessageBus.sendMessage(new NodesStartedEvent(this));

    if (credentialsPendingReset) {
      credentialsPendingReset = false;
    } else {
      // TODO events.emit("runtime-event",{id:"runtime-state",retain:true});
      MessageBus.sendMessage("runtime-event", new JsonObject()
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

  private void stopFlows(String type, JsonObject diff) {
    if (!started) { return; }

    type = StringUtils.trimToNull(type) != null ? type : "full";

    diff = diff != null ? diff
        : new JsonObject()
            .set("added", new JsonArray())
            .set("changed", new JsonArray())
            .set("removed", new JsonArray())
            .set("rewired", new JsonArray())
            .set("linked", new JsonArray());

    if (logger.isInfoEnabled()) {
      if (!"full".equals(type)) {
        logger.info("Stopping modified {}", type);
      } else {
        logger.info("Stopping flows");
      }
    }

    started = false;

    final JsonArray addedList = diff.get("added").asJsonArray();
    final JsonArray changedList = diff.get("changed").asJsonArray();
    final JsonArray removedList = diff.get("removed").asJsonArray();
    // final JtonArray rewiredList = diff.get("rewired").asJsonArray();
    final JsonArray linkedList = diff.get("linked").asJsonArray();

    JsonArray stopList = null;

    if ("nodes".equals(type)) {
      stopList = changedList.concat(removedList);
    } else if ("flows".equals(type)) {
      stopList = changedList.concat(removedList).concat(linkedList); // XXX
    }

    for (Iterator<Entry<String, FlowImpl>> it = activeFlows.entrySet().iterator(); it.hasNext();) {
      final Entry<String, FlowImpl> activeFlow = it.next();
      final String id = activeFlow.getKey();
      final FlowImpl flow = activeFlow.getValue();
      final JsonPrimitive _id = new JsonPrimitive(id);
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

    // TODO events.emit("nodes-stopped");
  }
}
