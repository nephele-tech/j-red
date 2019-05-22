package com.nepheletech.jred.runtime.nodes;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;

import com.nepheletech.jred.runtime.flows.Flow;
import com.nepheletech.jred.runtime.util.JRedUtil;
import com.nepheletech.jton.JtonArray;
import com.nepheletech.jton.JtonElement;
import com.nepheletech.jton.JtonNull;
import com.nepheletech.jton.JtonObject;
import com.nepheletech.jton.JtonPrimitive;

public class YamlNode extends AbstractNode {

  private final String property;

  public YamlNode(Flow flow, JtonObject config) {
    super(flow, config);

    this.property = config.getAsString("property", "payload");
  }

  @Override
  protected void onMessage(JtonObject msg) {
    logger.trace(">>> onMessage: msg={}", msg);

    final JtonElement value = JRedUtil.getMessageProperty(msg, property);
    if (value.isJtonPrimitive() && value.asJtonPrimitive().isString()) {
      JRedUtil.setMessageProperty(msg, property, yaml2jton(value.asString()), false);
    } else {
      JRedUtil.setMessageProperty(msg, property, new JtonPrimitive(jton2yaml(value)), false);
    }

    send(msg);
  }

  // ---

  private static final ThreadLocal<Yaml> yaml = new ThreadLocal<Yaml>();

  public static JtonElement yaml2jton(String yamlStr) {
    if (yaml.get() == null) {
      yaml.set(new Yaml());
    }

    return snakeyamlToJton(yaml.get().load(yamlStr));
  }

  public static String jton2yaml(JtonElement jton) {
    if (yaml.get() == null) {
      yaml.set(new Yaml());
    }

    return yaml.get().dump(jtonToSnakeyaml(jton));
  }

  private static Object jtonToSnakeyaml(JtonElement jton) {
    // JtonNull => null
    if (jton.isJtonNull())
      return null;

    // JtonArray => ArrayList
    if (jton.isJtonArray()) {
      List<Object> array = new ArrayList<>();
      for (JtonElement e : jton.asJtonArray()) {
        array.add(jtonToSnakeyaml(e));
      }
      return array;
    }

    // JtonObject => Map
    if (jton.isJtonObject()) {
      Map<String, Object> map = new LinkedHashMap<>();
      for (final Map.Entry<String, JtonElement> entry : jton.asJtonObject().entrySet()) {
        map.put(entry.getKey(), jtonToSnakeyaml(entry.getValue()));
      }
      return map;
    }

    // JtonPrimitive
    JtonPrimitive jp = jton.asJtonPrimitive();
    if (jp.isString())
      return jp.asString();
    if (jp.isNumber())
      return jp.asNumber();
    if (jp.isBoolean())
      return jp.asBoolean();

    // otherwise.. string is a good guess
    return jp.asString();
  }

  private static JtonElement snakeyamlToJton(Object o) {
    // NULL => JtonNull
    if (o == null)
      return JtonNull.INSTANCE;

    // Collection => JtonArray
    if (o instanceof Collection) {
      JtonArray array = new JtonArray();
      for (Object childObj : (Collection<?>) o) {
        array.push(snakeyamlToJton(childObj));
      }
      return array;
    }

    // Array => JsonArray
    if (o.getClass().isArray()) {
      JtonArray array = new JtonArray();

      int length = Array.getLength(array);
      for (int i = 0; i < length; i++) {
        array.push(snakeyamlToJton(Array.get(array, i)));
      }

      return array;
    }

    // Map => JtonObject
    if (o instanceof Map) {
      Map<?, ?> map = (Map<?, ?>) o;

      JtonObject jsonObject = new JtonObject();
      for (final Map.Entry<?, ?> entry : map.entrySet()) {
        final String name = String.valueOf(entry.getKey());
        final Object value = entry.getValue();
        jsonObject.set(name, snakeyamlToJton(value));
      }

      return jsonObject;
    }

    // everything else => JtonPrimitive
    if (o instanceof String)
      return new JtonPrimitive((String) o);
    if (o instanceof Number)
      return new JtonPrimitive((Number) o);
    if (o instanceof Character)
      return new JtonPrimitive((Character) o);
    if (o instanceof Boolean)
      return new JtonPrimitive((Boolean) o);

    // otherwise.. string is a good guess
    return new JtonPrimitive(String.valueOf(o));
  }
}
