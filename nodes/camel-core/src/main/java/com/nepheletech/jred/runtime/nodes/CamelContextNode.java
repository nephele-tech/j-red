/*
 * Copyright NepheleTech and other contributorns, http://www.nephelerech.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
