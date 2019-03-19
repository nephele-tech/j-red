package com.nepheletech.jred.runtime.nodes;

import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nepheletech.jred.runtime.flows.Flow;
import com.nepheletech.json.JsonArray;
import com.nepheletech.json.JsonObject;
import com.nepheletech.json.JsonParser;

public class ChangeNode extends AbstractNode {
  private static final Logger logger = LoggerFactory.getLogger(ChangeNode.class);

  private final JsonArray rules;

  public ChangeNode(Flow flow, JsonObject config) {
    super(flow, config);
    
    this.rules = config.getAsJsonArray("rules");

    boolean valid = true;
    for (int i = 0, n = rules.size(); i < n; i++) {
      final JsonObject rule = rules.get(i).asJsonObject();
      // Migrate to type-aware rules
      if (!rule.has("pt")) {
        rule.set("pt", "msg");
      }
      final String rule_t = rule.get("t").asString(null);
      if ("change".equals(rule_t) && rule.has("re")) {
        rule.set("fromt", "re");
        rule.remove("re");
      }
      if ("set".equals(rule_t) && !rule.has("tot")) {
        final String rule_to = rule.get("to").asString();
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

      final String rule_fromt = rule.get("fromt").asString();
      if ("change".equals(rule_t)
          && !"msg".equals(rule_fromt) && !"flow".equals(rule_fromt) && !"global".equals(rule_fromt)) {
        throw new UnsupportedOperationException();
      }

      final String rule_to = rule.get("to").asString();
      final String rule_tot = rule.get("tot").asString();
      if ("num".equals(rule_tot)) {
        rule.set("to", rule.get("to").asNumber());
      } else if ("json".equals(rule_tot) || "bin".equals(rule_tot)) {
        try {
          // check this is parsable JSON
          JsonParser.parse(rule_to);
        } catch (Exception e) {
          valid = false;
          // TODO error();
        }
      } else if ("bool".equals(rule_tot)) {
        rule.set("to", Boolean.valueOf(rule_to));
      } else if ("jsonata".equals(rule_tot)) {
        throw new UnsupportedOperationException();
      } else if ("env".equals(rule_tot)) {
        rule.set("to", evaluateNodeProperty(rule_to, "env", null, null));
      }
    }
  }

  @Override
  protected void onMessage(final JsonObject _msg) {
    logger.trace("onMessage: msg={}", _msg);

    applyRules(_msg, 0, msg -> {
      if (msg.has("error")) {
        // TODO node.error(err,msg);
      } else {
        send(msg);
      }
    });
  }

  private void applyRules(JsonObject msg, int currentRule, Consumer<JsonObject> done) {
    logger.trace(">>> applyRules: rule={}", rules.get(currentRule));
    
    if (currentRule >= rules.size()) {
      done.accept(msg);
      return;
    }
    
    final JsonObject r = rules.get(currentRule).asJsonObject();
    final String r_t = r.get("t").asString();
    if ("move".equals(r_t)) {
      
    } else {
      
    }
  }
}
