package com.nepheletech.jton;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import com.google.gson.internal.$Gson$Preconditions;

import javax.xml.bind.DatatypeConverter;

import com.google.gson.internal.LazilyParsedNumber;

/**
 * A class representing a Json primitive value. A primitive value is either a
 * String, a Java primitive, or a Java primitive wrapper type.
 */
public final class JtonPrimitive extends JtonElement {

  private static final Class<?>[] PRIMITIVE_TYPES = { int.class, long.class, short.class,
      float.class, double.class, byte.class, byte[].class, boolean.class, char.class,
      Number.class, Boolean.class, Character.class, Date.class };

  private Object value;

  private boolean jtonTransient = false;

  /**
   * Create a primitive containing a boolean value.
   *
   * @param bool the value to create the primitive with.
   */
  public JtonPrimitive(Boolean bool) {
    value = $Gson$Preconditions.checkNotNull(bool);
  }

  /**
   * Create a primitive containing a {@link Number}.
   *
   * @param number the value to create the primitive with.
   */
  public JtonPrimitive(Number number) {
    value = $Gson$Preconditions.checkNotNull(number);
  }

  /**
   * Create a primitive containing a String value.
   *
   * @param string the value to create the primitive with.
   */
  public JtonPrimitive(String string) {
    value = $Gson$Preconditions.checkNotNull(string);
  }

  /**
   * Create a primitive containing a character. The character is turned into a one
   * character String since Jton only supports String.
   *
   * @param c the value to create the primitive with.
   */
  public JtonPrimitive(Character c) {
    // convert characters to string since in JSON, characters are represented as a
    // single character string
    value = $Gson$Preconditions.checkNotNull(c).toString();
  }

  /**
   * Creates a primitive containing a date value.
   * 
   * @param date the value to create the primitive with.
   */
  public JtonPrimitive(Date date) {
    value = $Gson$Preconditions.checkNotNull(date);
  }

  /**
   * Creates a primitive containing a bytes value.
   * 
   * @param bytes the value to create the primitive with.
   */
  public JtonPrimitive(byte[] bytes) {
    value = $Gson$Preconditions.checkNotNull(bytes);
  }

  /**
   * Create a primitive using the specified Object.
   *
   * @param primitive     the value to create the primitive with.
   * @param jtonTransient
   */
  public JtonPrimitive(Object primitive, boolean jtonTransient) {
    if (jtonTransient) {
      this.value = primitive;
      this.jtonTransient = true;
    } else {
      $Gson$Preconditions.checkNotNull(primitive);
      $Gson$Preconditions.checkArgument(isPrimitiveOrString(primitive));
      
      if (primitive instanceof Character) {
        // convert characters to strings since in JSON, characters are represented as a
        // single character string
        this.value = primitive.toString();
      } else {
        this.value = primitive;
      }
    }
  }

  /**
   * Returns the same value as primitives are immutable.
   */
  @Override
  public JtonPrimitive deepCopy() {
    return this;
  }

  public boolean isJtonTransient() { return jtonTransient; }

  @SuppressWarnings("unchecked")
  public <T> T getValue() { return (T) value; }

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
    if (isNumber()) {
      return (Number) value;
    } else if (isBoolean()) {
      return ((Boolean) value) ? 1 : 0;
    } else if (value instanceof Date) {
      return ((Date) value).getTime();
    } else if (value instanceof byte[]) {
      throw new NumberFormatException("Can't convert byte array to Number");
    } else {
      return new LazilyParsedNumber(value.toString());
    }
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
    if (value instanceof BigInteger) {
      return (BigInteger) value;
    } else if (value instanceof LazilyParsedNumber) {
      return new BigInteger(((LazilyParsedNumber) value).toString());
    } else if (value instanceof Number) {
      return asBigDecimal().toBigInteger();
    } else if (value instanceof Boolean) {
      return ((Boolean) value) ? BigInteger.ONE : BigInteger.ZERO;
    } else if (value instanceof Date) {
      return BigInteger.valueOf(((Date) value).getTime());
    } else {
      return new BigInteger(value.toString());
    }
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
    if (value instanceof BigDecimal) {
      return (BigDecimal) value;
    } else if (value instanceof BigInteger) {
      return new BigDecimal((BigInteger) value);
    } else if (value instanceof Byte) {
      return new BigDecimal((Byte) value);
    } else if (value instanceof Short) {
      return new BigDecimal((Short) value);
    } else if (value instanceof Integer) {
      return new BigDecimal((Integer) value);
    } else if (value instanceof Long) {
      return new BigDecimal((Long) value);
    } else if (value instanceof Float) {
      return new BigDecimal((Float) value);
    } else if (value instanceof Double) {
      return new BigDecimal((Double) value);
    } else if (value instanceof LazilyParsedNumber) {
      return new BigDecimal(((LazilyParsedNumber) value).toString());
    } else if (value instanceof Boolean) {
      return ((Boolean) value) ? BigDecimal.ONE : BigDecimal.ZERO;
    } else if (value instanceof Date) {
      return new BigDecimal(((Date) value).getTime());
    } else if (value instanceof byte[]) {
      throw new NumberFormatException("Can't convert byte array to BigDecimal");
    } else {
      return new BigDecimal(value.toString());
    }
  }

  /**
   * convenience method to get this element as a primitive byte.
   *
   * @return get this element as a primitive byte.
   * @throws NumberFormatException if the value contained is not a valid byte
   *                               value.
   */
  @Override
  public byte asByte() {
    return isNumber() ? ((Number) value).byteValue() : asNumber().byteValue();
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
    return isNumber() ? ((Number) value).shortValue() : asNumber().shortValue();
  }

  /**
   * convenience method to get this element as a primitive integer.
   *
   * @return get this element as a primitive integer.
   * @throws NumberFormatException if the value contained is not a valid integer
   *                               value.
   */
  @Override
  public int asInt() {
    return isNumber() ? ((Number) value).intValue() : asNumber().intValue();
  }

  /**
   * convenience method to get this element as a primitive long.
   *
   * @return get this element as a primitive long.
   * @throws NumberFormatException if the value contained is not a valid long
   *                               value.
   */
  @Override
  public long asLong() {
    return isNumber() ? ((Number) value).longValue() : asNumber().longValue();
  }

  /**
   * convenience method to get this element as a primitive float.
   *
   * @return get this element as a primitive float.
   * @throws NumberFormatException if the value contained is not a valid float
   *                               value.
   */
  @Override
  public float asFloat() {
    return isNumber() ? ((Number) value).floatValue() : asNumber().floatValue();
  }

  /**
   * convenience method to get this element as a primitive double.
   *
   * @return get this element as a primitive double.
   * @throws NumberFormatException if the value contained is not a valid double.
   */
  @Override
  public double asDouble() {
    return isNumber() ? ((Number) value).doubleValue() : asNumber().doubleValue();
  }

  // --

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
    return value instanceof byte[] ? (byte[]) value : Base64.getDecoder().decode(asString());
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
    JtonPrimitive other = (JtonPrimitive) obj;
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
  private static boolean isIntegral(JtonPrimitive primitive) {
    if (primitive.value instanceof Number) {
      Number number = (Number) primitive.value;
      return number instanceof BigInteger || number instanceof Long || number instanceof Integer
          || number instanceof Short || number instanceof Byte;
    }
    return false;
  }
}
