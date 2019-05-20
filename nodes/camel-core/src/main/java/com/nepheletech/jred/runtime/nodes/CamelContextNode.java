/*
 * Copyright NepheleTech, http://www.nephelerech.com
 *
 * This file is part of J-RED Nodes project.
 *
 * J-RED Nodes is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * J-RED Nodes is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this J-RED Nodes; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package com.nepheletech.jred.runtime.nodes;

import org.apache.camel.CamelContext;
import org.apache.camel.impl.DefaultCamelContext;

import com.nepheletech.jred.runtime.events.NodesStartedEvent;
import com.nepheletech.jred.runtime.events.NodesStartedEventListener;
import com.nepheletech.jred.runtime.events.NodesStoppedEvent;
import com.nepheletech.jred.runtime.events.NodesStoppedEventListener;
import com.nepheletech.jred.runtime.flows.Flow;
import com.nepheletech.jton.JtonObject;
import com.nepheletech.messagebus.MessageBus;

public class CamelContextNode extends AbstractConfigurationNode
    implements NodesStartedEventListener, NodesStoppedEventListener {

  private final DefaultCamelContext camelContext;

  public CamelContextNode(Flow flow, JtonObject config) {
    super(flow, config);

    camelContext = new DefaultCamelContext();
    camelContext.disableJMX();
  }

  public CamelContext getCamelContext() { return camelContext; }

  @Override
  public void onNodesStarted(NodesStartedEvent event) throws Exception {
    logger.trace(">>> onNodesStarted:");

    if (camelContext != null) {
      MessageBus.sendMessage(getId(), camelContext);

      try {
        camelContext.start();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }

  @Override
  public void onNodesStopped(NodesStoppedEvent event) throws Exception {
    logger.trace(">>> onNodesStopped:");

    if (camelContext != null) {
      try {
        camelContext.stop();
        //camelContext.removeRouteDefinitions(camelContext.getRouteDefinitions());
        // TODO camelContext registry RESET
      } catch (Exception e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
  }
}
