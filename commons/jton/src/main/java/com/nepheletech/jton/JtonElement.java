/*
 * Copyright (C) 2008 Google Inc.
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

package com.nepheletech.jton;

import java.io.IOException;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;

import com.nepheletech.jton.AbstractJsonPrimitive;
import com.nepheletech.jton.JtonArray;
import com.nepheletech.jton.JtonElement;
import com.nepheletech.jton.JtonNull;
import com.nepheletech.jton.JtonObject;
import com.nepheletech.jton.JtonPrimitive;
import com.nepheletech.jton.JtonTransient;
import com.nepheletech.jton.internal.Streams;
import com.nepheletech.jton.stream.JsonWriter;

/**
 * A class representing an element of Json. It could either be a
 * {@link JtonObject}, a {@link JtonArray}, a {@link JtonPrimitive} or a
 * {@link JtonNull}.
 *
 * @author Inderjeet Singh
 * @author Joel Leitch
 */
public abstract class JtonElement {
  /**
   * Returns a deep copy of this element. Immutable elements like primitives and
   * nulls are not copied.
   * 
   * @since 2.8.2
   */
  public abstract JtonElement deepCopy();

  /**
   * provides check for verifying if this element is an array or not.
   *
   * @return true if this element is of type {@link JtonArray}, false otherwise.
   */
  public boolean isJsonArray() { return this instanceof JtonArray; }

  /**
   * provides check for verifying if this element is a Json object or not.
   *
   * @return true if this element is of type {@link JtonObject}, false otherwise.
   */
  public boolean isJsonObject() { return this instanceof JtonObject; }

  /**
   * provides check for verifying if this element is a primitive or not.
   *
   * @return true if this element is of type {@link JtonPrimitive}, false
   *         otherwise.
   */
  public boolean isJsonPrimitive() { return this instanceof AbstractJsonPrimitive; }

  /**
   * provides check for verifying if this element is a transient or not.
   *
   * @return true if this element is of type {@link JtonTransient}, false
   *         otherwise.
   */
  public boolean isJsonTransient() { return this instanceof JtonTransient; }

  /**
   * provides check for verifying if this element represents a null value or not.
   *
   * @return true if this element is of type {@link JtonNull}, false otherwise.
   * @since 1.2
   */
  public boolean isJsonNull() { return this instanceof JtonNull; }

  /**
   * convenience method to get this element as a {@link JtonObject}. If the
   * element is of some other type, a {@link IllegalStateException} will result.
   * Hence it is best to use this method after ensuring that this element is of
   * the desired type by calling {@link #isJsonObject()} first.
   *
   * @return get this element as a {@link JtonObject}.
   * @throws IllegalStateException if the element is of another type.
   */
  public JtonObject asJsonObject() {
    if (isJsonObject()) { return (JtonObject) this; }
    throw new IllegalStateException("Not a JSON Object: " + this);
  }

  public JtonObject asJsonObject(final JtonObject defaultValue) {
    return isJsonObject() ? asJsonObject() : defaultValue;
  }

  public JtonObject asJsonObject(final boolean create) {
    return isJsonObject() ? asJsonObject() : create ? new JtonObject() : null;
  }

  /**
   * convenience method to get this element as a {@link JtonArray}. If the element
   * is of some other type, a {@link IllegalStateException} will result. Hence it
   * is best to use this method after ensuring that this element is of the desired
   * type by calling {@link #isJsonArray()} first.
   *
   * @return get this element as a {@link JtonArray}.
   * @throws IllegalStateException if the element is of another type.
   */
  public JtonArray asJsonArray() {
    if (isJsonArray()) { return (JtonArray) this; }
    throw new IllegalStateException("Not a JSON Array: " + this);
  }

  public JtonArray asJsonArray(final JtonArray defaultValue) {
    return isJsonArray() ? asJsonArray() : defaultValue;
  }

  public JtonArray asJsonArray(final boolean create) {
    return isJsonArray() ? asJsonArray() : create ? new JtonArray() : null;
  }

  /**
   * convenience method to get this element as a {@link JtonPrimitive}. If the
   * element is of some other type, a {@link IllegalStateException} will result.
   * Hence it is best to use this method after ensuring that this element is of
   * the desired type by calling {@link #isJsonPrimitive()} first.
   *
   * @return get this element as a {@link JtonPrimitive}.
   * @throws IllegalStateException if the element is of another type.
   */
  public JtonPrimitive asJsonPrimitive() {
    if (isJsonPrimitive()) { return (JtonPrimitive) this; }
    throw new IllegalStateException("Not a JSON Primitive: " + this);
  }

  public JtonPrimitive asJsonPrimitive(final JtonPrimitive defaultValue) {
    return isJsonPrimitive() ? asJsonPrimitive() : defaultValue;
  }

  public JtonPrimitive asJsonPrimitive(final Object value) {
    return isJsonPrimitive() ? asJsonPrimitive() : value != null ? new JtonPrimitive(value) : null;
  }

  /**
   * convenience method to get this element as a {@link JtonTransient}. If the
   * element is of some other type, a {@link IllegalStateException} will result.
   * Hence it is best to use this method after ensuring that this element is of
   * the desired type by calling {@link #isJsonTransient()} first.
   *
   * @return get this element as a {@link JtonTransient}.
   * @throws IllegalStateException if the element is of another type.
   */
  public JtonTransient asJsonTransient() {
    if (isJsonTransient()) { return (JtonTransient) this; }
    throw new IllegalStateException("Not a JSON Transient: " + this);
  }

  public JtonTransient asJsonTransient(final JtonTransient defaultValue) {
    return isJsonTransient() ? asJsonTransient() : defaultValue;
  }

  public JtonTransient asJsonTransient(final Object value) {
    return isJsonTransient() ? asJsonTransient() : value != null ? new JtonTransient(value) : null;
  }

  /**
   * convenience method to get this element as a {@link JtonNull}. If the element
   * is of some other type, a {@link IllegalStateException} will result. Hence it
   * is best to use this method after ensuring that this element is of the desired
   * type by calling {@link #isJsonNull()} first.
   *
   * @return get this element as a {@link JtonNull}.
   * @throws IllegalStateException if the element is of another type.
   * @since 1.2
   */
  public JtonNull asJsonNull() {
    if (isJsonNull()) { return (JtonNull) this; }
    throw new IllegalStateException("Not a JSON Null: " + this);
  }

  /**
   * convenience method to get this element as a boolean value.
   *
   * @return get this element as a primitive boolean value.
   * @throws ClassCastException    if the element is of not a
   *                               {@link JtonPrimitive} and is not a valid
   *                               boolean value.
   * @throws IllegalStateException if the element is of the type {@link JtonArray}
   *                               but contains more than a single element.
   */
  public boolean asBoolean() {
    throw new UnsupportedOperationException(getClass().getSimpleName());
  }

  public boolean asBoolean(boolean defaultValue) {
    try {
      return isJsonPrimitive() ? asBoolean() : defaultValue;
    } catch (RuntimeException e) {
      return defaultValue;
    }
  }

  public Boolean asBoolean(Boolean defaultValue) {
    try {
      return isJsonPrimitive() ? asBoolean() : defaultValue;
    } catch (RuntimeException e) {
      return defaultValue;
    }
  }

  /**
   * convenience method to get this element as a {@link Number}.
   *
   * @return get this element as a {@link Number}.
   * @throws ClassCastException    if the element is of not a
   *                               {@link JtonPrimitive} and is not a valid
   *                               number.
   * @throws IllegalStateException if the element is of the type {@link JtonArray}
   *                               but contains more than a single element.
   */
  public Number asNumber() {
    throw new UnsupportedOperationException(getClass().getSimpleName());
  }

  /**
   * convenience method to get this element as a string value.
   *
   * @return get this element as a string value.
   * @throws ClassCastException    if the element is of not a
   *                               {@link JtonPrimitive} and is not a valid string
   *                               value.
   * @throws IllegalStateException if the element is of the type {@link JtonArray}
   *                               but contains more than a single element.
   */
  public String asString() {
    throw new UnsupportedOperationException(getClass().getSimpleName());
  }

  public String asString(String defaultValue) {
    try {
      final String value = isJsonPrimitive() ? asString() : null;
      return (value != null && !value.isEmpty()) ? value : defaultValue;
    } catch (RuntimeException e) {
      return defaultValue;
    }
  }

  /**
   * convenience method to get this element as a primitive double value.
   *
   * @return get this element as a primitive double value.
   * @throws ClassCastException    if the element is of not a
   *                               {@link JtonPrimitive} and is not a valid double
   *                               value.
   * @throws IllegalStateException if the element is of the type {@link JtonArray}
   *                               but contains more than a single element.
   */
  public double asDouble() {
    throw new UnsupportedOperationException(getClass().getSimpleName());
  }

  public double asDouble(final double defaultValue) {
    try {
      return isJsonPrimitive() ? asDouble() : defaultValue;
    } catch (RuntimeException e) {
      return defaultValue;
    }
  }

  public Double asDouble(final Double defaultValue) {
    try {
      return isJsonPrimitive() ? asDouble() : defaultValue;
    } catch (RuntimeException e) {
      return defaultValue;
    }
  }

  /**
   * convenience method to get this element as a primitive float value.
   *
   * @return get this element as a primitive float value.
   * @throws ClassCastException    if the element is of not a
   *                               {@link JtonPrimitive} and is not a valid float
   *                               value.
   * @throws IllegalStateException if the element is of the type {@link JtonArray}
   *                               but contains more than a single element.
   */
  public float asFloat() {
    throw new UnsupportedOperationException(getClass().getSimpleName());
  }

  public float asFloat(final float defaultValue) {
    try {
      return isJsonPrimitive() ? asFloat() : defaultValue;
    } catch (RuntimeException e) {
      return defaultValue;
    }
  }

  public Float asFloat(final Float defaultValue) {
    try {
      return isJsonPrimitive() ? asFloat() : defaultValue;
    } catch (RuntimeException e) {
      return defaultValue;
    }
  }

  /**
   * convenience method to get this element as a primitive long value.
   *
   * @return get this element as a primitive long value.
   * @throws ClassCastException    if the element is of not a
   *                               {@link JtonPrimitive} and is not a valid long
   *                               value.
   * @throws IllegalStateException if the element is of the type {@link JtonArray}
   *                               but contains more than a single element.
   */
  public long asLong() {
    throw new UnsupportedOperationException(getClass().getSimpleName());
  }

  public long asLong(long defaultValue) {
    try {
      return isJsonPrimitive() ? asLong() : defaultValue;
    } catch (RuntimeException e) {
      return defaultValue;
    }
  }

  public Long asLong(Long defaultValue) {
    try {
      return isJsonPrimitive() ? asLong() : defaultValue;
    } catch (RuntimeException e) {
      return defaultValue;
    }
  }

  /**
   * convenience method to get this element as a primitive integer value.
   *
   * @return get this element as a primitive integer value.
   * @throws ClassCastException    if the element is of not a
   *                               {@link JtonPrimitive} and is not a valid
   *                               integer value.
   * @throws IllegalStateException if the element is of the type {@link JtonArray}
   *                               but contains more than a single element.
   */
  public int asInt() {
    throw new UnsupportedOperationException(getClass().getSimpleName());
  }

  public int asInt(final int defaultValue) {
    try {
      return isJsonPrimitive() ? asInt() : defaultValue;
    } catch (RuntimeException e) {
      return defaultValue;
    }
  }

  public Integer asInt(final Integer defaultValue) {
    try {
      return isJsonPrimitive() ? asInt() : defaultValue;
    } catch (RuntimeException e) {
      return defaultValue;
    }
  }

  /**
   * convenience method to get this element as a primitive byte value.
   *
   * @return get this element as a primitive byte value.
   * @throws ClassCastException    if the element is of not a
   *                               {@link JtonPrimitive} and is not a valid byte
   *                               value.
   * @throws IllegalStateException if the element is of the type {@link JtonArray}
   *                               but contains more than a single element.
   * @since 1.3
   */
  public byte asByte() {
    throw new UnsupportedOperationException(getClass().getSimpleName());
  }

  public byte asByte(final byte defaultValue) {
    try {
      return isJsonPrimitive() ? asByte() : defaultValue;
    } catch (RuntimeException e) {
      return defaultValue;
    }
  }

  public Byte asByte(final Byte defaultValue) {
    try {
      return isJsonPrimitive() ? asByte() : defaultValue;
    } catch (RuntimeException e) {
      return defaultValue;
    }
  }

  /**
   * convenience method to get this element as a primitive character value.
   *
   * @return get this element as a primitive char value.
   * @throws ClassCastException    if the element is of not a
   *                               {@link JtonPrimitive} and is not a valid char
   *                               value.
   * @throws IllegalStateException if the element is of the type {@link JtonArray}
   *                               but contains more than a single element.
   * @since 1.3
   */
  public char asCharacter() {
    throw new UnsupportedOperationException(getClass().getSimpleName());
  }

  public char asCharacter(final char defaultValue) {
    try {
      return isJsonPrimitive() ? asCharacter() : defaultValue;
    } catch (RuntimeException e) {
      return defaultValue;
    }
  }

  public Character asCharacter(final Character defaultValue) {
    try {
      return isJsonPrimitive() ? asCharacter() : defaultValue;
    } catch (RuntimeException e) {
      return defaultValue;
    }
  }

  /**
   * convenience method to get this element as a {@link BigDecimal}.
   *
   * @return get this element as a {@link BigDecimal}.
   * @throws ClassCastException    if the element is of not a
   *                               {@link JtonPrimitive}. * @throws
   *                               NumberFormatException if the element is not a
   *                               valid {@link BigDecimal}.
   * @throws IllegalStateException if the element is of the type {@link JtonArray}
   *                               but contains more than a single element.
   * @since 1.2
   */
  public BigDecimal asBigDecimal() {
    throw new UnsupportedOperationException(getClass().getSimpleName());
  }

  public BigDecimal asBigDecimal(BigDecimal defaultValue) {
    try {
      return isJsonPrimitive() ? asBigDecimal() : defaultValue;
    } catch (RuntimeException e) {
      return defaultValue;
    }
  }

  /**
   * convenience method to get this element as a {@link BigInteger}.
   *
   * @return get this element as a {@link BigInteger}.
   * @throws ClassCastException    if the element is of not a
   *                               {@link JtonPrimitive}.
   * @throws NumberFormatException if the element is not a valid
   *                               {@link BigInteger}.
   * @throws IllegalStateException if the element is of the type {@link JtonArray}
   *                               but contains more than a single element.
   * @since 1.2
   */
  public BigInteger asBigInteger() {
    throw new UnsupportedOperationException(getClass().getSimpleName());
  }

  public BigInteger asBigInteger(BigInteger defaultValue) {
    try {
      return isJsonPrimitive() ? asBigInteger() : defaultValue;
    } catch (RuntimeException e) {
      return defaultValue;
    }
  }

  /**
   * convenience method to get this element as a primitive short value.
   *
   * @return get this element as a primitive short value.
   * @throws ClassCastException    if the element is of not a
   *                               {@link JtonPrimitive} and is not a valid short
   *                               value.
   * @throws IllegalStateException if the element is of the type {@link JtonArray}
   *                               but contains more than a single element.
   */
  public short asShort() {
    throw new UnsupportedOperationException(getClass().getSimpleName());
  }

  public short asShort(short defaultValue) {
    try {
      return isJsonPrimitive() ? asShort() : defaultValue;
    } catch (RuntimeException e) {
      return defaultValue;
    }
  }

  public Short asShort(Short defaultValue) {
    try {
      return isJsonPrimitive() ? asShort() : defaultValue;
    } catch (RuntimeException e) {
      return defaultValue;
    }
  }

  public Date asDate() {
    throw new UnsupportedOperationException(getClass().getSimpleName());
  }

  public Date asDate(Date defaultValue) {
    try {
      return isJsonPrimitive() ? asDate() : defaultValue;
    } catch (RuntimeException e) {
      return defaultValue;
    }
  }

  public java.sql.Date asSqlDate() {
    throw new UnsupportedOperationException(getClass().getSimpleName());
  }

  public java.sql.Date asSqlDate(java.sql.Date defaultValue) {
    try {
      return isJsonPrimitive() ? asSqlDate() : defaultValue;
    } catch (RuntimeException e) {
      return defaultValue;
    }
  }

  public java.sql.Time asSqlTime() {
    throw new UnsupportedOperationException(getClass().getSimpleName());
  }

  public java.sql.Time asSqlTime(java.sql.Time defaultValue) {
    try {
      return isJsonPrimitive() ? asSqlTime() : defaultValue;
    } catch (RuntimeException e) {
      return defaultValue;
    }
  }

  public java.sql.Timestamp asSqlTimestamp() {
    throw new UnsupportedOperationException(getClass().getSimpleName());
  }

  public java.sql.Timestamp asSqlTimestamp(java.sql.Timestamp defaultValue) {
    try {
      return isJsonPrimitive() ? asSqlTimestamp() : defaultValue;
    } catch (RuntimeException e) {
      return defaultValue;
    }
  }

  public byte[] asBytes() {
    throw new UnsupportedOperationException(getClass().getSimpleName());
  }

  /**
   * Returns a String representation of this element.
   */
  @Override
  public String toString() {
    return toString(null);
  }

  /**
   * Returns a String representation of this element.
   * <p>
   * Sets the indentation string to be repeated for each level of indentation in
   * the encoded document. If {@code indent.isEmpty()} the encoded document will
   * be compact. Otherwise the encoded document will be more human-readable.
   * 
   * @param indent a string containing only whitespace.
   * @return a string representation of this element.
   */
  public String toString(String intent) {
    try {
      final StringWriter stringWriter = new StringWriter();
      final JsonWriter jtonWriter = new JsonWriter(stringWriter);
      jtonWriter.setLenient(true);
      if (intent != null) {
        jtonWriter.setIndent(intent);
      }
      Streams.write(this, jtonWriter);
      return stringWriter.toString();
    } catch (IOException e) {
      throw new AssertionError(e);
    }
  }
}
