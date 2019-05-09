package com.nepheletech.jton;

import java.util.Date;

import com.nepheletech.jton.AbstractJsonPrimitive;
import com.nepheletech.jton.JtonPrimitive;

/**
 * 
 */
public final class JtonPrimitive extends AbstractJsonPrimitive {
  
  public static JtonPrimitive create(Object value) {
    return new JtonPrimitive(value);
  }

  /**
   * Create a primitive containing a boolean value.
   *
   * @param bool the value to create the primitive with.
   */
  public JtonPrimitive(Boolean bool) {
    setValue(bool);
  }

  /**
   * Create a primitive containing a {@link Number}.
   *
   * @param number the value to create the primitive with.
   */
  public JtonPrimitive(Number number) {
    setValue(number);
  }

  /**
   * Create a primitive containing a String value.
   *
   * @param string the value to create the primitive with.
   */
  public JtonPrimitive(String string) {
    setValue(string);
  }

  /**
   * Create a primitive containing a character. The character is turned into a one
   * character String since Json only supports String.
   *
   * @param c the value to create the primitive with.
   */
  public JtonPrimitive(Character c) {
    setValue(c);
  }

  /**
   * Creates a primitive containing a date value.
   * 
   * @param date the value to create the primitive with.
   */
  public JtonPrimitive(Date date) {
    setValue(date);
  }

  /**
   * Creates a primitive containing a bytes value.
   * 
   * @param bytes the value to create the primitive with.
   */
  public JtonPrimitive(byte[] bytes) {
    setValue(bytes);
  }

  /**
   * Create a primitive using the specified Object. It must be an instance of
   * {@link Number}, a Java primitive type, or a String.
   *
   * @param primitive the value to create the primitive with.
   */
  JtonPrimitive(Object primitive) {
    setValue(primitive, false);
  }

  /**
   * Returns the same value as primitives are immutable.
   * 
   * @since 2.8.2
   */
  @Override
  public JtonPrimitive deepCopy() {
    return this;
  }
}
