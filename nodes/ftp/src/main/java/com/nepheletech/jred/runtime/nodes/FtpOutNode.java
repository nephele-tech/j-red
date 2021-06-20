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

import static java.lang.String.format;

import java.util.Map.Entry;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;

import com.nepheletech.jred.runtime.flows.Flow;
import com.nepheletech.jton.JtonElement;
import com.nepheletech.jton.JtonObject;
import com.nepheletech.jton.JtonPrimitive;

// ftp://[username@]hostname[:port]/directoryname[?options] 
// sftp://[username@]hostname[:port]/directoryname[?options] 
// ftps://[username@]hostname[:port]/directoryname[?options]
public class FtpOutNode extends AbstractNode {

  private final String server;
  private final String dirname;
  private final boolean binary;

  private String privateKey = "-----BEGIN RSA PRIVATE KEY-----\n" +
  // TODO
      "-----END RSA PRIVATE KEY-----";

  public FtpOutNode(Flow flow, JtonObject config) {
    super(flow, config);

    this.server = config.getAsString("server", null);
    this.dirname = config.getAsString("dirname", null);
    this.binary = config.getAsBoolean("binary", false);
  }

  @Override
  public void configure() throws Exception {
    super.configure();

    if (server != null) {
      final FtpServerNode ftpServerNode = flow.getNode(server);
      logger.info("-----------------------------{}--------------!!! {}", server, ftpServerNode);
      if (ftpServerNode != null) {
        final String username = ftpServerNode.getUsername();
        final String password = ftpServerNode.getPassword();

        final String usernamePart = username != null ? username + "@" : "";
        final String passwordPart = password != null ? "&password=RAW(" + password + ")" : "";

        final String ftpUrl = format("%sftp://%s%s:%d/%s?autoCreate=true"
            + "&binary=%b&passiveMode=%b%s",
            ftpServerNode.isSecure() ? "s" : "",
            usernamePart, ftpServerNode.getHost(), ftpServerNode.getPort(), dirname,
            binary, ftpServerNode.isPassiveMode(), passwordPart);

        logger.info("====================={}", ftpUrl);

        camelContext.getRegistry()
            .bind("privateKey", privateKey.getBytes());

        fromF("direct:%s#ftp", getId())
            .toF("log:%s?level=TRACE&showBody=false&showHeaders=true", logger.getName())
            .to(ftpUrl)
            .log(LoggingLevel.INFO, logger,
                format("Upload complete: %s", header(Exchange.FILE_NAME)));
      }
    } else {
      logger.warn("-------------------------------------------!!! NO SERVER");
    }
  }

  @Override
  protected String getAdditionalRoute() {
    return format("direct:%s#ftp", getId());
  }

  @Override
  protected void onMessage(final Exchange exchange, final JtonObject msg) {
    logger.trace(">>> onMessage: exchange={}, msg={}", exchange, msg);
    /*
     * final ProducerTemplate template = camelContext.createProducerTemplate();
     * template.send("direct:" + getId() + "#sftp", new Processor() { public void
     * process(Exchange exchange) throws Exception { final Message in =
     * exchange.getIn();
     * 
     * in.setBody(msg.get("payload").toString(), String.class);
     * 
     * final JtonObject headers = msg.getAsJtonObject("headers", false); if (headers
     * != null) { for (Entry<String, JtonElement> entry : headers.entrySet()) {
     * final String key = entry.getKey(); final JtonElement value =
     * entry.getValue(); if (value.isJtonPrimitive()) { final JtonPrimitive _value =
     * value.asJtonPrimitive(); if (_value.isJtonTransient()) {
     * in.getHeaders().put(key, _value.getValue()); } else {
     * in.getHeaders().put(key, _value); } } } } } });
     */

    exchange.getIn().setBody(msg.get("payload").asJtonPrimitive().getValue());
  }
}
