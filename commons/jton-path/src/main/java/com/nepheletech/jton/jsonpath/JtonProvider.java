/*
 * Copyright 2011 the original author or authors.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.nepheletech.jton.jsonpath;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.jayway.jsonpath.InvalidJsonException;
import com.jayway.jsonpath.JsonPathException;
import com.jayway.jsonpath.spi.json.AbstractJsonProvider;
import com.nepheletech.jton.JtonArray;
import com.nepheletech.jton.JtonElement;
import com.nepheletech.jton.JtonNull;
import com.nepheletech.jton.JtonObject;
import com.nepheletech.jton.JtonParser;
import com.nepheletech.jton.JtonPrimitive;

public class JtonProvider extends AbstractJsonProvider {

  public Object unwrap(final Object o) {

    if (o == null) {
      return null;
    }

    if (!(o instanceof JtonElement)) {
      return o;
    }

    JtonElement e = (JtonElement) o;

    if (e.isJtonNull()) {
      return null;
    } else if (e.isJtonPrimitive()) {
      JtonPrimitive p = e.asJtonPrimitive();
      if (p.isString()) {
        return p.asString();
      } else if (p.isBoolean()) {
        return p.asBoolean();
      } else if (p.isNumber()) {
        return unwrapNumber(p.asNumber());
      }
    }

    return o;
  }

  private static boolean isPrimitiveNumber(final Number n) {
    return n instanceof Integer ||
        n instanceof Float ||
        n instanceof Double ||
        n instanceof Long ||
        n instanceof BigDecimal ||
        n instanceof BigInteger;
  }

  private static Number unwrapNumber(final Number n) {
    Number unwrapped;

    if (!isPrimitiveNumber(n)) {
      BigDecimal bigDecimal = new BigDecimal(n.toString());
      if (bigDecimal.scale() <= 0) {
        if (bigDecimal.abs().compareTo(new BigDecimal(Integer.MAX_VALUE)) <= 0) {
          unwrapped = bigDecimal.intValue();
        } else if (bigDecimal.abs().compareTo(new BigDecimal(Long.MAX_VALUE)) <= 0) {
          unwrapped = bigDecimal.longValue();
        } else {
          unwrapped = bigDecimal;
        }
      } else {
        final double doubleValue = bigDecimal.doubleValue();
        if (BigDecimal.valueOf(doubleValue).compareTo(bigDecimal) != 0) {
          unwrapped = bigDecimal;
        } else {
          unwrapped = doubleValue;
        }
      }
    } else {
      unwrapped = n;
    }
    return unwrapped;
  }

  @Override
  public Object parse(final String json) throws InvalidJsonException {
    return JtonParser.parse(json);
  }

  @Override
  public Object parse(final InputStream jsonStream, final String charset) throws InvalidJsonException {
    try {
      return JtonParser.parse(new InputStreamReader(jsonStream, charset));
    } catch (UnsupportedEncodingException e) {
      throw new JsonPathException(e);
    }
  }

  @Override
  public String toJson(final Object obj) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Object createArray() {
    return new JtonArray();
  }

  @Override
  public Object createMap() {
    return new JtonObject();
  }

  @Override
  public boolean isArray(final Object obj) {
    return (obj instanceof JtonArray || obj instanceof List);
  }

  @Override
  public Object getArrayIndex(final Object obj, final int idx) {
    return toJtonArray(obj).get(idx);
  }

  @Override
  public void setArrayIndex(final Object array, final int index, final Object newValue) {
    if (!isArray(array)) {
      throw new UnsupportedOperationException();
    } else {
      JtonArray arr = toJtonArray(array);
      if (index == arr.size()) {
        arr.push(createJtonElement(newValue));
      } else {
        arr.set(index, createJtonElement(newValue));
      }
    }
  }

  @Override
  public Object getMapValue(final Object obj, final String key) {
    JtonObject jsonObject = toJtonObject(obj);
    Object o = jsonObject.get(key);
    if (!jsonObject.has(key)) {
      return UNDEFINED;
    } else {
      return unwrap(o);
    }
  }

  @Override
  public void setProperty(final Object obj, final Object key, final Object value) {
    if (isMap(obj)) {
      toJtonObject(obj).set(key.toString(), createJtonElement(value));
    } else {
      JtonArray array = toJtonArray(obj);
      int index;
      if (key != null) {
        index = key instanceof Integer ? (Integer) key : Integer.parseInt(key.toString());
      } else {
        index = array.size();
      }

      if (index == array.size()) {
        array.push(createJtonElement(value));
      } else {
        array.set(index, createJtonElement(value));
      }
    }
  }

  public void removeProperty(final Object obj, final Object key) {
    if (isMap(obj)) {
      toJtonObject(obj).remove(key.toString());
    } else {
      JtonArray array = toJtonArray(obj);
      int index = key instanceof Integer ? (Integer) key : Integer.parseInt(key.toString());
      array.remove(index);
    }
  }

  @Override
  public boolean isMap(final Object obj) {
    // return (obj instanceof JsonObject || obj instanceof Map);
    return (obj instanceof JtonObject);
  }

  @Override
  public Collection<String> getPropertyKeys(final Object obj) {
    List<String> keys = new ArrayList<String>();
    for (Map.Entry<String, JtonElement> entry : toJtonObject(obj).entrySet()) {
      keys.add(entry.getKey());
    }

    return keys;
  }

  @Override
  public int length(final Object obj) {
    if (isArray(obj)) {
      return toJtonArray(obj).size();
    } else if (isMap(obj)) {
      return toJtonObject(obj).entrySet().size();
    } else {
      if (obj instanceof JtonElement) {
        JtonElement element = toJtonElement(obj);
        if (element.isJtonPrimitive()) {
          return element.toString().length();
        }
      }
    }

    throw new JsonPathException("length operation can not applied to " +
        obj != null ? obj.getClass().getName() : "null");
  }

  @Override
  public Iterable<?> toIterable(final Object obj) {
    JtonArray arr = toJtonArray(obj);
    List<Object> values = new ArrayList<Object>(arr.size());
    for (Object o : arr) {
      values.add(unwrap(o));
    }

    return values;
  }

  private JtonElement createJtonElement(final Object o) {
    if (o == null)
      return JtonNull.INSTANCE;
    if (o instanceof JtonElement)
      return (JtonElement) o;
    else
      return new JtonPrimitive(o, false);
  }

  private JtonArray toJtonArray(final Object o) {
    return (JtonArray) o;
  }

  private JtonObject toJtonObject(final Object o) {
    return (JtonObject) o;
  }

  private JtonElement toJtonElement(final Object o) {
    return (JtonElement) o;
  }
}
