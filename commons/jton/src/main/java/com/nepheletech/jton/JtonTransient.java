package com.nepheletech.jton;

import com.nepheletech.jton.AbstractJsonPrimitive;
import com.nepheletech.jton.JtonTransient;

/**
 * 
 */
public class JtonTransient extends AbstractJsonPrimitive {

  /**
   * Create a primitive using the specified Object. It must be an instance of
   * {@link Number}, a Java primitive type, or a String.
   *
   * @param primitive the value to create the primitive with.
   */
  public JtonTransient(Object primitive) {
    setValue(primitive, true);
  }

  /**
   * Returns the same value as primitives are immutable.
   * 
   * @since 2.8.2
   */
  @Override
  public JtonTransient deepCopy() {
    return this;
  }
  
  /**
   * 
   */
  @Override
  public Object getValue() {
    return super.getValue();
  }
  
}
