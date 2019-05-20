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
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;

import com.nepheletech.jred.runtime.flows.Flow;
import com.nepheletech.jton.JtonObject;
import com.nepheletech.jton.JsonParser;

public class KafkaInNode extends AbstractCamelNode implements Processor {

  private final String broker;
  private final String topic;
  private final String groupId;

  public KafkaInNode(Flow flow, JtonObject config) {
    super(flow, config);

    this.broker = config.getAsString("broker");
    this.topic = config.getAsString("topic");
    this.groupId = config.getAsString("groupId", getId());
  }

  protected void addRoutes(CamelContext camelContext) throws Exception {
    logger.trace(">>> addRoutes: {}", getId());

    camelContext.addRoutes(new RouteBuilder() {
      // onException(Exception.class)

      @Override
      public void configure() throws Exception {
        from("kafka:" + topic + "?brokers=" + broker + "&groupId=" + groupId)
            .to("log:DEBUG?showBody=true&showHeaders=true")
            .process(KafkaInNode.this);
      }
    });
  }

  @Override
  public void process(Exchange exchange) throws Exception {
    logger.trace(">>> process: exchange={}", exchange);

    final Message message = exchange.getIn();
    receive(new JtonObject()
        .set("_uid", exchange.getExchangeId())
        .set("payload", JsonParser.parse(message.getBody(String.class))));
  }

  @Override
  protected void onMessage(JtonObject msg) {
    logger.trace(">>> onMessage: msg={}", msg);

    send(msg);
  }
}
