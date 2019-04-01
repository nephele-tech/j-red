package com.nepheletech.jred.runtime.nodes;

import java.io.StringReader;
import java.io.StringWriter;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheException;
import com.github.mustachejava.MustacheFactory;
import com.nepheletech.jred.runtime.flows.Flow;
import com.nepheletech.jred.runtime.util.JRedUtil;
import com.nepheletech.json.JsonElement;
import com.nepheletech.json.JsonObject;
import com.nepheletech.json.JsonParser;
import com.nepheletech.json.JsonPrimitive;

public class TemplateNode extends AbstractNode {
  private final String field;
  private final String template;
  private final String syntax;
  private final String fieldType;
  private final String outputFormat;

  private final MustacheFactory mf;
  private Mustache mustache;

  public TemplateNode(Flow flow, JsonObject config) {
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
  protected void onMessage(JsonObject msg) {
    logger.trace(">>> onMessage: msg={}", msg);

    // Allow template contents to be defined externally
    // through inbound `msg.template` if `this.template` empty.
    String template = this.template;
    if (msg.has("template")) {
      if (template == null || template.isEmpty()) {
        template = msg.get("template").asString();
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

    send(msg);
  }

  protected Object prepare(final JsonObject msg) {
    // TODO

    return msg;
  }

  private void output(JsonObject msg, String _value) {
    JsonElement value;

    if ("json".equals(outputFormat)) {
      value = JsonParser.parse(_value);
    } else if ("yaml".equals(outputFormat)) {
      throw new UnsupportedOperationException();
    } else {
      value = new JsonPrimitive(_value);
    }

    if ("msg".equals(fieldType)) {
      JRedUtil.setMessageproperty(msg, field, value, true);
    } else if ("flow".equals(fieldType)
        || "global".equals(fieldType)) { throw new UnsupportedOperationException(); }
  }
}
