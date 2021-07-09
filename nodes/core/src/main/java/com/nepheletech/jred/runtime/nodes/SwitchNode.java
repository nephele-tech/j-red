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

import static com.nepheletech.jred.runtime.util.JRedUtil.getMessageProperty;

import java.util.Objects;

import org.apache.camel.Exchange;

import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.nepheletech.jred.runtime.flows.Flow;
import com.nepheletech.jred.runtime.util.JRedUtil;
import com.nepheletech.jton.JtonArray;
import com.nepheletech.jton.JtonElement;
import com.nepheletech.jton.JtonNull;
import com.nepheletech.jton.JtonObject;
import com.nepheletech.jton.JtonPrimitive;

public class SwitchNode extends AbstractNode {

  private final JtonArray rules;
  private final String property;
  private final String propertyType;
  private final boolean checkAll;

  private boolean valid = false;

  public SwitchNode(Flow flow, JtonObject config) {
    super(flow, config);

    this.rules = config.getAsJtonArray("rules", true);
    this.property = config.getAsString("property");
    this.propertyType = config.getAsString("propertyType", "msg");

    this.checkAll = config.getAsBoolean("checkall", true);
    this.valid = true;

    if ("jsonpath".equals(propertyType)) {
      this.valid = false;
      throw new UnsupportedOperationException("jsonpath is not supported yet");
    }

    for (JtonElement _rule : rules) {
      if (_rule.isJtonObject()) {
        final JtonObject rule = _rule.asJtonObject();

        if (!rule.has("vt") || !rule.get("vt").isJtonPrimitive()) {
          if (!Double.isNaN(rule.get("v").asDouble(Double.NaN))) {
            rule.set("vt", "num");
          } else {
            rule.set("vt", "str");
          }
        }
        final String vt = rule.getAsString("vt");
        if ("num".equals(vt)) {
          if (Double.isNaN(rule.getAsDouble("v", Double.NaN))) {
            rule.set("v", rule.getAsDouble("v"));
          } else if ("jsonpath".equals(vt)) {
            this.valid = false;
            throw new UnsupportedOperationException("jsonpath is not supported yet");
          }
        }
        if (rule.get("v2") != JtonNull.INSTANCE) {
          if (!rule.has("v2t") || !rule.isJtonPrimitive("v2t")) {
            if (!Double.isNaN(rule.getAsDouble("v2"))) {
              rule.set("v2t", "num");
            } else {
              rule.set("v2t", "str");
            }
          }
          final String v2t = rule.getAsString("v2t");
          if ("num".equals(v2t)) {
            if (Double.isNaN(rule.getAsDouble("v2"))) {
              rule.set("v2", rule.getAsDouble("v2"));
            } else if ("jsonpath".equals(vt)) {
              this.valid = false;
              throw new UnsupportedOperationException("jsonpath is not supported yet");
            }
          }
        }
      }
    }
  }

  @Override
  protected void onMessage(final Exchange exchange, final JtonObject msg) {
    if (!valid) {
      return;
    }

    JtonArray onward = new JtonArray();

    JtonElement prop;

    if ("jsonpath".equals(this.propertyType)) {
      throw new UnsupportedOperationException("jsonpath is not supported yet");
    } else {
      prop = JRedUtil.evaluateNodeProperty(property, propertyType, this, msg);
    }

    boolean elseFlag = true;

    for (JtonElement _rule : rules) {
      if (!_rule.isJtonObject()) {
        continue;
      }
      JtonObject rule = _rule.asJtonObject();
      logger.info("----------------------------------------------------------------{}", rule);

      JtonElement test = prop;

      JtonElement v1, v2;

      String vt = rule.getAsString("vt");
      if ("prev".equals(vt)) {
        v1 = JtonNull.INSTANCE;
      } else if ("jsonpath".equals(vt)) {
        throw new UnsupportedOperationException("jsonpath");
      } else {
        v1 = JRedUtil.evaluateNodeProperty(rule.getAsString("v", ""), vt, this, msg);
      }

      String v2t = rule.getAsString("v2t", null);
      if ("prev".equals(v2t)) {
        v2 = JtonNull.INSTANCE;
      } else if ("jsonpath".equals(v2t)) {
        throw new UnsupportedOperationException("jsonpath");
      } else {
        v2 = JRedUtil.evaluateNodeProperty(rule.getAsString("v2", ""), v2t, this, msg);
      }

      String t = rule.getAsString("t", null);
      if ("else".equals(t)) {
        test = new JtonPrimitive(elseFlag);
        elseFlag = true;
      }

      boolean result = false;

      if ("eq".equals(t)) {
        result = Objects.equals(test, v1);
      } else if ("neq".equals(t)) {
        result = !Objects.equals(test, v1);
      } else if ("lt".equals(t)) {
        double a = test.asDouble(Double.NaN);
        double v = v1.asDouble(Double.NaN);
        result = isOneNaN(a, v) ? false : a < v;
      } else if ("lte".equals(t)) {
        double a = test.asDouble(Double.NaN);
        double v = v1.asDouble(Double.NaN);
        result = isOneNaN(a, v) ? false : a <= v;
      } else if ("gt".equals(t)) {
        double a = test.asDouble(Double.NaN);
        double v = v1.asDouble(Double.NaN);
        result = isOneNaN(a, v) ? false : a > v;
      } else if ("gte".equals(t)) {
        double a = test.asDouble(Double.NaN);
        double v = v1.asDouble(Double.NaN);
        result = isOneNaN(a, v) ? false : a >= v;
      } else if ("hask".equals(t)) {
        if (test.isJtonObject()) {
          result = test.asJtonObject().has(v1.asString(""));
        }
      } else if ("btwn".equals(t)) { // TODO handle dates...
        double a = test.asDouble(Double.NaN);
        double v = v1.asDouble(Double.NaN);
        double _v2 = v2.asDouble(Double.NaN);
        result = isOneNaN(a, v, _v2) ? false : a >= v && a <= _v2;
      } else if ("cont".equals(t)) {
        result = test.asString("").contains(v1.asString(null));
      } else if ("regex".equals(t)) {
        result = test.asString("").matches(v1.asString(null));
      } else if ("true".equals(t)) {
        Boolean value = test.asBoolean(false);
        result = test != null ? value == Boolean.TRUE : false;
      } else if ("false".equals(t)) {
        Boolean value = test.asBoolean(false);
        result = test != null ? value == Boolean.FALSE : false;
      } else if ("null".equals(t)) {
        result = test.isJtonNull();
      } else if ("nnull".equals(t)) {
        result = !test.isJtonNull();
      } else if ("empty".equals(t)) {
        if (test.isJtonPrimitive()) {
          final JtonPrimitive _test = test.asJtonPrimitive();
          if (_test.isString()) {
            result = _test.asString().isEmpty();
          } else if (_test.isBuffer()) {
            final byte[] buffer = _test.getValue();
            result = buffer.length == 0;
          }
        } else if (test.isJtonArray()) {
          result = test.asJtonArray().isEmpty();
        } else if (test.isJtonObject()) {
          result = test.asJtonObject().isEmpty();
        }
      } else if ("nempty".equals(t)) {
        if (test.isJtonPrimitive()) {
          final JtonPrimitive _test = test.asJtonPrimitive();
          if (_test.isString()) {
            result = !_test.asString().isEmpty();
          } else if (_test.isBuffer()) {
            final byte[] buffer = _test.getValue();
            result = buffer.length > 0;
          }
        } else if (test.isJtonArray()) {
          result = !test.asJtonArray().isEmpty();
        } else if (test.isJtonObject()) {
          result = !test.asJtonObject().isEmpty();
        }
      } else if ("istype".equals(t)) {
        final String b = v1.asString(null);
        if ("array".equals(b)) {
          result = test.isJtonArray();
        } else if ("object".equals(b)) {
          result = test.isJtonObject();
        } else if ("undefined".equals(b) || "null".equals(b)) {
          result = test.isJtonNull();
        } else if (test.isJtonPrimitive()) {
          final JtonPrimitive _test = test.asJtonPrimitive();
          if ("string".equals(b)) {
            result = _test.isString();
          } else if ("number".equals(b)) {
            result = _test.isNumber();
          } else if ("boolean".equals(b)) {
            result = _test.isBoolean();
          } else if ("buffer".equals(b)) {
            result = _test.isBuffer();
          } else if ("json".equals(b)) {
            try {
              JsonParser.parseString(_test.asString());
              result = true;
            } catch (JsonParseException e) {
              result = false;
            }
          }
        }
      } else if ("head".equals(t)) {
        final int count = v1.asInt(0);
        final JtonObject parts = msg.getAsJtonObject("parts", false);
        if (parts != null) {
          result = parts.getAsInt("index", 0) < count;
        }
      } else if ("index".equals(t)) {
        final int min = v1.asInt(0);
        final int max = v2.asInt(0);
        final int index = getMessageProperty(msg, "msg.parts.index").asInt(0);
        result = ((min <= index) && (index <= max));
      } else if ("tail".equals(t)) {
        final int count = v1.asInt(0);
        final JtonObject parts = msg.getAsJtonObject("parts", false);
        if (parts != null) {
          result = parts.getAsInt("count", 0) - count <= parts.getAsInt("index", 0);
        }
      } else if ("jsonata_exp".equals(t)) {
        throw new UnsupportedOperationException("jsonata_exp");
      } else if ("else".equals(t)) {
        result = test.asJtonPrimitive().asBoolean();
      }

      if (result) {
        onward.push(msg);
        elseFlag = false;
        if (!checkAll) {
          break;
        }
      } else {
        onward.push(JtonNull.INSTANCE);
      }
    }

    send(exchange, onward);
  }

  private boolean isOneNaN(double a1, double a2) {
    return Double.isNaN(a1) || Double.isNaN(a2);
  }

  private boolean isOneNaN(double a1, double a2, double a3) {
    return Double.isNaN(a1) || Double.isNaN(a2) || Double.isNaN(a3);
  }

}
