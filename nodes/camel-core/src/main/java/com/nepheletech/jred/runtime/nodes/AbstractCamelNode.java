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

import com.nepheletech.jred.runtime.flows.Flow;
import com.nepheletech.jton.JtonObject;
import com.nepheletech.messagebus.MessageBus;
import com.nepheletech.messagebus.MessageBusListener;
import com.nepheletech.messagebus.Subscription;

public abstract class AbstractCamelNode extends AbstractNode {

  private final String camelContextRef;

  private final Subscription camelContextStartupSubscription;

  public AbstractCamelNode(Flow flow, JtonObject config) {
    super(flow, config);

    this.camelContextRef = config.getAsString("context");

    if (this.camelContextRef != null) {
      this.camelContextStartupSubscription = MessageBus
          .subscribe(CamelContextNode.byRef(this.camelContextRef),
              new MessageBusListener<CamelContext>() {
                @Override
                public void messageSent(String topic, CamelContext camelContext) {
                  try {
                    addRoutes(camelContext);
                  } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                  }
                }
              });
    } else {
      this.camelContextStartupSubscription = null;
    }
  }

  protected CamelContext getCamelContext() {
    return ((CamelContextNode) getFlow().getNode(camelContextRef)).getCamelContext();
  }

  protected abstract void addRoutes(CamelContext camelContext) throws Exception;

  @Override
  protected void onClosed(boolean removed) {
    logger.trace(">>> onClosed: {}", removed);

    if (/* removed && */ camelContextRef != null) {
      camelContextStartupSubscription.unsubscribe();
    }
  }
}
