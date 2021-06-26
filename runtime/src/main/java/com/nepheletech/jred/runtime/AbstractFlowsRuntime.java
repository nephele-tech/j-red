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
package com.nepheletech.jred.runtime;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.apache.camel.CamelContext;
import org.apache.camel.impl.DefaultCamelContext;

import com.nepheletech.jred.runtime.flows.FlowsManager;
import com.nepheletech.jred.runtime.nodes.Node;
import com.nepheletech.jred.runtime.storage.Storage;
import com.nepheletech.jton.JtonArray;
import com.nepheletech.jton.JtonObject;

public class AbstractFlowsRuntime implements FlowsRuntime {

  private final Storage storage;
  private final FlowsManager flowsManager;
  private final CamelContext camelContext;

  protected AbstractFlowsRuntime(Storage storage) {
    this.storage = storage;
    this.flowsManager = new FlowsManager(this);
    this.camelContext = new DefaultCamelContext();
    this.camelContext.disableJMX();
    this.camelContext.getShutdownStrategy().setSuppressLoggingOnTimeout(true);
    this.camelContext.getShutdownStrategy().setLogInflightExchangesOnTimeout(false);
    this.camelContext.getShutdownStrategy().setTimeout(3);
    this.camelContext.getShutdownStrategy().setTimeUnit(TimeUnit.SECONDS);
  }

  @Override
  public CamelContext getCamelContext() {
    return camelContext;
  }

  @Override
  public Storage getStorage() { return storage; }

  public String loadFlows() {
    return loadFlows(false);
  }

  @Override
  public String loadFlows(boolean forceStart) {
    return flowsManager.load(forceStart);
  }

  @Override
  public void startFlows() {
    flowsManager.startFlows();
    
    camelContext.start();
  }

  @Override
  public void stopFlows() {
    camelContext.stop();
    flowsManager.stopFlows();
  }

  @Override
  public JtonObject getFlows() { return flowsManager.getFlows(); }

  @Override
  public String setFlows(JtonArray config, String type) throws IOException {
    return flowsManager.setFlows(config, type);
  }

  @Override
  public Node getNode(String id) {
    return flowsManager.getNode(id);
  }

  @Override
  public JtonObject getNodeCredentials(String type, String id) {
    return flowsManager.getCredentials().get(id);
    
    // TODO some things I don't follow... (runtime/lib/api/flows.js)
  }

  @Override
  public JtonObject getGlobalContext() {
    return flowsManager.getGlobalContext();
  }
}
