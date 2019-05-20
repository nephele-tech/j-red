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
