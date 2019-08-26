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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nepheletech.jred.runtime.flows.Flow;
import com.nepheletech.jred.runtime.util.JRedUtil;
import com.nepheletech.jton.JsonParser;
import com.nepheletech.jton.JsonSyntaxException;
import com.nepheletech.jton.JtonArray;
import com.nepheletech.jton.JtonElement;
import com.nepheletech.jton.JtonObject;
import com.nepheletech.jton.JtonPrimitive;
import com.nepheletech.jton.JtonUtil;

public class ChangeNode extends AbstractNode {
  private static final Logger logger = LoggerFactory.getLogger(ChangeNode.class);

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
        if ("re".equals(fromt)) {
          try {
            final String fromRE = rule.get("from").asString();
            rule.set("fromRE", Pattern.compile(fromRE), true);
          } catch (PatternSyntaxException e) {
            valid = false;
            throw new IllegalArgumentException("change.errors.invalid-fome", e); // FIXME
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
          JsonParser.parse(to);
        } catch (JsonSyntaxException e) {
          valid = false;
          throw new IllegalArgumentException("change.errors.invalid-json", e); // FIXME
        }
      } else if ("bool".equals(tot)) {
        rule.set("to", rule.getAsBoolean("to"));
      } else if ("jsonpath".equals(tot)) {
        try {
          rule.set("to", JRedUtil.prepareJsonPathExpression(to), true);
        } catch (RuntimeException e) {
          valid = false;
          throw new IllegalArgumentException("change.errors.invalid-expr", e); // FIXME
        }
      } else if ("env".equals(tot)) {
        rule.set("to", JRedUtil.evaluateNodeProperty(to, "env", this, null));
      }
    }
  }

  @Override
  protected void onMessage(JtonObject msg) {
    logger.trace("onMessage: msg.keySet={}", msg.keySet());

    if (!valid) { return; }

    // applyRules

    for (JtonElement _rule : rules) {
      final JtonObject r = _rule.asJtonObject();
      final String t = r.getAsString("t");
      if ("move".equals(t)) {
        final String p = r.getAsString("p");
        final String pt = r.getAsString("pt");
        final String to = r.getAsString("to");
        final String tot = r.getAsString("tot");
        if (!tot.equals(pt) || p.indexOf(to) != -1) {
          msg = applyRule(msg, new JtonObject()
              .set("t", "set")
              .set("p", to)
              .set("pt", tot)
              .set("to", p)
              .set("tot", pt));
          applyRule(msg, new JtonObject()
              .set("t", "delete")
              .set("p", p)
              .set("pt", pt));
        } else { // 2 step move if we moving from a child
          msg = applyRule(msg, new JtonObject()
              .set("t", "set")
              .set("p", "_temp_move")
              .set("pt", tot)
              .set("to", p)
              .set("tot", pt));
          applyRule(msg, new JtonObject()
              .set("t", "delete")
              .set("p", p)
              .set("pt", pt));
          msg = applyRule(msg, new JtonObject()
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
        msg = applyRule(msg, r);
      }
    }

    send(msg);
  }

  private JtonObject applyRule(JtonObject msg, JtonObject rule) {
    final String property = rule.getAsString("p");

    final String t = rule.getAsString("t");
    final String pt = rule.getAsString("pt");

    final JtonElement value = getToValue(msg, rule);
    final JtonElement fromParts = getFromValue(msg, rule);

    String fromType = null;
    JtonElement fromValue = null;

    if ("change".equals(t)) {
      final JtonElement from = rule.get("from");
      final String fromt = rule.getAsString("fromt");
      if ("msg".equals(fromt)) {
        fromValue = JRedUtil.getMessageProperty(msg, from.asString());
      } else if ("flow".equals(fromt) || "global".equals(fromt)) {
        fromValue = JRedUtil.getObjectProperty(getContext(fromt), from.asString());
      } else {
        fromType = fromt;
        fromValue = from;
      }
    }

    if ("msg".equals(pt)) {
      if ("delete".equals(t)) {
        JRedUtil.deleteMessageProperty(msg, property);
      } else if ("set".equals(t)) {
        JRedUtil.setMessageProperty(msg, property, value, true);
      } else if ("change".equals(t)) {
        final JtonPrimitive current = JRedUtil.getMessageProperty(msg, property)
            .asJtonPrimitive(false);
        if (current != null) {
          if (current.isString()) {
            if (("num".equals(fromType) || "bool".equals(fromType) || "str".equals(fromType))
                && current.asString().equals(fromValue.asString())) {
              // str representation of exact from number/boolean
              // only replace if they match exactly
              JRedUtil.setMessageProperty(msg, property, value, false);
            } else {
              JRedUtil.setMessageProperty(msg, property, new JtonPrimitive(current.asString()
                  .replaceAll(fromValue.asString(), value.asString())), false);
            }
          } else if (current.isNumber() && "num".equals(fromType)) {
            if (current.asNumber().equals(fromValue.asNumber())) {
              JRedUtil.setMessageProperty(msg, property, value, false);
            }
          } else if (current.isBoolean() && "bool".equals(fromType)) {
            if (current.asBoolean() == fromValue.asBoolean()) {
              JRedUtil.setMessageProperty(msg, property, value, false);
            }
          }
        }
      }
    } else {
      JtonObject target = null;
      if ("flow".equals(pt) || "global".equals(pt)) {
        target = getContext(pt);
      }
      if (target != null) {
        if ("delete".equals(t)) {
          JRedUtil.deleteObjectProperty(target, property);
        } else if ("set".equals(t)) {
          JtonUtil.setProperty(target, property, value, true);
        } else if ("change".equals(t)) {
          final JtonPrimitive current = JtonUtil.getProperty(target, property).asJtonPrimitive(null);
          if (current != null) {
            if (current.isString()) {
              if (("num".equals(fromType) || "bool".equals(fromType) || "str".equals(fromType))
                  && current.asString().equals(fromValue.asString())) {
                // str representation of exact from number/boolean
                // only replace if they match exactly
                JtonUtil.setProperty(target, property, value, true);
              } else {
                JRedUtil.setMessageProperty(msg, property, new JtonPrimitive(current.asString()
                    .replaceAll(fromValue.asString(), value.asString())), false);
              }
            } else if (current.isNumber() && "num".equals(fromType)) {
              if (current.asNumber().equals(fromValue.asNumber())) {
                JtonUtil.setProperty(target, property, value, true);
              }
            } else if (current.isBoolean() && "bool".equals(fromType)) {
              if (current.asBoolean() == fromValue.asBoolean()) {
                JtonUtil.setProperty(target, property, value, true);
              }
            }
          }
        }
      }
    }

    return msg;
  }

  private JtonElement getToValue(JtonObject msg, JtonObject rule) {
    return JRedUtil.evaluateNodeProperty(rule.getAsString("to"), rule.getAsString("tot"), this, msg);
  }

  private JtonObject getFromValue(JtonObject msg, JtonObject rule) {
    String fromValue;
    String fromType;
    String fromRE;

    final String t = rule.getAsString("t");
    final String fromt = rule.getAsString("fromt");
    if ("change".equals(t)) {
      if ("msg".equals(fromt) || "flow".equals(fromt) || "global".equals(fromt)) {
        if ("msg".equals(fromt)) {
          return getFromValueType(JRedUtil
              .getMessageProperty(msg, rule.getAsString("from")).asJtonPrimitive());
        }
      }
    }

    return null;
  }

  private JtonObject getFromValueType(JtonElement _fromValue) {
    final String fromType;
    final Pattern fromRE;
    final JtonObject fromParts = new JtonObject();
//    if (_fromValue.isJtonTransient()) {
//      final JtonTransient fromValue = _fromValue.asJtonTransient();
//      if (fromValue.getValue() instanceof Pattern) {
//        fromType = "re";
//        fromRE = (Pattern) fromValue.getValue();
//      }
//    } else if (_fromValue.isJtonPrimitive()) {
//      final JtonPrimitive fromValue = _fromValue.asJtonPrimitive();
//      if (fromValue.isNumber()) {
//        fromType = "num";
//      } else if (fromValue.isBoolean()) {
//        fromType = "bool";
//      }
//    }

    return fromParts;
  }
}
