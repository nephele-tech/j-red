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
          .subscribe(this.camelContextRef, new MessageBusListener<CamelContext>() {
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

    if (/*removed &&*/ camelContextRef != null) {
      camelContextStartupSubscription.unsubscribe();
    }
  }
}
