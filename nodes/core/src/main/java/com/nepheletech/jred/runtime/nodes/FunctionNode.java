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

import java.util.ArrayList;
import java.util.List;

import javax.script.ScriptException;

import org.apache.camel.Exchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nepheletech.jfc.ScriptEvaluator;
import com.nepheletech.jred.runtime.flows.Flow;
import com.nepheletech.jred.runtime.util.JRedUtil;
import com.nepheletech.jton.JtonArray;
import com.nepheletech.jton.JtonElement;
import com.nepheletech.jton.JtonObject;

public class FunctionNode extends AbstractNode {
  private static final Logger logger = LoggerFactory.getLogger(FunctionNode.class);

  private static final String IMPORT = "//@import ";
  private static final int IMPORT_LEN = IMPORT.length();

  private final JtonObject nodeContext = new JtonObject();

  private final String func;
  @SuppressWarnings("unused")
  private final int outputs;

  private ScriptEvaluator<JtonElement> se = null;
  private Runnable closeHandler = null;

  // TODO package protect
  public final class Env {
    public JtonElement get(String key) {
      return getFlow().getSetting(key);
    }
  }

  public final Env env = new Env();

  public FunctionNode(Flow flow, JtonObject config) {
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

    imports.add(JtonElement.class.getPackage().getName() + ".*");
    imports.add(JRedUtil.class.getName());

    // create script evaluator

    final String[] _imports = imports.toArray(new String[imports.size()]);
    final String[] parameterNames = new String[] { "node", "msg", "env",
        "context", "flow", "global" };
    final Class<?>[] parameterTypes = new Class<?>[] {
        FunctionNode.class, JtonObject.class, Env.class,
        JtonObject.class, JtonObject.class, JtonObject.class
    };
    final Class<?>[] throwTypes = new Class<?>[] {
        Exception.class
    };

    se = new ScriptEvaluator<>(_imports, func,
        JtonElement.class, parameterNames, parameterTypes, throwTypes/*
                                                                      * , getName() != null ? getName() : getId()
                                                                      */);

    try {
      se.compile();
    } catch (ScriptException e) {
      throw new RuntimeException(e);
    }
  }

  public Runnable getCloseHandler() {
    return closeHandler;
  }

  public void setCloseHandler(Runnable closeHandler) {
    this.closeHandler = closeHandler;
  }

  @Override
  protected void onClosed(boolean removed) {
    logger.trace(">>> onClosed");

    if (closeHandler != null) {
      try {
        closeHandler.run();
      } catch (Exception e) {
        e.printStackTrace();
        // TODO report error
      }
      closeHandler = null;
    }

    status(new JtonObject());
  }

  @Override
  protected void onMessage(Exchange exchange, JtonObject msg) {
    logger.trace(">>> onMessage: msg={}", msg);

    try {
      send(exchange, se.evaluate(new Object[] { this, msg, this.env,
          this.nodeContext, this.getFlowContext(), this.getGlobalContext() }));
    } catch (ScriptException e) {
      final JtonArray sourceCode = new JtonArray()
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

  public void trace(JtonObject msg) {
    log_helper(TRACE, msg);
  }

  public void trace(String messafe) {
    log_helper(TRACE, messafe);
  }

  public void debug(JtonObject msg) {
    log_helper(DEBUG, msg);
  }

  public void debug(String messafe) {
    log_helper(DEBUG, messafe);
  }

  @Override
  public void log(JtonObject msg) {
    log_helper(INFO, msg);
  }

  public void warn(JtonObject msg) {
    log_helper(WARN, msg);
  }

  public void warn(String messafe) {
    log_helper(WARN, messafe);
  }

  public void error(JtonObject msg) {
    log_helper(ERROR, msg);
  }

  public void error(String messafe) {
    log_helper(ERROR, messafe);
  }
}
