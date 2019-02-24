package com.nepheletech.jred.runtime.flows;

import java.util.HashSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nepheletech.jred.runtime.FlowsRuntime;
import com.nepheletech.json.JsonArray;
import com.nepheletech.json.JsonObject;

public final class Credentials {
  private static final Logger logger = LoggerFactory.getLogger(Credentials.class);

  private final FlowsRuntime flowsRuntime;

  private final JsonObject credentialCache = new JsonObject();

  private boolean dirty = false;
  private boolean encryptionEnabled;
  private boolean removeDefaultKey;

  public Credentials(FlowsRuntime flowsRuntime) {
    this.flowsRuntime = flowsRuntime;
  }

  public FlowsRuntime getFlowsRuntime() { return flowsRuntime; }

  public boolean isDirty() { return dirty; }

  /**
   * Deletes any credentials for nodes that no longer exist.
   * 
   * @param config a flow config
   * @return a promise for saving of credentials to storage.
   */
  public void clean(JsonArray config) {
    final HashSet<String> existingIds = new HashSet<>();
    config.forEach(_n -> {
      final JsonObject n = _n.asJsonObject();
      final String nId = n.get("id").asString();
      existingIds.add(nId);
      if (n.has("credentials")) {
        extract(n);
      }
    });
    boolean deletedCredentials = false;
    for (String c : credentialCache.keySet()) {
      if (!existingIds.contains(c)) {
        deletedCredentials = true;
        credentialCache.remove(c);
      }
    }
    if (deletedCredentials) {
      dirty = true;
    }
  }

  /**
   * Extracts and stores any credential updates in the provided node.
   * <p>
   * The provided node may have a {@code credentials} property that contains new
   * credentials for the node. This function loops through the credentials in the
   * definition for node-type and applies any of the updates provided in the node.
   * <p>
   * This function does not save the credentials to disk as it is expected to be
   * called multiple times when a new flow is deployed.
   * 
   * @param node the node to extract credentials from
   */
  public void extract(JsonObject node) {
    final String nodeID = node.get("id").asString();
    final String nodeType = node.get("type").asString();
    final JsonObject newCreds = node.get("credentials").asJsonObject(null);
    if (newCreds != null) {
      node.remove("credentials");

      throw new UnsupportedOperationException();
    }
  }

  public JsonObject export() {
    final JsonObject result = credentialCache;
    if (encryptionEnabled) {
      throw new UnsupportedOperationException();
    }
    dirty = false;
    if (removeDefaultKey) {
      throw new UnsupportedOperationException();
    } else {
      return result;
    }
  }
}
