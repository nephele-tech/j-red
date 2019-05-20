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

import com.nepheletech.jred.runtime.flows.Flow;
import com.nepheletech.jred.runtime.util.JRedUtil;
import com.nepheletech.jton.JsonParser;
import com.nepheletech.jton.JsonSyntaxException;
import com.nepheletech.jton.JtonElement;
import com.nepheletech.jton.JtonObject;
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
  protected void onMessage(JtonObject msg) {
    logger.trace(">>> onMessage: msg={}", msg);

    final JtonElement value = JRedUtil.getMessageProperty(msg, property);
    if (value != null) {
      if (value.isJtonPrimitive() && value.asJtonPrimitive().isString()) {
        if ("".equals(action) || "obj".equals(action)) {
          try {
            JRedUtil.setMessageproperty(msg, property, JsonParser.parse(value.asString()), false);
          } catch (JsonSyntaxException e) {
            error(e, msg);
          }
        }
        send(msg);
      } else {
        if ("".equals(action) || "str".equals(action)) {
          // TODO Buffer
          JRedUtil.setMessageproperty(msg, property,
              JtonPrimitive.create(pretty ? value.toString("   ") : value.toString()), false);
        }
        send(msg);
      }
    } else {
      send(msg);
    }
  }

}
