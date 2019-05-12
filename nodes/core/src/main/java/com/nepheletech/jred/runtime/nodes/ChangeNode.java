package com.nepheletech.jred.runtime.nodes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nepheletech.jred.runtime.flows.Flow;
import com.nepheletech.jred.runtime.util.JRedUtil;
import com.nepheletech.jton.JtonArray;
import com.nepheletech.jton.JtonElement;
import com.nepheletech.jton.JtonObject;
import com.nepheletech.jton.JsonParser;
import com.nepheletech.jton.JtonPrimitive;
import com.nepheletech.jton.JsonUtil;

public class ChangeNode extends AbstractNode {
  private static final Logger logger = LoggerFactory.getLogger(ChangeNode.class);

  private final JtonArray rules;

  private boolean valid = true;

  public ChangeNode(Flow flow, JtonObject config) {
    super(flow, config);

    this.rules = config.getAsJtonArray("rules");

    for (int i = 0, iMax = rules.size(); i < iMax; i++) {
      final JtonObject rule = rules.getAsJtonObject(i);
      // Migrate to type-aware rules
      if (!rule.has("pt")) {
        rule.set("pt", "msg");
      }
      final String rule_t = rule.getAsString("t");
      if ("change".equals(rule_t) && rule.has("re")) {
        rule.set("fromt", "re");
        rule.remove("re");
      }
      if ("set".equals(rule_t) && !rule.has("tot")) {
        final String rule_to = rule.getAsString("to");
        if (rule_to.indexOf("msg.") == 0 && !rule.has("tot")) {
          rule.set("to", rule_to.substring(4));
          rule.set("tot", "msg");
        }
      }
      if (!rule.has("tot")) {
        rule.set("tot", "str");
      }
      if (!rule.has("fromt")) {
        rule.set("fromt", "str");
      }

      final String rule_fromt = rule.getAsString("fromt");
      if ("change".equals(rule_t)
          && !"msg".equals(rule_fromt) && !"flow".equals(rule_fromt) && !"global".equals(rule_fromt)) {
        throw new UnsupportedOperationException();
      }

      final String rule_to = rule.getAsString("to", null);
      final String rule_tot = rule.getAsString("tot", null);
      if ("num".equals(rule_tot)) {
        rule.set("to", rule.get("to").asNumber());
      } else if ("json".equals(rule_tot) || "bin".equals(rule_tot)) {
        try {
          // check this is parsable JSON
          JsonParser.parse(rule_to);
        } catch (Exception e) {
          valid = false;
          throw new IllegalArgumentException("change.errors.invalid-json"); // FIXME
        }
      } else if ("bool".equals(rule_tot)) {
        rule.set("to", Boolean.valueOf(rule_to));
      } else if ("jsonata".equals(rule_tot)) {
        // do nothing... TODO can we validate the expression?
      } else if ("env".equals(rule_tot)) {
        rule.set("to", JRedUtil.evaluateNodeProperty(rule_to, "env", this, null));
      }
    }
  }

  @Override
  protected void onMessage(JtonObject msg) {
    logger.trace("onMessage: msg={}", msg);

    if (!valid) { return; }

    for (JtonElement _rule : rules) {
      final JtonObject r = _rule.asJtonObject();
      final String r_t = r.getAsString("t");
      if ("move".equals(r_t)) {
        final String r_p = r.getAsString("p");
        final String r_pt = r.getAsString("pt");
        final String r_to = r.getAsString("to");
        final String r_tot = r.getAsString("tot");
        if (!r_tot.equals(r_pt) || r_p.indexOf(r_to) != -1) {
          msg = applyRule(msg, new JtonObject()
              .set("t", "set")
              .set("p", r_to)
              .set("pt", r_tot)
              .set("to", r_p)
              .set("tot", r_pt));
          applyRule(msg, new JtonObject()
              .set("t", "delete")
              .set("p", r_p)
              .set("pt", r_pt));
        } else { // 2 step move if we moving from a child
          msg = applyRule(msg, new JtonObject()
              .set("t", "set")
              .set("p", "_temp_move")
              .set("pt", r_tot)
              .set("to", r_p)
              .set("tot", r_pt));
          applyRule(msg, new JtonObject()
              .set("t", "delete")
              .set("p", r_p)
              .set("pt", r_pt));
          msg = applyRule(msg, new JtonObject()
              .set("t", "set")
              .set("p", r_to)
              .set("pt", r_tot)
              .set("to", "_temp_move")
              .set("tot", r_pt));
          applyRule(msg, new JtonObject()
              .set("t", "delete")
              .set("p", "_temp_move")
              .set("pt", r_pt));
        }
      } else {
        msg = applyRule(msg, r);
      }
      if (msg == null) { return; }
    }
    send(msg);
  }

  private JtonObject applyRule(JtonObject msg, JtonObject rule) {
    final String rule_t = rule.getAsString("t");
    final String rule_pt = rule.getAsString("pt");
    final String rule_tot = rule.getAsString("tot");

    final String property = rule.getAsString("p");

    JtonElement value = rule.get("to");

    if ("json".equals(rule_tot)) {
      value = JsonParser.parse(value.asString()).asJtonArray();
    } else if ("bin".equals(rule_tot)) {
      final JtonArray byteArray = JsonParser.parse(value.asString()).asJtonArray();
      value = new JtonPrimitive(JRedUtil.toBuffer(byteArray));
    } else if ("msg".equals(rule_tot)) {
      value = JRedUtil.getMessageProperty(msg, value.asString());
    } else if ("flow".equals(rule_tot) || "global".equals(rule_tot)) {
      value = JRedUtil.getObjectProperty(getContext(rule_tot), value.asString());
    } else if ("date".equals(rule_tot)) {
      value = new JtonPrimitive(System.currentTimeMillis());
    } else if ("jsonata".equals(rule_tot)) {
      value = JRedUtil.evaluateJSONataExpression(msg, value.asString());
    }

    String fromType = null;
    JtonElement fromValue = null;
    // TODO fromRE

    if ("change".equals(rule_t)) {
      final JtonElement rule_from = rule.get("from");
      final String rule_fromt = rule.getAsString("fromt");
      if ("msg".equals(rule_fromt) || "flow".equals(rule_fromt) || "global".equals(rule_fromt)) {
        throw new UnsupportedOperationException();
      } else {
        fromType = rule_fromt;
        fromValue = rule_from;
        // fromRE = rule.fromRE;
      }
    }

    if ("msg".equals(rule_pt)) {
      if ("delete".equals(rule_t)) {
        JRedUtil.setMessageproperty(msg, property, null, false);
      } else if ("set".equals(rule_t)) {
        JRedUtil.setMessageproperty(msg, property, value, true);
      } else if ("change".equals(rule_t)) {
        final JtonPrimitive current = JsonUtil.getProperty(msg, property).asJtonPrimitive(false);
        if (current != null) {
          if (current.isString()) {
            if (("num".equals(fromType) || "bool".equals(fromType) || "str".equals(fromType))
                && current.asString().equals(fromValue.asString())) {
              // str representation of exact from number/boolean
              // only replace if they match exactly
              JsonUtil.setProperty(msg, property, value, true);
            } else {
              // TODO regular expression
              throw new UnsupportedOperationException();
            }
          } else if (current.isNumber() && "num".equals(fromType)) {
            if (current.asNumber().equals(fromValue.asNumber())) {
              JsonUtil.setProperty(msg, property, value, true);
            }
          } else if (current.isBoolean() && "bool".equals(fromType)) {
            if (current.asBoolean() == fromValue.asBoolean()) {
              JsonUtil.setProperty(msg, property, value, true);
            }
          }
        }
      }
    } else {
      JtonObject target = null;
      if ("flow".equals(rule_pt) || "global".equals(rule_pt)) {
        target = getContext(rule_pt);
      }
      if (target != null) {
        if ("delete".equals(rule_t)) {
          JsonUtil.setProperty(target, property, null, false);
        } else if ("set".equals(rule_t)) {
          JsonUtil.setProperty(target, property, value, true);
        } else if ("change".equals(rule_t)) {
          final JtonPrimitive current = JsonUtil.getProperty(target, property).asJtonPrimitive(null);
          if (current != null) {
            if (current.isString()) {
              if (("num".equals(fromType) || "bool".equals(fromType) || "str".equals(fromType))
                  && current.asString().equals(fromValue.asString())) {
                // str representation of exact from number/boolean
                // only replace if they match exactly
                JsonUtil.setProperty(target, property, value, true);
              } else {
                throw new UnsupportedOperationException(); // XXX
              }
            } else if (current.isNumber() && "num".equals(fromType)) {
              if (current.asNumber().equals(fromValue.asNumber())) {
                JsonUtil.setProperty(target, property, value, true);
              }
            } else if (current.isBoolean() && "bool".equals(fromType)) {
              if (current.asBoolean() == fromValue.asBoolean()) {
                JsonUtil.setProperty(target, property, value, true);
              }
            }
          }
        }
      }
    }

    return msg;
  }
}
