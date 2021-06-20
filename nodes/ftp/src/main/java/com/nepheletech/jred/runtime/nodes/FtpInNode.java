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
import org.apache.commons.codec.digest.DigestUtils;

import com.nepheletech.jred.runtime.flows.Flow;
import com.nepheletech.jton.JtonObject;
import com.nepheletech.jton.JtonParser;

public class FtpInNode extends AbstractNode implements Processor {

  private final String server;
  private final String dirname;
  private final boolean binary;
  private final boolean recursive;
  private final int repeat;
  private final String ret;

  public FtpInNode(Flow flow, JtonObject config) {
    super(flow, config);

    this.server = config.getAsString("server", null);
    this.dirname = config.getAsString("dirname", null);
    this.binary = config.getAsBoolean("binary", false);
    this.recursive = config.getAsBoolean("recursive", false);
    this.repeat = config.getAsInt("repeat", 1);
    this.ret = config.getAsString("ret", "txt");
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
        
        final long delay = 1000L * (long) repeat;

        fromF("%sftp://%s%s:%d/%s?readLock=changed&noop=true"
            + "&binary=%b&recursive=%b&passiveMode=%b&delay=%d%s",
            ftpServerNode.isSecure() ? "s" : "",
            usernamePart, ftpServerNode.getHost(), ftpServerNode.getPort(), dirname,
            binary, recursive, ftpServerNode.isPassiveMode(), delay, passwordPart)
                .toF("log:%s?level=DEBUG&showBody=false&showHeaders=true", logger.getName())
                .process(FtpInNode.this)
                .toF("direct:%s", getId());
      }
    } else {
      logger.warn("-------------------------------------------!!! NO SERVER");
    }
  }

  @Override
  public void process(Exchange exchange) throws Exception {
    logger.trace(">>> process: exchange={}", exchange);

    final Message message = exchange.getIn();
    final byte[] bs = message.getBody(byte[].class);
    final String md5 = DigestUtils.md5Hex(bs);

    final JtonObject msg = new JtonObject()
        .set("headers", new JtonObject()
            .set("ftp", new JtonObject()
                .set("MD5Sum", md5) // MD5 Sum
                .set(Exchange.FILE_PATH,
                    message.getHeader(Exchange.FILE_PATH, String.class))
                .set(Exchange.FILE_NAME,
                    message.getHeader(Exchange.FILE_NAME, String.class))));

    if ("bin".equals(ret)) {
      msg.set("payload", bs);
    } else {
      final String payload = message.getBody(String.class);
      if ("obj".equals(ret)) {
        try {
          msg.set("payload", JtonParser.parse(payload));
        } catch (Exception e) {
          logger.warn(e.getMessage());
          msg.set("payload", payload);
        }
      } else {
        msg.set("payload", payload);
      }
    }

    message.setBody(msg);
  }

  @Override
  protected void onMessage(final Exchange exchange, final JtonObject msg) {
    send(exchange, msg);
  }
}
