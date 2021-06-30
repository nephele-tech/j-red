/*
 * This file is part of J-RED API project.
 *
 * J-RED API is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * J-RED API is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this J-RED API; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package com.nepheletech.jred.runtime.util;

import java.time.Instant;
import java.util.Arrays;

import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jayway.jsonpath.JsonPath;
import com.nepheletech.jred.runtime.nodes.Node;
import com.nepheletech.jton.JtonArray;
import com.nepheletech.jton.JtonElement;
import com.nepheletech.jton.JtonNull;
import com.nepheletech.jton.JtonObject;
import com.nepheletech.jton.JtonParser;
import com.nepheletech.jton.JtonPrimitive;
import com.nepheletech.jton.JtonUtil;
import com.nepheletech.jton.jsonpath.JtonPathConfiguration;
import com.nepheletech.messagebus.MessageBus;

// See: @node-red/util/lib/util.js
public final class JRedUtil {
  private static final Logger logger = LoggerFactory.getLogger(JRedUtil.class);

  /**
   * Initialize JsonPath to use JTON objects.
   */
  static {
    JtonPathConfiguration.configure();
  }

  private JRedUtil() {
  }

  /**
   * 
   * @param value
   * @param type
   * @return
   */
  public static JtonElement evaluateNodeProperty(String value, String type) {
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
  public static JtonElement evaluateNodeProperty(String value, String type, Node node, JtonObject msg) {
    if ("str".equals(type)) {
      return new JtonPrimitive(value != null ? value : "");
    } else if ("num".equals(type)) {
      try {
        final JtonPrimitive num = JtonParser.parse(value).asJtonPrimitive();
        return new JtonPrimitive(num.isNumber() ? num.asNumber() : Double.NaN);
      } catch (RuntimeException e) {
        return new JtonPrimitive(Double.NaN);
      }
    } else if ("json".equals(type)) {
      return JtonParser.parse(value);
    } else if ("re".equals(type)) {
      // TODO regular expression
      throw new UnsupportedOperationException();
    } else if ("date".equals(type)) {
      return new JtonPrimitive(Instant.now().toEpochMilli());
    } else if ("bin".equals(type)) {
      final JtonArray byteArray = JtonParser.parse(value).asJtonArray();
      return new JtonPrimitive(JRedUtil.toBuffer(byteArray));
    } else if ("msg".equals(type) && msg != null) {
      return getMessageProperty(msg, value);
    } else if ("flow".equals(type) || "global".equals(type)) {
      return getObjectProperty(node.getContext(type), value);
    } else if ("bool".equals(type)) {
      return new JtonPrimitive(Boolean.valueOf(value));
    } else if ("jsonpath".equals(type)) {
      return evaluateJsonPathExpression(msg, value);
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
   * 
   * @param prop
   * @param type
   * @param node
   * @param msg
   * @param value
   */
  public static void setNodeProperty(String prop, String type, Node node, JtonObject msg, Object value) {
    JtonElement _value = value instanceof JtonElement
        ? (JtonElement) value
        : value == null
            ? JtonNull.INSTANCE
            : new JtonPrimitive(value, false);

    if ("msg".equals(type)) {
      setMessageProperty(msg, prop, _value, true);
    } else if ("flow".equals(type)) {
      setObjectProperty(node.getFlowContext(), prop, _value, true);
    } else if ("global".equals(type)) {
      setObjectProperty(node.getGlobalContext(), prop, _value, true);
    }
  }

  public static void deleteNodeProperty(String prop, String type, Node node, JtonObject msg) {
    if ("msg".equals(type)) {
      deleteMessageProperty(msg, prop);
    } else if ("flow".equals(type)) {
      deleteObjectProperty(node.getFlowContext(), prop);
    } else if ("global".equals(type)) {
      deleteObjectProperty(node.getGlobalContext(), prop);
    }
  }

  /**
   * Checks if a string contains any Environment Variable specifiers and returns
   * it with their values substituted in place.
   * <p>
   * For example, if the env var `WHO` is set to `Joe`, the string `Hello ${WHO}!`
   * will return `Hello Joe!`.
   * 
   * @param env the string to parse
   * @return
   */
  public static JtonElement evaluateEnvProperty(String env) {
    // TODO implementation
    final String _value = env != null ? System.getenv(env) : null;
    return _value != null ? new JtonPrimitive(_value) : JtonNull.INSTANCE;
  }

  /**
   * Gets a property of a message object.
   * <p>
   * Unlike {@link #getObjectProperty(JtonObject, String)}, this method will strip
   * {@code msg.} from the front of the property expression if present.
   * 
   * @param msg  the message object
   * @param expr the property expression
   * @return the message property, or undefined if it does exist
   */
  public static JtonElement getMessageProperty(JtonObject msg, String expr) {
    if (expr.indexOf("msg.") == 0) {
      expr = expr.substring(4);
    }
    return getObjectProperty(msg, expr);
  }

  /**
   * Unlike {@link #setObjectProperty(JtonObject, String, JtonElement, boolean)},
   * this function will strip `msg.` from the front of the property expression if
   * present.
   * 
   * @param msg           the message object
   * @param prop          the property expression
   * @param value         the value to set
   * @param createMissing whether to create missing parent properties
   */
  public static void setMessageProperty(JtonObject msg, String prop, Object value, boolean createMissing) {
    if (prop.indexOf("msg.") == 0) {
      prop = prop.substring(4);
    }

    final JtonElement _value = value instanceof JtonElement
        ? (JtonElement) value
        : value == null
            ? JtonNull.INSTANCE
            : new JtonPrimitive(value, false);

    setObjectProperty(msg, prop, _value, createMissing);
  }

  /**
   * Unlike {@link #removeObjectProperty(JtonObject, String)}, this function will
   * strip `msg.` from the front of the property expression if present.
   * 
   * @param msg  the message object
   * @param prop the property expression
   */
  public static void deleteMessageProperty(JtonObject msg, String prop) {
    if (prop.indexOf("msg.") == 0) {
      prop = prop.substring(4);
    }
    deleteObjectProperty(msg, prop);
  }

  /**
   * Gets a property of an object.
   * 
   * @param msg  the object
   * @param expr the property expression
   * @return the object property, or undefined if it does not exist
   */
  public static JtonElement getObjectProperty(JtonObject msg, String expr) {
    return JtonUtil.getProperty(msg, expr);
  }

  /**
   * Set a property of an object.
   * 
   * @param msg           the object
   * @param prop          the property expression
   * @param value         the value to set
   * @param createMissing whether to create parent properties
   */
  public static void setObjectProperty(final JtonObject msg, String prop, JtonElement value, boolean createMissing) {
    JtonUtil.setProperty(msg, prop, value, createMissing);
  }

  /**
   * Delete a property of an object.
   * 
   * @param msg  the object
   * @param prop the property expression
   */
  public static void deleteObjectProperty(final JtonObject msg, String prop) {
    JtonUtil.deleteProperty(msg, prop);
  }

  /**
   * 
   * @param expr
   * @return
   */
  public static JsonPath prepareJsonPathExpression(String expr) {
    return JsonPath.compile(expr);
  }

  /**
   * Evaluate a JsonPath expression.
   * 
   * @param msg  the message object to evaluate against
   * @param expr the JsonPath expression
   * @return the result of the expression
   */
  public static JtonElement evaluateJsonPathExpression(JtonObject msg, String expr) {
    try {
      return JsonPath.read(msg, expr);
    } catch (RuntimeException e) {
      if (logger.isDebugEnabled()) {
        logger.debug("JsonPath expr failed: " + expr, e);
      }
      return JtonNull.INSTANCE;
    }
  }

  /**
   * Evaluate a JsonPath expression.
   * 
   * @param msg
   * @param value
   * @return
   */
  public static JtonElement evaluateJsonPathExpression(JtonObject msg, JsonPath jsonPath) {
    try {
      return jsonPath.read(msg);
    } catch (RuntimeException e) {
      if (logger.isDebugEnabled()) {
        logger.debug("JsonPath expr failed: " + jsonPath != null ? jsonPath.getPath() : null, e);
      }
      return JtonNull.INSTANCE;
    }
  }

  /**
   * Encode an object to JSON without losing information about non-JSON types such
   * as Buffer and Function.
   * 
   * *This function is closely tied to its reverse within the editor*
   * 
   * @param msg
   * @param opts
   * @return the enocded object
   */
  public static JtonObject encodeObject(JtonObject msg, JtonObject opts) {
    try {
      int debugLength = 1_000;
      if (opts != null && opts.has("maxLength")) {
        debugLength = opts.get("maxLength").asInt(debugLength);
      }

      final JtonElement _msg = msg.get("msg");
      
      // TODO Error
      
      if (_msg.isJtonObject()) {
        msg.set("format", "Object");
        msg.set("msg", _msg.toString());
      } else if (_msg.isJtonArray()) {
        final JtonArray a = _msg.asJtonArray();
        final int arrayLength = a.size();
        msg.set("format", "array[" + arrayLength + "]");
        if (arrayLength > debugLength) {
          msg.set("msg", new JtonObject()
              .set("__enc__", true)
              .set("type", "array")
              .set("data", new JtonArray(a.subList(0, debugLength)))
              .set("length", debugLength).toString());
        } else {
          msg.set("msg", a.toString());
        }
      } else if (_msg.isJtonPrimitive()) {
        final JtonPrimitive p = _msg.asJtonPrimitive();
        if (p.isJtonTransient()) {
          msg.set("msg", "[Type not printable]");
        } else if (p.isBoolean()) {
          msg.set("format", "boolean");
          msg.set("msg", _msg);
        } else if (p.isNumber()) {
          msg.set("format", "number");
          msg.set("msg", _msg);
        } else if (p.getValue() instanceof byte[]) {
          
          //
          // Buffer
          //
          
          final byte[] buffer = p.getValue();
          final int bufferLength = buffer.length;
          msg.set("format", "buffer[" + bufferLength + "]");
          if (bufferLength > debugLength) {
            //msg.set("msg", toJtonArray(Arrays.copyOf(buffer, debugLength)));
            msg.set("msg", new JtonPrimitive(Hex.encodeHexString(Arrays.copyOf(buffer, debugLength))));
          } else {
            //msg.set("msg", toJtonArray(buffer));
            msg.set("msg", new JtonPrimitive(Hex.encodeHexString(buffer)));
          }
        } else {
          final String str = p.asString();
          final int strLength = str.length();
          msg.set("format", "string[" + strLength + "]");
          msg.set("msg", (strLength > debugLength)
              ? str.substring(0, debugLength) + "..."
              : str);
        }
      } else if (_msg.isJtonNull()) {
        msg.set("format", "undefined");
        msg.set("msg", "(undefined)");
      }

      return msg;
    } catch (Exception e) {

      return null;
    }
  }

  /**
   * 
   * @param byteArray
   * @return
   */
  public static byte[] toBuffer(JtonArray byteArray) {
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
  public static JtonArray toJtonArray(byte[] buffer) {
    final JtonArray byteArray = new JtonArray();
    for (byte b : buffer) {
      byteArray.push(b);
    }
    return byteArray;
  }

  /**
   * 
   * @return
   */
  public static JtonArray stackTrace(Throwable t) {
    final JtonArray stackTrace = new JtonArray();
    StackTraceElement elements[] = t.getStackTrace();
    for (StackTraceElement e : elements) {
      stackTrace.push(new JtonObject()
          .set("className", e.getClassName())
          .set("methodName", e.getMethodName())
          .set("fileName", e.getFileName())
          .set("lineNumber", e.getLineNumber()));
    }
    return stackTrace;
  }

  @Deprecated
  public static void publish(String localTopic, String topic, JtonObject data) {
    MessageBus.sendMessage(localTopic, new JtonObject()
        .set("topic", topic)
        .set("data", data));
  }

  @Deprecated
  public static void publish(String localTopic, String topic, JtonObject data, boolean retain) { // TODO retain
    MessageBus.sendMessage(localTopic, new JtonObject()
        .set("topic", topic)
        .set("data", data));
  }
}
