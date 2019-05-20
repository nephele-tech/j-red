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
import com.nepheletech.jton.JsonParser;
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
  protected void onMessage(JtonObject msg) {
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

    } catch(RuntimeException e) {
      e.printStackTrace();
      throw e;
    }
    
    send(msg);
  }

  protected Object prepare(final JtonObject msg) {
    msg.set("_flow", getFlowContext());
    msg.set("_global", getGlobalContext());
    return msg;
  }

  private void output(JtonObject msg, String _value) {
    JtonElement value;
    
    logger.debug("----------------------------------------------------------------------{}", _value);

    if ("json".equals(outputFormat)) {
      value = JsonParser.parse(_value);
    } else if ("yaml".equals(outputFormat)) {
      throw new UnsupportedOperationException();
    } else {
      value = new JtonPrimitive(_value);
    }

    if ("msg".equals(fieldType)) {
      JRedUtil.setMessageproperty(msg, field, value, true);
    } else if ("flow".equals(fieldType)
        || "global".equals(fieldType)) { throw new UnsupportedOperationException(); }
  }
}
