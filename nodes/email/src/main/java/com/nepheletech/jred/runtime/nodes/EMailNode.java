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
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;

import com.nepheletech.jred.runtime.flows.Flow;
import com.nepheletech.jton.JtonObject;

public class EMailNode extends AbstractCamelNode implements HasCredentials {
  private final String server;
  private final String port;
  private final boolean secure;
  private final boolean tls;
  private final String to;

  private String userid;
  private String password;

  public EMailNode(Flow flow, JtonObject config) {
    super(flow, config);
    this.server = config.getAsString("server");
    this.port = config.getAsString("port");
    this.secure = config.getAsBoolean("secure", true);
    this.tls = config.getAsBoolean("tls", true);
    this.to = config.getAsString("to");
  }

  @Override
  public void setCredentials(JtonObject credentials) {
    if (credentials != null) {
      this.userid = credentials.getAsString("userid", null);
      this.password = credentials.getAsString("password", null);
    } else {
      this.userid = null;
      this.password = null;
    }
  }

  @Override
  protected void addRoutes(CamelContext camelContext) throws Exception {

    camelContext.addRoutes(new RouteBuilder() {
      // onException(Exception.class)

      @Override
      public void configure() throws Exception {
        from("direct:" + getId())
            .to(String.format("log:DEBUG?showBody=false&showHeaders=%b", logger.isDebugEnabled()));
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
      }
    });
  }

}
