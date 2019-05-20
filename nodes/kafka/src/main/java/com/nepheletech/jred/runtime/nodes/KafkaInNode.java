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
