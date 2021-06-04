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

import static com.nepheletech.jton.JtonUtil.getProperty;
import static java.lang.String.format;
import static java.util.Arrays.asList;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.camel.Exchange;
import java.util.Optional;
import java.util.Set;

import com.nepheletech.jred.runtime.flows.Flow;
import com.nepheletech.jton.JtonElement;
import com.nepheletech.jton.JtonObject;
import com.nepheletech.jton.JtonPrimitive;

public class KafkaOutNode extends AbstractNode {

  private final String brokers;
  private final String topic;

  private final Set<String> KAFKA_KEYS = new HashSet<>(asList(new String[] {
      "KEY",
      "OVERRIDE_TOPIC",
      "OVERRIDE_TIMESTAMP",
      "PARTITION_KEY"
  }));

  public KafkaOutNode(Flow flow, JtonObject config) {
    super(flow, config);

    final String brokerRef = config.getAsString("broker");
    this.brokers = (brokerRef != null) ? ((KafkaBrokerNode) flow.getNode(brokerRef)).getHosts() : null;

    this.topic = config.getAsString("topic");
  }

  @Override
  public void configure() throws Exception {
    super.configure();

    fromF("direct:%s#kafka", getId())
        .toF("log:%s?level=TRACE&showBody=false&showHeaders=true", logger.getName())
        .toF("kafka:%s?brokers=%s", topic, brokers);
  }

  @Override
  protected String getAdditionalFlow() {
    return format("direct:%s#kafka", getId());
  }

  @Override
  protected void onMessage(Exchange exchange, JtonObject msg) {
    logger.trace(">>> onMessage: exchange={}, msg={}", exchange, msg);

    final Map<String, Object> kafkaHeaders = new HashMap<>();
    final Optional<JtonObject> headers = getProperty(msg, "headers.kafka").asOptJtonObject();
    if (headers.isPresent()) {
      for (Entry<String, JtonElement> entry : headers.get().entrySet()) {
        final String key = entry.getKey().toUpperCase();
        if (KAFKA_KEYS.contains(key)) {
          final Optional<JtonPrimitive> value = entry.getValue().asOptJtonPrimitive();
          if (value.isPresent()) {
            kafkaHeaders.put("kafka." + key, value.get().getValue());
          }
        }
      }
    }

    exchange.getIn().getHeaders().putAll(kafkaHeaders);
    exchange.getIn().setBody(msg.get("payload"));
  }
}
