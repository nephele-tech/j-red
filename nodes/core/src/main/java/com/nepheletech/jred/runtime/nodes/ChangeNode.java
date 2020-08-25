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

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import com.google.common.base.Objects;
import com.google.gson.JsonSyntaxException;
import com.jayway.jsonpath.JsonPath;
import com.nepheletech.jred.runtime.flows.Flow;
import com.nepheletech.jred.runtime.util.JRedUtil;
import com.nepheletech.jton.JtonArray;
import com.nepheletech.jton.JtonElement;
import com.nepheletech.jton.JtonNull;
import com.nepheletech.jton.JtonObject;
import com.nepheletech.jton.JtonParser;
import com.nepheletech.jton.JtonPrimitive;
import com.nepheletech.jton.JtonUtil;

public class ChangeNode extends AbstractNode {
  private final JtonArray rules;
  private boolean valid = true;

  public ChangeNode(Flow flow, JtonObject config) {
    super(flow, config);

    this.rules = config.getAsJtonArray("rules", true);

    for (int i = 0, iMax = rules.size(); i < iMax; i++) {
      final JtonObject rule = rules.getAsJtonObject(i);
      // Migrate to type-aware rules
      if (!rule.has("pt")) {
        rule.set("pt", "msg");
      }
      final String t = rule.getAsString("t");
      if ("change".equals(t) && rule.has("re")) {
        rule.set("fromt", "re");
        rule.remove("re");
      }
      if ("set".equals(t) && !rule.has("tot")) {
        final String to = rule.getAsString("to");
        if (to.indexOf("msg.") == 0 && !rule.has("tot")) {
          rule.set("to", to.substring(4));
          rule.set("tot", "msg");
        }
      }
      if (!rule.has("tot")) {
        rule.set("tot", "str");
      }
      if (!rule.has("fromt")) {
        rule.set("fromt", "str");
      }

      final String fromt = rule.getAsString("fromt");
      if ("change".equals(t)
          && !"msg".equals(fromt) && !"flow".equals(fromt) && !"global".equals(fromt)) {
        final String fromRE = rule.get("from").asString();
        if ("re".equals(fromt)) {
          try {
            rule.set("fromRE", Pattern.compile(fromRE), true);
          } catch (PatternSyntaxException e) {
            valid = false;
            throw new IllegalArgumentException("Invalid 'from' property", e);
          }
        }
      }

      final String to = rule.getAsString("to", null);
      final String tot = rule.getAsString("tot", null);
      if ("num".equals(tot)) {
        rule.set("to", rule.getAsNumber("to"));
      } else if ("json".equals(tot) || "bin".equals(tot)) {
        try {
          // check this is parsable JSON
          JtonParser.parse(to);
        } catch (JsonSyntaxException e) {
          valid = false;
          throw new IllegalArgumentException("Invalid 'to' JSON property", e);
        }
      } else if ("bool".equals(tot)) {
        rule.set("to", rule.getAsBoolean("to"));
      } else if ("jsonpath".equals(tot)) {
        try {
          // TODO optimize JSONPath evaluation
          rule.set("toJSONPath", JRedUtil.prepareJsonPathExpression(to), true);
        } catch (RuntimeException e) {
          valid = false;
          throw new IllegalArgumentException("Invalid JSONPath expression", e);
        }
      } else if ("env".equals(tot)) {
        // replace 'to' with environment parameter value and...
        rule.set("to", JRedUtil.evaluateNodeProperty(to, "env", this, null));
        // set 'tot' to 'str'
        rule.set("tot", "str");
      }
    }
  }

  @Override
  protected void onMessage(JtonObject msg) {
    logger.trace("onMessage: msg.keySet={}", msg.keySet());

    if (valid) {
      send(applyRules(msg));
    }
  }

  private JtonObject applyRules(JtonObject msg) {
    rules.forEach(elem -> {
      final JtonObject r = elem.asJtonObject();
      final String t = r.getAsString("t");

      if ("move".equals(t)) {
        final String to = r.getAsString("to");
        final String tot = r.getAsString("tot");
        final String p = r.getAsString("p");
        final String pt = r.getAsString("pt");
        if (!tot.equals(pt) || (p.indexOf(to) != -1)) {
          applyRule(msg, new JtonObject()
              .set("t", "set")
              .set("p", to)
              .set("pt", tot)
              .set("to", p)
              .set("tot", pt));
          applyRule(msg, new JtonObject()
              .set("t", "delete")
              .set("p", p)
              .set("pt", pt));
        } else {
          applyRule(msg, new JtonObject()
              .set("t", "set")
              .set("p", "_temp_move")
              .set("pt", tot)
              .set("to", p)
              .set("tot", pt));
          applyRule(msg, new JtonObject()
              .set("t", "delete")
              .set("p", p)
              .set("pt", pt));
          applyRule(msg, new JtonObject()
              .set("t", "set")
              .set("p", to)
              .set("pt", tot)
              .set("to", "_temp_move")
              .set("tot", pt));
          applyRule(msg, new JtonObject()
              .set("t", "delete")
              .set("p", "_temp_move")
              .set("pt", pt));
        }
      } else {
        applyRule(msg, r);
      }
    });

    return msg;
  }

  private void applyRule(JtonObject msg, JtonObject rule) {
    final String property = rule.getAsString("p");

    final String pt = rule.getAsString("pt");
    if ("msg".equals(pt)) {
      final String t = rule.getAsString("t");
      if ("delete".equals(t)) {
        JRedUtil.deleteMessageProperty(msg, property);
      } else if ("set".equals(t)) {
        JRedUtil.setMessageProperty(msg, property, getToValue(msg, rule), true);
      } else if ("change".equals(t)) {
        final JtonElement fromValue = getFromValue(msg, rule);
        final String fromValueType = getFromValueType(msg, rule);
        final JtonElement fromRE = rule.get("fromRE");
        final JtonElement toValue = getToValue(msg, rule);

        final JtonElement current = JRedUtil.getMessageProperty(msg, property);
        if (current.isJtonPrimitive()) {
          final JtonPrimitive currPrimitive = current.asJtonPrimitive();
          if (currPrimitive.isString()) {
            if (("num".equals(fromValueType) || "bool".equals(fromValueType) || "str".equals(fromValueType)
                && Objects.equal(currPrimitive.asString(), fromValue))) {
              // str representation of exact from number/boolean
              // only replace if they match exactly
              JRedUtil.setMessageProperty(msg, property, toValue, false);
            } else {
              final String value = (!fromRE.isJtonNull())
                  ? ((Pattern) fromRE.asJtonPrimitive().getValue())
                      .matcher(currPrimitive.asString()).replaceAll(toValue.asString())
                  : currPrimitive.asString().replace(fromValue.asString(), toValue.asString());
              JRedUtil.setMessageProperty(msg, property, new JtonPrimitive(value), false);
            }
          } else if (currPrimitive.isNumber() && "num".equals(fromValueType)) {
            if (currPrimitive.equals(fromValue)) {
              JRedUtil.setMessageProperty(msg, property, toValue, false);
            }
          } else if (currPrimitive.isBoolean() && "bool".equals(fromValueType)) {
            if (currPrimitive.equals(fromValue)) {
              JRedUtil.setMessageProperty(msg, property, toValue, false);
            }
          }
        }
      }
    } else if ("flow".equals(pt) || "global".equals(pt)) {
      final String t = rule.getAsString("t");
      if ("delete".equals(t)) {
        JRedUtil.deleteNodeProperty(property, pt, this, msg);
      } else if ("set".equals(t)) {
        JRedUtil.setNodeProperty(property, pt, this, msg, getToValue(msg, rule));
      } else if ("change".equals(pt)) {
        final JtonElement fromValue = getFromValue(msg, rule);
        final String fromValueType = getFromValueType(msg, rule);
        final JtonElement fromRE = rule.get("fromRE");
        final JtonElement toValue = getToValue(msg, rule);

        final JtonObject target = getContext(pt);

        final JtonElement current = JRedUtil.getMessageProperty(msg, property);
        if (current.isJtonPrimitive()) {
          final JtonPrimitive currPrimitive = current.asJtonPrimitive();
          if (currPrimitive.isString()) {
            if (("num".equals(fromValueType) || "bool".equals(fromValueType) || "str".equals(fromValueType)
                && Objects.equal(currPrimitive.asString(), fromValue))) {
              // str representation of exact from number/boolean
              // only replace if they match exactly
              JtonUtil.setProperty(target, property, toValue, true);
            } else {
              final String value = (!fromRE.isJtonNull())
                  ? ((Pattern) fromRE.asJtonPrimitive().getValue())
                      .matcher(currPrimitive.asString()).replaceAll(toValue.asString())
                  : currPrimitive.asString().replace(fromValue.asString(), toValue.asString());
              JRedUtil.setNodeProperty(property, pt, this, msg, new JtonPrimitive(value));
            }
          } else if (currPrimitive.isNumber() && "num".equals(fromValueType)) {
            if (currPrimitive.equals(fromValue)) {
              JtonUtil.setProperty(target, property, toValue, true);
            }
          } else if (currPrimitive.isBoolean() && "bool".equals(fromValueType)) {
            if (currPrimitive.equals(fromValue)) {
              JtonUtil.setProperty(target, property, toValue, true);
            }
          }
        }
      }
    }
  }

  private JtonElement getToValue(JtonObject msg, JtonObject rule) {
    final String to = rule.getAsString("to");
    final String tot = rule.getAsString("tot");
    if ("jsonpath".equals(tot)) {
      final JsonPath jsonPath = (JsonPath) rule.getAsJtonPrimitive("toJSONPath").getValue();
      return JRedUtil.evaluateJsonPathExpression(msg, jsonPath);
    } else {
      return JRedUtil.evaluateNodeProperty(to, tot, this, msg);
    }
  }

  private JtonElement getFromValue(JtonObject msg, JtonObject rule) {
    final String t = rule.getAsString("t");
    if ("change".equals(t)) {
      final String fromt = rule.getAsString("fromt");
      if ("msg".equals(fromt) || "flow".equals(fromt) || "global".equals(fromt)) {
        if ("msg".equals(fromt)) {
          return JRedUtil.getMessageProperty(msg, rule.getAsString("from"));
        } else if ("flow".equals(fromt) || "global".equals(fromt)) {
          return JRedUtil.evaluateNodeProperty(rule.getAsString("from"), fromt, this, msg);
        }
      } else {
        return rule.get("from");
      }
    }
    return JtonNull.INSTANCE;
  }

  private String getFromValueType(JtonObject msg, JtonObject rule) {
    final String t = rule.getAsString("t");
    if ("change".equals(t)) {
      final String fromt = rule.getAsString("fromt");
      if ("msg".equals(fromt) || "flow".equals(fromt) || "global".equals(fromt)) {
        throw new UnsupportedOperationException(); // TODO
      } else {
        return fromt;
      }
    }
    return null;
  }
}
