package com.nepheletech.jred.runtime.nodes;

import java.util.ArrayList;
import java.util.List;

import javax.script.ScriptException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nepheletech.jfc.ScriptEvaluator;
import com.nepheletech.jred.runtime.flows.Flow;
import com.nepheletech.json.JsonArray;
import com.nepheletech.json.JsonElement;
import com.nepheletech.json.JsonObject;

public class FunctionNode extends AbstractNode {
  private static final Logger logger = LoggerFactory.getLogger(FunctionNode.class);

  private static final String IMPORT = "//@import ";
  private static final int IMPORT_LEN = IMPORT.length();

  private final String func;
  private final int outputs;

  private ScriptEvaluator<JsonElement> se = null;
  private Runnable stopHandler = null;

  public FunctionNode(Flow flow, JsonObject config) {
    super(flow, config);

    this.func = config.get("func").asString();
    this.outputs = config.get("outputs").asInt(1);

    // @imports

    final List<String> imports = new ArrayList<>();
    final String[] lines = func.split("\n");
    for (String line : lines) {
      if (line.startsWith(IMPORT)) {
        imports.add(line.substring(IMPORT_LEN).trim().replaceAll(";$", ""));
      }
    }

    imports.add(JsonElement.class.getPackage().getName() + ".*");

    // create script evaluator

    final String[] _imports = imports.toArray(new String[imports.size()]);
    final String[] parameterNames = new String[] { "node", "msg", "logger" };
    final Class<?>[] parameterTypes = new Class<?>[] {
        FunctionNode.class, JsonObject.class, Logger.class
    };
    final Class<?>[] throwTypes = new Class<?>[] {
        Exception.class
    };

    se = new ScriptEvaluator<>(_imports, func, JsonElement.class, parameterNames, parameterTypes, throwTypes,
        getName() != null ? getName() : getId());

    try {
      se.compile();
    } catch (ScriptException e) {
      throw new RuntimeException(e);
    }
  }

  public Runnable getStopHandler() { return stopHandler; }

  public void setStopHandler(Runnable stopHandler) { this.stopHandler = stopHandler; }

  public void stop() {
    logger.trace(">>> stop");

    try {
      if (stopHandler != null) {
        try {
          stopHandler.run();
        } catch (Exception e) {
          e.printStackTrace();
          // TODO report error
        }
        stopHandler = null;
      }
    } finally {
      super.close();
    }
  }

  @Override
  protected void onMessage(JsonObject msg) {
    logger.trace(">>> onMessage: msg={}", msg);

    try {
      if (outputs > 1) {
        send(se.evaluate(new Object[] { this, msg, logger }).asJsonArray(null));
      } else {
        send(se.evaluate(new Object[] { this, msg, logger }).asJsonObject(null));
      }
    } catch (ScriptException e) {
      final JsonArray sourceCode = new JsonArray()
          .push("// JFunction1.java");
      final String[] script = se.getScript().split("\n");
      for (String line : script) {
        sourceCode.push(line);
      }
      msg.set("_sourceCode", sourceCode);
      // ---
      throw new RuntimeException(e);
    }
  }
}
