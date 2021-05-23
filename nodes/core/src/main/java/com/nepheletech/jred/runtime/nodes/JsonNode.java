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

import com.google.gson.JsonSyntaxException;
import com.nepheletech.jred.runtime.flows.Flow;
import com.nepheletech.jred.runtime.util.JRedUtil;
import com.nepheletech.jton.JtonElement;
import com.nepheletech.jton.JtonObject;
import com.nepheletech.jton.JtonParser;
import com.nepheletech.jton.JtonPrimitive;

public class JsonNode extends AbstractNode {

  private final String property;
  private final String action;
  private final boolean pretty;

  public JsonNode(Flow flow, JtonObject config) {
    super(flow, config);

    this.property = config.getAsString("property", "payload");
    this.action = config.getAsString("action", ""); // "", "str", "obj"
    this.pretty = config.getAsBoolean("pretty", false);
  }

  @Override
  protected JtonElement onMessage(JtonObject msg) {
    logger.trace(">>> onMessage: msg={}", msg);

    final JtonElement value = JRedUtil.getMessageProperty(msg, property);
    if (value != null) {
      if (value.isJtonPrimitive() && value.asJtonPrimitive().isString()) {
        if ("".equals(action) || "obj".equals(action)) {
          try {
            JRedUtil.setMessageProperty(msg, property, JtonParser.parse(value.asString()), false);
          } catch (JsonSyntaxException e) {
            error(e, msg);
          }
        }
        return(msg);
      } else {
        if ("".equals(action) || "str".equals(action)) {
          // TODO Buffer
          JRedUtil.setMessageProperty(msg, property,
              new JtonPrimitive(pretty ? value.toString("   ") : value.toString()), false);
        }
        return(msg);
      }
    } else {
      return(msg);
    }
  }

}
