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

// ftp://[username@]hostname[:port]/directoryname[?options] 
// sftp://[username@]hostname[:port]/directoryname[?options] 
// ftps://[username@]hostname[:port]/directoryname[?options]
public class FtpNode extends AbstractCamelNode implements HasCredentials {
  private final boolean secure;

  private final String host;
  private final String port;
  private final String path;

  private String username;
  private String password;

  private String privateKey = "-----BEGIN RSA PRIVATE KEY-----\n" +
  // TODO
      "-----END RSA PRIVATE KEY-----";

  public FtpNode(Flow flow, JtonObject config) {
    super(flow, config);

    this.secure = config.getAsBoolean("secure", false);

    this.host = config.getAsString("host");
    this.port = config.getAsString("port", this.secure ? "22" : "21");
    this.path = config.getAsString("path", "");
  }

  public void setCredentials(JtonObject credentials) {
    if (credentials != null) {
      this.username = credentials.getAsString("username", null);
      this.password = credentials.getAsString("password", null);
    } else {
      this.username = null;
      this.password = null;
    }
  }

  @Override
  protected void addRoutes(CamelContext camelContext) throws Exception {
    final String ftpUrl = String.format("%sftp://%s@%s:%s%s?password=RAW(%s)&privateKey=%s&autoCreate=true&binary=true",
        (secure ? "s" : ""), username, host, port, path, password, "#privateKey");

    logger.info("====================={}", ftpUrl);

    camelContext.getRegistry()
        .bind("privateKey", privateKey.getBytes());

    camelContext.addRoutes(new RouteBuilder() {
      // onException(Exception.class)

      @Override
      public void configure() throws Exception {
        from("direct:" + getId())
            .to("log:DEBUG?showBody=true&showHeaders=true")
            .to(ftpUrl);
      }
    });
  }

  @Override
  protected void onMessage(JtonObject msg) {
    logger.trace(">>> onMessage: msg={}", msg);

    final ProducerTemplate template = getCamelContext().createProducerTemplate();
    template.send("direct:" + getId(), new Processor() {
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
