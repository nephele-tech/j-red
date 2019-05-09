package com.nepheletech.jred.runtime.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jayway.jsonpath.JsonPath;
import com.nepheletech.jred.runtime.nodes.Node;
import com.nepheletech.jton.JtonArray;
import com.nepheletech.jton.JtonElement;
import com.nepheletech.jton.JtonNull;
import com.nepheletech.jton.JtonObject;
import com.nepheletech.jton.JsonParser;
import com.nepheletech.jton.JtonPrimitive;
import com.nepheletech.jton.JsonUtil;
import com.nepheletech.messagebus.MessageBus;

public final class JRedUtil {
  private static final Logger logger = LoggerFactory.getLogger(JRedUtil.class);

  private JRedUtil() {}

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
        final JtonPrimitive num = JsonParser.parse(value).asJsonPrimitive();
        return new JtonPrimitive(num.isNumber() ? num.asNumber() : Double.NaN);
      } catch (RuntimeException e) {
        return new JtonPrimitive(Double.NaN);
      }
    } else if ("json".equals(type)) {
      return JsonParser.parse(value);
    } else if ("re".equals(type)) {
      throw new UnsupportedOperationException();
    } else if ("date".equals(type)) {
      return new JtonPrimitive(System.currentTimeMillis());
    } else if ("bin".equals(type)) {
      final JtonArray byteArray = JsonParser.parse(value).asJsonArray();
      return new JtonPrimitive(JRedUtil.toBuffer(byteArray));
    } else if ("msg".equals(type) && msg != null) {
      return getMessageProperty(msg, value);
    } else if ("flow".equals(type) || "global".equals(type)) {
      return getObjectProperty(node.getContext(type), value);
    } else if ("bool".equals(type)) {
      return new JtonPrimitive(Boolean.valueOf(value));
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
            : JtonPrimitive.create(value);

    if ("msg".equals(type)) {
      setMessageproperty(msg, prop, _value, true);
    } else if ("flow".equals(type)) {
      setObjectProperty(node.getFlowContext(), prop, _value, true);
    } else if ("global".equals(type)) {
      setObjectProperty(node.getGlobalContext(), prop, _value, true);
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
  public static JtonElement evaluateEnvProperty(String value) {
    final String _value = System.getenv().get(value);
    return value != null ? new JtonPrimitive(_value) : JtonNull.INSTANCE;
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
  public static void setMessageproperty(JtonObject msg, String prop, JtonElement value, boolean createMissing) {
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
  public static JtonElement getObjectProperty(JtonObject msg, String expr) {
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
  public static void setObjectProperty(final JtonObject msg, String prop, JtonElement value, boolean createMissing) {
    JsonUtil.setProperty(msg, prop, value, createMissing);
  }

  /**
   * Evaluate a JSONata expression.
   * 
   * @param msg  the message object to evaluate against
   * @param expr the JSONata expression
   * @return the result of the expression
   */
  public static JtonElement evaluateJSONataExpression(JtonObject msg, String expr) {
    return JsonPath.read(msg, expr);
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
  public static JtonArray toByteArray(byte[] buffer) {
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

  public static void publish(String localTopic, String topic, JtonObject data) {
    MessageBus.sendMessage(localTopic, new JtonObject()
        .set("topic", topic)
        .set("data", data));
  }

  public static void publish(String localTopic, String topic, JtonObject data, boolean retain) { // TODO retain
    MessageBus.sendMessage(localTopic, new JtonObject()
        .set("topic", topic)
        .set("data", data));
  }
}
