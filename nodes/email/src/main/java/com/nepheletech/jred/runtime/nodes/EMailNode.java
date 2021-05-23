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
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;

import com.nepheletech.jred.runtime.flows.Flow;
import com.nepheletech.jton.JtonElement;
import com.nepheletech.jton.JtonObject;

public class EMailNode extends AbstractNode implements HasCredentials {
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
  protected JtonElement onMessage(JtonObject msg) {
    // TODO Auto-generated method stub
    
    return null;
  }
}
