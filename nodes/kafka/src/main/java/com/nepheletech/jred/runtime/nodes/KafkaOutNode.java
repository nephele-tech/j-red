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

import java.util.Map.Entry;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;

import com.nepheletech.jred.runtime.flows.Flow;
import com.nepheletech.jton.JtonElement;
import com.nepheletech.jton.JtonObject;
import com.nepheletech.jton.JtonPrimitive;

public class KafkaOutNode extends AbstractCamelNode {

  private final String broker;
  private final String topic;

  public KafkaOutNode(Flow flow, JtonObject config) {
    super(flow, config);

    this.broker = config.getAsString("broker");
    this.topic = config.getAsString("topic");
  }

  @Override
  protected void addRoutes(CamelContext camelContext) throws Exception {
    logger.trace("----------------------------------------------------------------------->>> addRoutes {}", getId());

    camelContext.addRoutes(new RouteBuilder() {
      // onException(Exception.class)

      @Override
      public void configure() throws Exception {
        from("direct:" + getId())
            .to("log:DEBUG?showBody=true&showHeaders=true")
            .to("kafka:" + topic + "?brokers=" + broker);
      }
    });
  }

  @Override
  protected void onMessage(JtonObject msg) {
    logger.trace(">>> onMessage: msg={}", msg);

    final ProducerTemplate template = getCamelContext().createProducerTemplate();
    template.send("direct:" + getId(), new Processor() {
      @Override
      public void process(Exchange exchange) throws Exception {
        final Message in = exchange.getIn();

        in.setBody(msg.get("payload").toString(), String.class);

        final JtonObject headers = msg.getAsJtonObject("headers", false);
        if (headers != null) {
          for (Entry<String, JtonElement> entry : headers.entrySet()) {
            final String key = entry.getKey();
            final JtonElement value = entry.getValue();
            if (value.isJtonPrimitive()) {
              final JtonPrimitive _value = value.asJtonPrimitive();
              if (_value.isJtonTransient()) {
                in.getHeaders().put(key, _value.getValue());
              } else {
                in.getHeaders().put(key, _value);
              }
            }
          }
        }
      }
    });
  }
}
