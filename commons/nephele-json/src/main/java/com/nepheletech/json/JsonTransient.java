package com.nepheletech.json;

/**
 * 
 */
public class JsonTransient extends AbstractJsonPrimitive {

  /**
   * Create a primitive using the specified Object. It must be an instance of
   * {@link Number}, a Java primitive type, or a String.
   *
   * @param primitive the value to create the primitive with.
   */
  public JsonTransient(Object primitive) {
    setValue(primitive, true);
  }

  /**
   * Returns the same value as primitives are immutable.
   * 
   * @since 2.8.2
   */
  @Override
  public JsonTransient deepCopy() {
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
