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

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.component.kafka.KafkaConstants;

import com.nepheletech.jred.runtime.flows.Flow;
import com.nepheletech.jton.JtonObject;
import com.nepheletech.jton.JtonParser;

public class KafkaInNode extends AbstractNode implements Processor {

  private final String brokers;
  private final String topic;
  private final String groupId;

  public KafkaInNode(Flow flow, JtonObject config) {
    super(flow, config);

    final String brokerRef = config.getAsString("broker");
    this.brokers = (brokerRef != null) ? ((KafkaBrokerNode) flow.getNode(brokerRef)).getHosts() : null;

    this.topic = config.getAsString("topic");
    this.groupId = config.getAsString("groupId", getId());
  }

  @Override
  public void configure() throws Exception {
    super.configure();

    fromF("kafka:%s?brokers=%s&groupId=%s", topic, brokers, groupId)
        .toF("log:%s?level=DEBUG&showBody=true&showHeaders=true", logger.getName())
        .process(KafkaInNode.this);
  }

  @Override
  public void process(Exchange exchange) throws Exception {
    logger.trace(">>> process: exchange={}", exchange);

    final Message message = exchange.getIn();

    JtonObject msg = new JtonObject()
        .set("_uid", exchange.getExchangeId())
        .set("payload", JtonParser.parse(message.getBody(String.class)))
        .set("headers", new JtonObject()
            .set("kafka", new JtonObject()
                .set("TOPIC",
                    message.getHeader(KafkaConstants.TOPIC, String.class))
                .set("PARTITION",
                    message.getHeader(KafkaConstants.PARTITION, Integer.class))
                .set("TIMESTAMP",
                    message.getHeader(KafkaConstants.TIMESTAMP, Long.class))
                .set("OFFSET",
                    message.getHeader(KafkaConstants.OFFSET, Long.class))
                .set("LAST_RECORD_BEFORE_COMMIT",
                    message.getHeader(KafkaConstants.LAST_RECORD_BEFORE_COMMIT, Boolean.class))
                .set("LAST_POLL_RECORD",
                    message.getHeader(KafkaConstants.LAST_POLL_RECORD, Boolean.class))
                .set("MANUAL_COMMIT",
                    message.getHeader(KafkaConstants.MANUAL_COMMIT, Boolean.class))));

    receive(msg);
  }

  @Override
  protected void onMessage(JtonObject msg) {
    logger.trace(">>> onMessage: msg={}", msg);

    send(msg);
  }
}
