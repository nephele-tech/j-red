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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nepheletech.jfc.ScriptEvaluator;
import com.nepheletech.jred.runtime.flows.Flow;
import com.nepheletech.jton.JtonArray;
import com.nepheletech.jton.JtonElement;
import com.nepheletech.jton.JtonObject;

public class FunctionNode extends AbstractNode {
  private static final Logger logger = LoggerFactory.getLogger(FunctionNode.class);

  private static final String IMPORT = "//@import ";
  private static final int IMPORT_LEN = IMPORT.length();

  private final String func;
  private final int outputs;

  private ScriptEvaluator<JtonElement> se = null;
  private Runnable stopHandler = null;

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

    // create script evaluator

    final String[] _imports = imports.toArray(new String[imports.size()]);
    final String[] parameterNames = new String[] { "node", "msg", "logger" };
    final Class<?>[] parameterTypes = new Class<?>[] {
        FunctionNode.class, JtonObject.class, Logger.class
    };
    final Class<?>[] throwTypes = new Class<?>[] {
        Exception.class
    };

    se = new ScriptEvaluator<>(_imports, func, JtonElement.class, parameterNames, parameterTypes, throwTypes,
        getName() != null ? getName() : getId());

    try {
      se.compile();
    } catch (ScriptException e) {
      throw new RuntimeException(e);
    }
  }

  public Runnable getStopHandler() { return stopHandler; }

  public void setStopHandler(Runnable stopHandler) { this.stopHandler = stopHandler; }

  @Override
  protected void onClosed(boolean removed) {
    logger.trace(">>> onClosed");

    if (stopHandler != null) {
      try {
        stopHandler.run();
      } catch (Exception e) {
        e.printStackTrace();
        // TODO report error
      }
      stopHandler = null;
    }
  }

  @Override
  protected void onMessage(JtonObject msg) {
    logger.trace(">>> onMessage: msg={}", msg);

    try {
      if (outputs > 1) {
        send(se.evaluate(new Object[] { this, msg, logger }).asJtonArray(null));
      } else {
        send(se.evaluate(new Object[] { this, msg, logger }).asJtonObject(null));
      }
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
}
