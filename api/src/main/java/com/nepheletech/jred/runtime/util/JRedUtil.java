package com.nepheletech.jred.runtime.util;

import com.jayway.jsonpath.JsonPath;
import com.nepheletech.jred.runtime.nodes.Node;
import com.nepheletech.json.JsonArray;
import com.nepheletech.json.JsonElement;
import com.nepheletech.json.JsonNull;
import com.nepheletech.json.JsonObject;
import com.nepheletech.json.JsonParser;
import com.nepheletech.json.JsonPrimitive;
import com.nepheletech.json.JsonUtil;

public final class JRedUtil {

  private JRedUtil() {}
  
  /**
   * 
   * @param value
   * @param type
   * @return
   */
  public static JsonElement evaluateNodeProperty(String value, String type) {
    return evaluateNodeProperty(value, type, null, null);
  }

  /**
   * 
   * @param value
   * @param type
   * @param node
   * @param msg
   * @return
   */
  public static JsonElement evaluateNodeProperty(String value, String type, Node node, JsonObject msg) {
    if ("str".equals(type)) {
      return new JsonPrimitive(value != null ? value : "");
    } else if ("num".equals(type)) {
      try {
        final JsonPrimitive num = JsonParser.parse(value).asJsonPrimitive();
        return new JsonPrimitive(num.isNumber() ? num.asNumber() : Double.NaN);
      } catch (RuntimeException e) {
        return new JsonPrimitive(Double.NaN);
      }
    } else if ("json".equals(type)) {
      return JsonParser.parse(value);
    } else if ("re".equals(type)) {
      throw new UnsupportedOperationException();
    } else if ("date".equals(type)) {
      return new JsonPrimitive(System.currentTimeMillis());
    } else if ("bin".equals(type)) {
      throw new UnsupportedOperationException();
    } else if ("msg".equals(type) && msg != null) {
      return getMessageProperty(msg, value);
    } else if ("flow".equals(type) || "global".equals(type)) {
      return getObjectProperty(node.getContext(type), value);
    } else if ("bool".equals(type)) {
      return new JsonPrimitive(Boolean.valueOf(value));
    } else if ("jsonata".equals(type)) {
      return evaluateJSONataExpression(msg, value);
    } else if ("env".equals(type)) {
      if (node != null) {
        return node.getFlow().getSetting(value);
      } else {
        return evaluateEnvProperty(value);
      }
    } else {
      return msg.get(value);
    }
  }

  /**
   * Checks if a string contains any Environment Variable specifiers and returns
   * it with their values substituted in place.
   * <p>
   * For example, if the env var `WHO` is set to `Joe`, the string `Hello ${WHO}!`
   * will return `Hello Joe!`.
   * 
   * @param value the string to parse
   * @return
   */
  public static JsonElement evaluateEnvProperty(String value) {
    final String _value = System.getenv().get(value);
    return value != null ? new JsonPrimitive(_value) : JsonNull.INSTANCE;
  }

  /**
   * Gets a property of a message object.
   * <p>
   * Unlike {@link #getObjectProperty(JsonObject, String)}, this method will strip
   * {@code msg.} from the front of the property expression if present.
   * 
   * @param msg  the message object
   * @param expr the property expression
   * @return the message property, or undefined if it does exist
   */
  public static JsonElement getMessageProperty(JsonObject msg, String expr) {
    if (expr.indexOf("msg.") == 0) {
      expr = expr.substring(4);
    }
    return getObjectProperty(msg, expr);
  }

  /**
   * Unlike {@link #setObjectProperty(JsonObject, String, JsonElement, boolean)},
   * this function will strip `msg.` from the front of the property expression if
   * present.
   * 
   * @param msg           the message object
   * @param prop          the property expression
   * @param value         the value to set
   * @param createMissing whether to create missing parent properties
   */
  public static void setMessageproperty(JsonObject msg, String prop, JsonElement value, boolean createMissing) {
    if (prop.indexOf("msg.") == 0) {
      prop = prop.substring(4);
    }
    setObjectProperty(msg, prop, value, createMissing);
  }

  /**
   * Gets a property of an object.
   * 
   * @param msg  the object
   * @param expr the property expression
   * @return the object property, or undefined if it does not exist
   */
  public static JsonElement getObjectProperty(JsonObject msg, String expr) {
    return JsonUtil.getProperty(msg, expr);
  }

  /**
   * Set a property of an object.
   * 
   * @param msg           the object
   * @param prop          the property expression
   * @param value         the value to set
   * @param createMissing whether to create parent properties
   */
  public static void setObjectProperty(final JsonObject msg, String prop, JsonElement value, boolean createMissing) {
    JsonUtil.setProperty(msg, prop, value, createMissing);
  }

  /**
   * Evaluate a JSONata expression.
   * 
   * @param msg  the message object to evaluate against
   * @param expr the JSONata expression
   * @return the result of the expression
   */
  public static JsonElement evaluateJSONataExpression(JsonObject msg, String expr) {
    return JsonPath.read(msg, expr);
  }
  
  /**
   * 
   * @param byteArray
   * @return
   */
  public static byte[] toBuffer(JsonArray byteArray) {
    final byte[] buffer = new byte[byteArray.size()];
    for (int i = 0, iMax = buffer.length; i < iMax; i++) {
      buffer[i] = byteArray.getAsByte(i);
    }
    return buffer;
  }
  
  /**
   * 
   * @param buffer
   * @return
   */
  public static JsonArray toByteArray(byte[] buffer) {
    final JsonArray byteArray = new JsonArray();
    for (byte b : buffer) {
      byteArray.push(b);
    }
    return byteArray;
  }
}
