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

import java.io.StringReader;
import java.io.StringWriter;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheException;
import com.github.mustachejava.MustacheFactory;
import com.nepheletech.jred.runtime.flows.Flow;
import com.nepheletech.jred.runtime.util.JRedUtil;
import com.nepheletech.jton.JtonElement;
import com.nepheletech.jton.JtonObject;
import com.nepheletech.jton.JtonParser;
import com.nepheletech.jton.JtonPrimitive;

public class TemplateNode extends AbstractNode {
  private final String field;
  private final String template;
  private final String syntax;
  private final String fieldType;
  private final String outputFormat;

  // Mustache
  private final MustacheFactory mf;
  private Mustache mustache;

  public TemplateNode(Flow flow, JtonObject config) {
    super(flow, config);

    this.field = config.get("field").asString("payload");
    this.template = config.get("template").asString();
    this.syntax = config.get("syntax").asString("mustache");
    this.fieldType = config.get("fieldType").asString("msg");
    this.outputFormat = config.get("output").asString("str");

    if ("mustache".equals(this.syntax)) {
      mf = new DefaultMustacheFactory();
      if (this.template != null) {
        try {
          mustache = mf.compile(new StringReader(template), getName() != null ? getName() : "template");
          // status("green", "dot", "compiled");
        } catch (MustacheException e) {
          mustache = null;
          // log(Constants.NODE_RED_ERROR, e, ExceptionUtils.getRootCauseMessage(e));
          // status("red", "dot", "template error");
        }
      } else {
        mustache = null;
      }
    } else {
      mf = null;
      mustache = null;
    }
  }

  @Override
  protected JtonElement onMessage(JtonObject msg) {
    logger.trace(">>> onMessage: msg={}", msg);

    try {

      // Allow template contents to be defined externally
      // through inbound `msg.template` if `this.template` empty.
      String template = this.template;
      if (msg.has("template")) {
        if (template == null || template.isEmpty()) {
          template = msg.getAsString("template");
        }
      }

      if ("mustache".equals(syntax)) {
        final Mustache mustache = (this.template == null)
            ? mf.compile(new StringReader(template), getName() != null ? getName() : "template")
            : this.mustache;
        final StringWriter w = new StringWriter();
        mustache.execute(w, prepare(msg.deepCopy()));
        output(msg, w.toString());
      } else {
        output(msg, template);
      }

    } catch (RuntimeException e) {
      e.printStackTrace();
      throw e;
    }

    return(msg);
  }

  protected Object prepare(final JtonObject msg) {
    return msg
        .set("flow", getFlowContext())
        .set("global", getGlobalContext());
  }

  private void output(JtonObject msg, String _value) {
    JtonElement value;

    if ("json".equals(outputFormat)) {
      value = JtonParser.parse(_value);
    } else if ("yaml".equals(outputFormat)) {
      value = YamlNode.yaml2jton(_value);
    } else {
      value = new JtonPrimitive(_value);
    }

    JRedUtil.setNodeProperty(field, fieldType, this, msg, value);
  }
}
