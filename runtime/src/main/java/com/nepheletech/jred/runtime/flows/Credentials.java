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

import java.util.HashSet;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nepheletech.jred.runtime.FlowsRuntime;
import com.nepheletech.jton.JtonArray;
import com.nepheletech.jton.JtonObject;

public final class Credentials {
  private static final Logger logger = LoggerFactory.getLogger(Credentials.class);

  private final FlowsRuntime flowsRuntime;

  private final JtonObject credentialCache = new JtonObject();
  //private final JsonObject credentialsDef = new JsonObject();

  private boolean dirty = false;
  private boolean encryptionEnabled;
  private boolean removeDefaultKey;

  public Credentials(FlowsRuntime flowsRuntime) {
    this.flowsRuntime = flowsRuntime;
  }

  public FlowsRuntime getFlowsRuntime() { return flowsRuntime; }

  public boolean isDirty() { return dirty; }
  
  /**
   * Sets the credentials from storage.
   */
  public void load(JtonObject credentials) {
    dirty = false;
    
    // TODO encrypted
    credentialCache.putAll(credentials);
  }

  /**
   * Gets the credentials for the given node id.
   * 
   * @param id the node id for the credentials
   * @return the credentials
   */
  public JtonObject get(String id) {
    return credentialCache.getAsJtonObject(id, true); // TODO false
  }

  /**
   * Deletes any credentials for nodes that no longer exist.
   * 
   * @param config a flow config
   * @return a promise for saving of credentials to storage.
   */
  public void clean(JtonArray config) {
    final HashSet<String> existingIds = new HashSet<>();
    config.forEach(_n -> {
      final JtonObject n = _n.asJtonObject();
      final String nId = n.getAsString("id");
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
  public void extract(JtonObject node) {
    logger.trace(">>> extract: node={}", node);
    
    final String nodeID = node.getAsString("id");
    
    final JtonObject newCreds = node.getAsJtonObject("credentials", false);
    
    if (newCreds != null) {
      node.remove("credentials");
      
      final JtonObject savedCredentials = credentialCache.getAsJtonObject(nodeID, true);
      for (String cred : newCreds.keySet()) {
        final String value = newCreds.getAsString(cred, null);
        if (StringUtils.trimToNull(value) == null) {
          savedCredentials.remove(cred);
          dirty = true;
          continue;
        }
        if (!savedCredentials.has(cred)
            || !savedCredentials.get(cred).equals(newCreds.get(cred))) {
          savedCredentials.set(cred, newCreds.get(cred));
          dirty = true;
        }
      }
      
      credentialCache.set(nodeID, savedCredentials);
    }
  }

  public JtonObject export() {
    final JtonObject result = credentialCache;
    if (encryptionEnabled) { throw new UnsupportedOperationException(); }
    dirty = false;
    if (removeDefaultKey) {
      throw new UnsupportedOperationException();
    } else {
      return result;
    }
  }
}
