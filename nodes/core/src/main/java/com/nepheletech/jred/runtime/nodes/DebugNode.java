package com.nepheletech.jred.runtime.nodes;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.BiConsumer;

import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nepheletech.jred.runtime.flows.Flow;
import com.nepheletech.jred.runtime.util.JRedUtil;
import com.nepheletech.json.JsonArray;
import com.nepheletech.json.JsonElement;
import com.nepheletech.json.JsonObject;
import com.nepheletech.json.JsonPrimitive;

public final class DebugNode extends AbstractNode {
  private final Logger logger = LoggerFactory.getLogger(DebugNode.class);

  private boolean active;

  private final boolean tosidebar;
  private final boolean console;
  private final boolean tostatus;
  private final String complete;
  private final int severity;

  private final String preparedEditExpression = null;

  public DebugNode(Flow flow, JsonObject config) {
    super(flow, config);

    final String targetType = config.get("targetType").asString("msg");
    final boolean hasEditExpression = "jsonata".equals(targetType);
    final String editExpression = hasEditExpression ? config.get("complete").asString() : null;
    final String complete = hasEditExpression ? null : config.get("complete").asString("payload");
    this.complete = "false".equals(complete) ? "payload" : complete;
    this.console = config.get("console").asBoolean(false);
    this.tostatus = !"true".contentEquals(this.complete) && config.getAsBoolean("tostatus", false);
    this.tosidebar = config.get("tosidebar").asBoolean(true);
    this.severity = config.get("severity").asInt(40);
    this.active = config.get("active").asBoolean(true);

    if (this.tostatus) {
      // TODO set status
    } else {
      // TODO clear status
    }

    // TODO editExpression

    logger.trace(">>> created");
  }

  public boolean isActive() { return active; }

  public void setActive(boolean active) {
    logger.trace(">>> setActive: {}", active);
    this.active = active;
  }

  public boolean isTosidebar() { return tosidebar; }

  public boolean isTostatus() { return tostatus; }

  @Override
  protected void onMessage(final JsonObject msg) {
    logger.trace(">>> onMessage: id={}, msg={}", getId(), msg);

    if ("true".equals(complete)) {
      // debug complete msg object
      if (this.console) {
        log(msg); // TODO: colors & depth
      }
      if (active && tosidebar) {
        sendDebug(new JsonObject()
            .set("id", getId())
            .set("name", getName())
            .set("topic", msg.get("topic"))
            .set("msg", msg)
            .set("_path", msg.get("_path")));
      }
    } else {
      prepareValue(msg, (error, _msg) -> {
        if (error != null) {
          error(error, null);
          return;
        }
        final JsonElement output = _msg.get("msg");
        if (this.console) {
          if (output.isJsonPrimitive()) {
            final JsonPrimitive _output = output.asJsonPrimitive();
            log(msg);
          } else if (output.isJsonObject()) {
            log(msg);
          } else {
            log(msg);
          }
        }
        if (tostatus) {
          String st = output.isJsonPrimitive() ? output.asString() : output.toString();
          if (st.length() > 32) {
            st = st.substring(0, 32) + "...";
          }
          
          logger.debug("send status: {}", st);
          
          status(new JsonObject()
              .set("fill", "grey")
              .set("shape", "dot")
              .set("text", st));
        }
        if (active && tosidebar) {
          sendDebug(_msg);
        }
      });
    }
  }

  private void sendDebug(JsonObject msg) {
    logger.trace(">>> sendDebug: active={}, msg={}", active, msg);

    // don't put blank errors in sidebar (but do add to logs)
    msg = encodeObject(msg, null); // FIXME maxLength
    
    JRedUtil.publish("debug", "debug", msg);
  }

  private void prepareValue(JsonObject msg, BiConsumer<Throwable, JsonObject> done) {
    // Either apply the jsonata expression or...
    if (preparedEditExpression != null) {
      // TODO
    } else {
      // Extract the required message property
      String property = "payload";
      JsonElement output = msg.get(property); // XXX FIXME
      if (!"false".equals(complete) && complete != null) {
        property = this.complete;
        output = msg.get(property); // XXX FIXME
      }
      done.accept(null, new JsonObject()
          //.set("id", Optional.ofNullable(getAlias()).orElse(getId()))
          .set("id", getId())
          .set("z", getZ())
          .set("name", getName())
          .set("topic", msg.get("topic"))
          .set("property", property)
          .set("msg", output)
          .set("_path", msg.get("_path")));
    }
  }

  private JsonObject encodeObject(JsonObject msg, JsonObject opts) {
    int debugLength = 1_000;
    if (opts != null && opts.has("maxLength")) {
      debugLength = opts.get("maxLength").asInt(debugLength);
    }

    final JsonElement _msg = msg.get("msg");
    
    if (_msg.isJsonObject()) {
      msg.set("format", "Object");
      msg.set("msg", _msg.toString());
    } else if (_msg.isJsonArray()) {
      final JsonArray a = _msg.asJsonArray();
      final int arrayLength = a.size();
      msg.set("format", "array[" + arrayLength + "]");
      if (arrayLength > debugLength) {
        msg.set("msg", new JsonObject()
            .set("__enc__", true)
            .set("type", "array")
            .set("data", new JsonArray(a.subList(0, debugLength)))
            .set("length", debugLength).toString());
      } else {
        msg.set("msg", a.toString());
      }
    } else if (_msg.isJsonPrimitive()) {
      final JsonPrimitive p = _msg.asJsonPrimitive();
      if (p.isJsonTransient()) {
        msg.set("msg", "[Type not printable]");
      } else if (p.isBoolean()) {
        msg.set("format", "boolean");
        msg.set("msg", _msg);
      } else if (p.isNumber()) {
        msg.set("format", "number");
        msg.set("msg", _msg);
      } else if (p.getValue() instanceof byte[]) {
        final byte[] buffer = (byte[]) p.getValue();
        final int bufferLength = buffer.length;
        msg.set("format", "buffer[" + bufferLength + "]");
        if (bufferLength > debugLength) {
          msg.set("msg", new JsonPrimitive(Hex.encodeHexString(Arrays.copyOf(buffer, debugLength))));
        } else {
          msg.set("msg", new JsonPrimitive(Hex.encodeHexString(buffer)));
        }
      } else {
        final String str = p.asString();
        final int strLength = str.length();
        msg.set("format", "string[" + strLength + "]");
        msg.set("msg", (strLength > debugLength)
            ? str.substring(0, debugLength) + "..."
            : str);
      }
    } else if (_msg.isJsonNull()) {
      msg.set("format", "undefined");
      msg.set("msg", "(undefined)");
    }

    return msg;
  }
}
