/*
 * This file is part of J-RED Runtime project.
 *
 * J-RED Runtime is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * J-RED Runtime is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this J-RED Runtime; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package com.nepheletech.jred.runtime.nodes;


import org.apache.camel.Exchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nepheletech.jred.runtime.flows.Flow;
import com.nepheletech.jton.JtonObject;

public final class StatusNode extends AbstractNode {
  private static final Logger log = LoggerFactory.getLogger(StatusNode.class);

  public StatusNode(Flow flow, JtonObject config) {
    super(flow, config);
  }

  @Override
  protected void onMessage(final Exchange exchange, final JtonObject msg) {
    log.trace(">>> onMessage: msg={}" + msg);
    
    send(exchange, msg);
  }
}
