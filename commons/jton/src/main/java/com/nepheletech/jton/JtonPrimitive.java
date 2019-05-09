package com.nepheletech.json;

import java.util.Date;

/**
 * 
 */
public final class JsonPrimitive extends AbstractJsonPrimitive {
  
  public static JsonPrimitive create(Object value) {
    return new JsonPrimitive(value);
  }

  /**
   * Create a primitive containing a boolean value.
   *
   * @param bool the value to create the primitive with.
   */
  public JsonPrimitive(Boolean bool) {
    setValue(bool);
  }

  /**
   * Create a primitive containing a {@link Number}.
   *
   * @param number the value to create the primitive with.
   */
  public JsonPrimitive(Number number) {
    setValue(number);
  }

  /**
   * Create a primitive containing a String value.
   *
   * @param string the value to create the primitive with.
   */
  public JsonPrimitive(String string) {
    setValue(string);
  }

  /**
   * Create a primitive containing a character. The character is turned into a one
   * character String since Json only supports String.
   *
   * @param c the value to create the primitive with.
   */
  public JsonPrimitive(Character c) {
    setValue(c);
  }

  /**
   * Creates a primitive containing a date value.
   * 
   * @param date the value to create the primitive with.
   */
  public JsonPrimitive(Date date) {
    setValue(date);
  }

  /**
   * Creates a primitive containing a bytes value.
   * 
   * @param bytes the value to create the primitive with.
   */
  public JsonPrimitive(byte[] bytes) {
    setValue(bytes);
  }

  /**
   * Create a primitive using the specified Object. It must be an instance of
   * {@link Number}, a Java primitive type, or a String.
   *
   * @param primitive the value to create the primitive with.
   */
  JsonPrimitive(Object primitive) {
    setValue(primitive, false);
  }

  /**
   * Returns the same value as primitives are immutable.
   * 
   * @since 2.8.2
   */
  @Override
  public JsonPrimitive deepCopy() {
    return this;
  }
}
