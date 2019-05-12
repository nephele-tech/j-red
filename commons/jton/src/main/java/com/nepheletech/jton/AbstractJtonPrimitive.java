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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import javax.xml.bind.DatatypeConverter;

import com.nepheletech.jton.AbstractJtonPrimitive;
import com.nepheletech.jton.JtonElement;
import com.nepheletech.jton.JtonPrimitive;
import com.nepheletech.jton.internal.$Gson$Preconditions;
import com.nepheletech.jton.internal.LazilyParsedNumber;

/**
 * A class representing a Json primitive value. A primitive value is either a
 * String, a Java primitive, or a Java primitive wrapper type.
 *
 * @author Inderjeet Singh
 * @author Joel Leitch
 */
public abstract class AbstractJtonPrimitive extends JtonElement {

  private static final Class<?>[] PRIMITIVE_TYPES = { int.class, long.class, short.class,
      float.class, double.class, byte.class, byte[].class, boolean.class, char.class, Integer.class, Long.class,
      Short.class, Float.class, Double.class, Byte.class, Boolean.class, Character.class, Date.class };

  private Object value;

  public Object getValue() { return value; }

  protected void setValue(Object primitive) {
    setValue(primitive, false);
  }

  protected void setValue(Object primitive, boolean jsonTransient) {
    if (primitive instanceof Character) {
      // convert characters to strings since in JSON, characters are represented as a
      // single
      // character string
      char c = ((Character) primitive).charValue();
      this.value = String.valueOf(c);
    } else {
      if (!jsonTransient) {
        $Gson$Preconditions.checkArgument(primitive instanceof Number
            || isPrimitiveOrString(primitive));
      }
      this.value = primitive;
    }
  }

  /**
   * Check whether this primitive contains a boolean value.
   *
   * @return true if this primitive contains a boolean value, false otherwise.
   */
  public boolean isBoolean() { return value instanceof Boolean; }

  /**
   * convenience method to get this element as a boolean value.
   *
   * @return get this element as a primitive boolean value.
   */
  @Override
  public boolean asBoolean() {
    if (isBoolean()) {
      return (Boolean) value;
    } else {
      // Check to see if the value as a String is "true" in any case.
      return Boolean.parseBoolean(asString());
    }
  }

  /**
   * Check whether this primitive contains a Number.
   *
   * @return true if this primitive contains a Number, false otherwise.
   */
  public boolean isNumber() { return value instanceof Number; }

  /**
   * convenience method to get this element as a Number.
   *
   * @return get this element as a Number.
   * @throws NumberFormatException if the value contained is not a valid Number.
   */
  @Override
  public Number asNumber() {
    return value instanceof String ? new LazilyParsedNumber((String) value) : (Number) value;
  }

  /**
   * Check whether this primitive contains a String value.
   *
   * @return true if this primitive contains a String value, false otherwise.
   */
  public boolean isString() { return value instanceof String; }

  /**
   * convenience method to get this element as a String.
   *
   * @return get this element as a String.
   */
  @Override
  public String asString() {
    if (isNumber()) {
      return asNumber().toString();
    } else if (isBoolean()) {
      return ((Boolean) value).toString();
    } else if (isString()) { // ggeorg
      return (String) value;
    } else if (value instanceof Date) { // ggeorg
      final Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
      c.setTime(asDate());
      if (value instanceof java.sql.Date) {
        return DatatypeConverter.printDate(c);
      } else if (value instanceof java.sql.Time) {
        return DatatypeConverter.printTime(c);
      } else {
        return DatatypeConverter.printDateTime(c);
      }
    } else if (value instanceof byte[]) {
      return Base64.getEncoder().encodeToString((byte[]) value);
    } else { // ggeorg
      return null;
    }
  }

  /**
   * convenience method to get this element as a primitive double.
   *
   * @return get this element as a primitive double.
   * @throws NumberFormatException if the value contained is not a valid double.
   */
  @Override
  public double asDouble() {
    return isNumber() ? asNumber().doubleValue() : Double.parseDouble(asString());
  }

  /**
   * convenience method to get this element as a {@link BigDecimal}.
   *
   * @return get this element as a {@link BigDecimal}.
   * @throws NumberFormatException if the value contained is not a valid
   *                               {@link BigDecimal}.
   */
  @Override
  public BigDecimal asBigDecimal() {
    return value instanceof BigDecimal ? (BigDecimal) value : new BigDecimal(value.toString());
  }

  /**
   * convenience method to get this element as a {@link BigInteger}.
   *
   * @return get this element as a {@link BigInteger}.
   * @throws NumberFormatException if the value contained is not a valid
   *                               {@link BigInteger}.
   */
  @Override
  public BigInteger asBigInteger() {
    return value instanceof BigInteger ? (BigInteger) value : new BigInteger(value.toString());
  }

  /**
   * convenience method to get this element as a float.
   *
   * @return get this element as a float.
   * @throws NumberFormatException if the value contained is not a valid float.
   */
  @Override
  public float asFloat() {
    return isNumber() ? asNumber().floatValue() : Float.parseFloat(asString());
  }

  /**
   * convenience method to get this element as a primitive long.
   *
   * @return get this element as a primitive long.
   * @throws NumberFormatException if the value contained is not a valid long.
   */
  @Override
  public long asLong() {
    return isNumber() ? asNumber().longValue()
        : value instanceof Date ? ((Date) value).getTime() : Long.parseLong(asString());
  }

  /**
   * convenience method to get this element as a primitive short.
   *
   * @return get this element as a primitive short.
   * @throws NumberFormatException if the value contained is not a valid short
   *                               value.
   */
  @Override
  public short asShort() {
    return isNumber() ? asNumber().shortValue() : Short.parseShort(asString());
  }

  /**
   * convenience method to get this element as a primitive integer.
   *
   * @return get this element as a primitive integer.
   * @throws NumberFormatException if the value contained is not a valid integer.
   */
  @Override
  public int asInt() {
    return isNumber() ? asNumber().intValue() : Integer.parseInt(asString());
  }

  @Override
  public byte asByte() {
    return isNumber() ? asNumber().byteValue() : Byte.parseByte(asString());
  }

  @Override
  public char asCharacter() {
    return asString().charAt(0);
  }

  @Override
  public Date asDate() {
    return value instanceof Date ? (Date) value
        : isString() ? DatatypeConverter.parseDateTime(asString()).getTime()
            : new Date(asLong(0L));
  }

  @Override
  public java.sql.Date asSqlDate() {
    return value instanceof java.sql.Date ? (java.sql.Date) value
        : isString() ? new java.sql.Date(DatatypeConverter.parseDateTime(asString()).getTime().getTime())
            : new java.sql.Date(asLong(0L));
  }

  @Override
  public java.sql.Time asSqlTime() {
    return value instanceof java.sql.Time ? (java.sql.Time) value
        : isString() ? new java.sql.Time(DatatypeConverter.parseDateTime(asString()).getTime().getTime())
            : new java.sql.Time(asLong(0L));
  }

  @Override
  public java.sql.Timestamp asSqlTimestamp() {
    return value instanceof java.sql.Timestamp ? (java.sql.Timestamp) value
        : isString() ? new java.sql.Timestamp(DatatypeConverter.parseDateTime(asString()).getTime().getTime())
            : new java.sql.Timestamp(asLong(0L));
  }
  
  /**
   * Check whether this primitive contains a buffer (byte array).
   *
   * @return true if this primitive contains a buffer, false otherwise.
   */
  public boolean isBuffer() { return value instanceof byte[]; }

  /**
   * convenience method to get this element as a byte array.
   *
   * @return get this element as a primitive byte array.
   */
  @Override
  public byte[] asBytes() {
    return value instanceof byte[] ? (byte[]) value : 
      Base64.getDecoder().decode(asString());
  }

  private static boolean isPrimitiveOrString(Object target) {
    if (target instanceof String) { return true; }

    Class<?> classOfPrimitive = target.getClass();
    for (Class<?> standardPrimitive : PRIMITIVE_TYPES) {
      if (standardPrimitive.isAssignableFrom(classOfPrimitive)) { return true; }
    }
    return false;
  }

  @Override
  public int hashCode() {
    if (value == null) { return 31; }
    // Using recommended hashing algorithm from Effective Java for longs and doubles
    if (isIntegral(this)) {
      long value = asNumber().longValue();
      return (int) (value ^ (value >>> 32));
    }
    if (value instanceof Number) {
      long value = Double.doubleToLongBits(asNumber().doubleValue());
      return (int) (value ^ (value >>> 32));
    }
    return value.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) { return true; }
    if (obj == null || getClass() != obj.getClass()) { return false; }
    AbstractJtonPrimitive other = (JtonPrimitive) obj;
    if (value == null) { return other.value == null; }
    if (isIntegral(this) && isIntegral(other)) { return asNumber().longValue() == other.asNumber().longValue(); }
    if (value instanceof Number && other.value instanceof Number) {
      double a = asNumber().doubleValue();
      // Java standard types other than double return true for two NaN. So, need
      // special handling for double.
      double b = other.asNumber().doubleValue();
      return a == b || (Double.isNaN(a) && Double.isNaN(b));
    }
    return value.equals(other.value);
  }

  /**
   * Returns true if the specified number is an integral type (Long, Integer,
   * Short, Byte, BigInteger)
   */
  private static boolean isIntegral(AbstractJtonPrimitive primitive) {
    if (primitive.value instanceof Number) {
      Number number = (Number) primitive.value;
      return number instanceof BigInteger || number instanceof Long || number instanceof Integer
          || number instanceof Short || number instanceof Byte;
    }
    return false;
  }
}
