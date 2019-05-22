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
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Set;

import com.nepheletech.jton.JtonArray;
import com.nepheletech.jton.JtonElement;
import com.nepheletech.jton.JtonNull;
import com.nepheletech.jton.JtonObject;
import com.nepheletech.jton.JtonPrimitive;
import com.nepheletech.jton.JtonTransient;
import com.nepheletech.jton.internal.LinkedTreeMap;

/**
 * A class representing an object type in Jton. An object consists of name-value
 * pairs where names are strings, and values are any other type of
 * {@link JtonElement}. This allows for a creating a tree of JtonElements. The
 * member elements of this object are maintained in order they were added.
 *
 * @author Inderjeet Singh
 * @author Joel Leitch
 */
public final class JtonObject extends JtonElement implements Map<String, JtonElement> {
  private final LinkedTreeMap<String, JtonElement> members = new LinkedTreeMap<String, JtonElement>();

  /**
   * Creates a deep copy of this element and all its children
   * 
   * @since 2.8.2
   */
  @Override
  public JtonObject deepCopy() {
    JtonObject result = new JtonObject();
    for (Map.Entry<String, JtonElement> entry : members.entrySet()) {
      result.set(entry.getKey(), entry.getValue().deepCopy());
    }
    return result;
  }

  /**
   * Adds a member, which is a name-value pair, to self. The name must be a
   * String, but the value can be an arJtonary JtonElement, thereby allowing you
   * to build a full tree of JtonElements rooted at this node.
   *
   * @param property name of the member.
   * @param value    the member object.
   */
  public JtonObject set(String property, JtonElement value) {
    if (value == null) {
      value = JtonNull.INSTANCE;
    }
    members.put(property, value);
    return this;
  }

  /**
   * Removes the {@code property} from this {@link JtonObject}.
   *
   * @param property name of the member that should be removed.
   * @return the {@link JtonElement} object that is being removed.
   * @since 1.3
   */
  public JtonElement remove(String property) {
    final JtonElement member = members.remove(property);
    return member != null ? member : JtonNull.INSTANCE;
  }

  /**
   * Convenience method to add a primitive member. The specified value is
   * converted to a JtonPrimitive of String.
   *
   * @param property name of the member.
   * @param value    the string value associated with the member.
   */
  public JtonObject set(String property, String value) {
    set(property, createJtonElement(value));
    return this;
  }

  /**
   * Convenience method to add a primitive member. The specified value is
   * converted to a JtonPrimitive of Number.
   *
   * @param property name of the member.
   * @param value    the number value associated with the member.
   */
  public JtonObject set(String property, Number value) {
    set(property, createJtonElement(value));
    return this;
  }

  /**
   * Convenience method to add a boolean member. The specified value is converted
   * to a JtonPrimitive of Boolean.
   *
   * @param property name of the member.
   * @param value    the number value associated with the member.
   */
  public JtonObject set(String property, Boolean value) {
    set(property, createJtonElement(value));
    return this;
  }

  /**
   * Convenience method to add a char member. The specified value is converted to
   * a JtonPrimitive of Character.
   *
   * @param property name of the member.
   * @param value    the number value associated with the member.
   */
  public JtonObject set(String property, Character value) {
    set(property, createJtonElement(value));
    return this;
  }

  public JtonObject set(String property, Date value) {
    set(property, createJtonElement(value));
    return this;
  }

  public JtonObject set(String property, byte[] value) {
    set(property, createJtonElement(value));
    return this;
  }

  public JtonObject set(String property, Object value, boolean jtonTransient) {
    final JtonElement e = value instanceof JtonElement 
        ? (JtonElement) value : createJtonElement(value, jtonTransient);
    return set(property, e);
  }

  /**
   * Creates the proper {@link JtonElement} object from the given {@code value}
   * object.
   *
   * @param value the object to generate the {@link JtonElement} for
   * @return a {@link JtonPrimitive} if the {@code value} is not null, otherwise a
   *         {@link JtonNull}
   */
  private JtonElement createJtonElement(Object value) {
    return createJtonElement(value, false);
  }

  private JtonElement createJtonElement(Object value, boolean jtonTransient) {
    return value == null ? JtonNull.INSTANCE : jtonTransient ? new JtonTransient(value) : new JtonPrimitive(value);
  }

  /**
   * Returns a set of members of this object. The set is ordered, and the order is
   * in which the elements were added.
   *
   * @return a set of members of this object.
   */
  @Override
  public Set<Map.Entry<String, JtonElement>> entrySet() {
    return members.entrySet();
  }

  /**
   * Returns a set of members key values.
   *
   * @return a set of member keys as Strings
   * @since 2.8.1
   */
  @Override
  public Set<String> keySet() {
    return members.keySet();
  }

  /**
   * Returns the number of key/value pairs in the object.
   *
   * @return the number of key/value pairs in the object.
   */
  @Override
  public int size() {
    return members.size();
  }

  /**
   * Convenience method to check if a member with the specified name is present in
   * this object.
   *
   * @param memberName name of the member that is being checked for presence.
   * @return true if there is a member with the specified name, false otherwise.
   */
  public boolean has(String memberName) {
    return members.containsKey(memberName);
  }

  /**
   * Returns the member with the specified name.
   *
   * @param memberName name of the member that is being requested.
   * @return the member matching the name. {@link JtonNull} if no such member
   *         exists.
   */
  public JtonElement get(String memberName) {
    final JtonElement member = members.get(memberName);
    return member != null ? member : JtonNull.INSTANCE;
  }

  /**
   * Convenience method to get the specified member as a JtonPrimitive element.
   *
   * @param memberName name of the member being requested.
   * @return the JtonPrimitive corresponding to the specified member.
   */
  public JtonPrimitive getAsJtonPrimitive(String memberName) {
    return get(memberName).asJtonPrimitive();
  }

  public JtonPrimitive getAsJtonPrimitive(String memberName, JtonPrimitive defaultValue) {
    return get(memberName).asJtonPrimitive(defaultValue);
  }

  public JtonPrimitive getAsJtonPrimitive(String memberName, Object value) {
    return get(memberName).asJtonPrimitive(value);
  }

  public boolean isJtonPrimitive(String memberName) {
    return get(memberName).isJtonPrimitive();
  }

  /**
   * Convenience method to get the specified member as a JtonTransient element.
   *
   * @param memberName name of the member being requested.
   * @return the JtonTransient corresponding to the specified member.
   */
  public JtonTransient getAsJtonTransient(String memberName) {
    return get(memberName).asJtonTransient();
  }

  public JtonTransient getAsJtonTransient(String memberName, JtonTransient defaultValue) {
    return get(memberName).asJtonTransient(defaultValue);
  }

  public JtonTransient getAsJtonTransient(String memberName, Object value) {
    return get(memberName).asJtonTransient(value);
  }

  public boolean isJtonTransient(String memberName) {
    return get(memberName).isJtonTransient();
  }

  // ---
  
  public Number getAsNumber(String memberName) {
    return getAsJtonPrimitive(memberName).asNumber();
  }

  public BigDecimal getAsBigDecimal(String memberName) {
    return getAsJtonPrimitive(memberName).asBigDecimal();
  }

  public BigInteger getAsBigInteger(String memberName) {
    return getAsJtonPrimitive(memberName).asBigInteger();
  }

  public boolean getAsBoolean(String memberName) {
    return getAsJtonPrimitive(memberName).asBoolean();
  }

  public boolean getAsBoolean(String memberName, boolean defaultValue) {
    return isJtonPrimitive(memberName)
        ? getAsJtonPrimitive(memberName).asBoolean(defaultValue)
        : defaultValue;
  }

  public byte getAsByte(String memberName) {
    return getAsJtonPrimitive(memberName).asByte();
  }

  public byte getAsByte(String memberName, final byte defaultValue) {
    return isJtonPrimitive(memberName)
        ? getAsJtonPrimitive(memberName).asByte(defaultValue)
        : defaultValue;
  }

  public char getAsCharacter(String memberName) {
    return getAsJtonPrimitive(memberName).asCharacter();
  }

  public char getAsCharacter(String memberName, final char defaultValue) {
    return isJtonPrimitive(memberName)
        ? getAsJtonPrimitive(memberName).asCharacter(defaultValue)
        : defaultValue;
  }

  public double getAsDouble(String memberName) {
    return getAsJtonPrimitive(memberName).asDouble();
  }

  public double getAsDouble(String memberName, final double defaultValue) {
    return isJtonPrimitive(memberName)
        ? getAsJtonPrimitive(memberName).asDouble(defaultValue)
        : defaultValue;
  }

  public float getAsFloat(String memberName) {
    return getAsJtonPrimitive(memberName).asFloat();
  }

  public float getAsFloat(String memberName, final float defaultValue) {
    return isJtonPrimitive(memberName)
        ? getAsJtonPrimitive(memberName).asFloat(defaultValue)
        : defaultValue;
  }

  public int getAsInt(String memberName) {
    return getAsJtonPrimitive(memberName).asInt();
  }

  public int getAsInt(String memberName, final int defaultValue) {
    return isJtonPrimitive(memberName)
        ? getAsJtonPrimitive(memberName).asInt(defaultValue)
        : defaultValue;
  }

  public String getAsString(String memberName) {
    return getAsJtonPrimitive(memberName).asString();
  }

  public String getAsString(String memberName, final String defaultValue) {
    return isJtonPrimitive(memberName)
        ? getAsJtonPrimitive(memberName).asString(defaultValue)
        : defaultValue;
  }

  public Date getAsDate(String memberName) {
    return getAsJtonPrimitive(memberName).asDate();
  }

  public Date getAsDate(String memberName, final Date defaultValue) {
    return isJtonPrimitive(memberName)
        ? getAsJtonPrimitive(memberName).asDate(defaultValue)
        : defaultValue;
  }

  public java.sql.Date getAsSqlDate(String memberName) {
    return getAsJtonPrimitive(memberName).asSqlDate();
  }

  public java.sql.Date getAsSqlDate(String memberName, final java.sql.Date defaultValue) {
    return isJtonPrimitive(memberName)
        ? getAsJtonPrimitive(memberName).asSqlDate(defaultValue)
        : defaultValue;
  }

  public java.sql.Time getAsSqlTime(String memberName) {
    return getAsJtonPrimitive(memberName).asSqlTime();
  }

  public java.sql.Time getAsSqlTime(String memberName, final java.sql.Time defaultValue) {
    return isJtonPrimitive(memberName)
        ? getAsJtonPrimitive(memberName).asSqlTime(defaultValue)
        : defaultValue;
  }

  public java.sql.Timestamp getAsSqlTimestamp(String memberName) {
    return getAsJtonPrimitive(memberName).asSqlTimestamp();
  }

  public java.sql.Timestamp getAsSqlTimestamp(String memberName, final java.sql.Timestamp defaultValue) {
    return isJtonPrimitive(memberName)
        ? getAsJtonPrimitive(memberName).asSqlTimestamp(defaultValue)
        : defaultValue;
  }

  public byte[] getAsBytes(String memberName) {
    return getAsJtonPrimitive(memberName).asBytes();
  }

  // ---

  /**
   * Convenience method to get the specified member as a JtonArray.
   *
   * @param memberName name of the member being requested.
   * @return the JtonArray corresponding to the specified member.
   */
  public JtonArray getAsJtonArray(String memberName) {
    return get(memberName).asJtonArray();
  }

  public JtonArray getAsJtonArray(String memberName, boolean create) {
    return get(memberName).asJtonArray(create);
  }

  public JtonArray getAsJtonArray(String memberName, JtonArray defaultValue) {
    return get(memberName).asJtonArray(defaultValue);
  }

  public boolean isJtonArray(String memberName) {
    return get(memberName).isJtonArray();
  }

  /**
   * Convenience method to get the specified member as a JtonObject.
   *
   * @param memberName name of the member being requested.
   * @return the JtonObject corresponding to the specified member.
   */
  public JtonObject getAsJtonObject(String memberName) {
    return get(memberName).asJtonObject();
  }

  public JtonObject getAsJtonObject(String memberName, boolean create) {
    return get(memberName).asJtonObject(create);
  }

  public JtonObject getAsJtonObject(String memberName, JtonObject defaultValue) {
    return get(memberName).asJtonObject(defaultValue);
  }

  public boolean isJtonObject(String memberName) {
    return get(memberName).isJtonObject();
  }

  @Override
  public boolean equals(Object o) {
    return (o == this) || (o instanceof JtonObject
        && ((JtonObject) o).members.equals(members));
  }

  @Override
  public int hashCode() {
    return members.hashCode();
  }

  // -----------------------------------------------------------------------
  // Map<String, JtonElement> implementation (not i GSON).
  // -----------------------------------------------------------------------

  @Override
  public boolean isEmpty() { return members.isEmpty(); }

  @Deprecated
  @Override
  public boolean containsKey(Object key) {
    return members.containsKey(key);
  }

  @Deprecated
  @Override
  public boolean containsValue(Object value) {
    return members.containsValue(value);
  }

  @Deprecated
  @Override
  public JtonElement get(Object key) {
    return get((String) key);
  }

  @Deprecated
  @Override
  public JtonElement put(String key, JtonElement value) {
    if (value == null) {
      value = JtonNull.INSTANCE;
    }
    return members.put(key, value);
  }

  @Deprecated
  @Override
  public JtonElement remove(Object key) {
    return remove(key);
  }

  @Override
  public void putAll(Map<? extends String, ? extends JtonElement> m) {
    members.putAll(m);
  }

  @Override
  public void clear() {
    members.clear();
  }

  @Override
  public Collection<JtonElement> values() {
    return members.values();
  }

  // ---

  public JtonArray keys() {
    return keys(this);
  }

  public static JtonArray keys(Map<String, ?> map) {
    final JtonArray keys = new JtonArray();
    map.keySet().forEach(keys::push);
    return keys;
  }
}
