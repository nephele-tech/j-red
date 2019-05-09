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

package com.nepheletech.json;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * A class representing an array type in Json. An array is a list of
 * {@link JsonElement}s each of which can be of a different type. This is an
 * ordered list, meaning that the order in which elements are added is
 * preserved.
 *
 * @author Inderjeet Singh
 * @author Joel Leitch
 */
public final class JsonArray extends JsonElement implements List<JsonElement> {
  private final List<JsonElement> elements;

  /**
   * Creates an empty JsonArray.
   */
  public JsonArray() {
    elements = new ArrayList<JsonElement>();
  }

  public JsonArray(int capacity) {
    elements = new ArrayList<JsonElement>(capacity);
  }

  public JsonArray(Collection<? extends JsonElement> c) {
    elements = new ArrayList<JsonElement>();
    addAll(c);
  }

  /**
   * Creates a deep copy of this element and all its children
   * 
   * @since 2.8.2
   */
  @Override
  public JsonArray deepCopy() {
    if (!elements.isEmpty()) {
      JsonArray result = new JsonArray(elements.size());
      for (JsonElement element : elements) {
        result.add(element.deepCopy());
      }
      return result;
    }
    return new JsonArray();
  }

  /**
   * Adds the specified boolean to self.
   *
   * @param bool the boolean that needs to be added to the array.
   */
  public JsonArray push(Boolean bool) {
    elements.add(bool == null ? JsonNull.INSTANCE : new JsonPrimitive(bool));
    return this;
  }

  /**
   * Adds the specified character to self.
   *
   * @param character the character that needs to be added to the array.
   */
  public JsonArray push(Character character) {
    elements.add(character == null ? JsonNull.INSTANCE : new JsonPrimitive(character));
    return this;
  }

  /**
   * Adds the specified number to self.
   *
   * @param number the number that needs to be added to the array.
   */
  public JsonArray push(Number number) {
    elements.add(number == null ? JsonNull.INSTANCE : new JsonPrimitive(number));
    return this;
  }

  /**
   * Adds the specified string to self.
   *
   * @param string the string that needs to be added to the array.
   */
  public JsonArray push(String string) {
    elements.add(string == null ? JsonNull.INSTANCE : new JsonPrimitive(string));
    return this;
  }

  /**
   * Adds the specified element to self.
   *
   * @param element the element that needs to be added to the array.
   */
  public JsonArray push(JsonElement element) {
    if (element == null) {
      element = JsonNull.INSTANCE;
    }
    elements.add(element);
    return this;
  }

  /**
   * Adds all the elements of the specified array to self.
   *
   * @param array the array whose elements need to be added to the array.
   */
  public void addAll(JsonArray array) {
    elements.addAll(array.elements);
  }

  /**
   * Replaces the element at the specified position in this array with the
   * specified element. Element can be null.
   * 
   * @param index   index of the element to replace
   * @param element element to be stored at the specified position
   * @return the element previously at the specified position
   * @throws IndexOutOfBoundsException if the specified index is outside the array
   *                                   bounds
   */
  public JsonElement set(int index, JsonElement element) {
    if (elements.size() < index) {
      for (int i = elements.size(); i < index; i++) {
        elements.add(JsonNull.INSTANCE);
      }
      elements.add(element);
      return JsonNull.INSTANCE;
    } else {
      return elements.set(index, element);
    }
  }

  /**
   * Removes the first occurrence of the specified element from this array, if it
   * is present. If the array does not contain the element, it is unchanged.
   * 
   * @param element element to be removed from this array, if present
   * @return true if this array contained the specified element, false otherwise
   * @since 2.3
   */
  public boolean remove(JsonElement element) {
    return elements.remove(element);
  }

  /**
   * Removes the element at the specified position in this array. Shifts any
   * subsequent elements to the left (subtracts one from their indices). Returns
   * the element that was removed from the array.
   * 
   * @param index index the index of the element to be removed
   * @return the element previously at the specified position
   * @throws IndexOutOfBoundsException if the specified index is outside the array
   *                                   bounds
   * @since 2.3
   */
  public JsonElement remove(int index) {
    return elements.remove(index);
  }

  /**
   * Returns true if this array contains the specified element.
   * 
   * @return true if this array contains the specified element.
   * @param element whose presence in this array is to be tested
   * @since 2.3
   */
  public boolean contains(JsonElement element) {
    return elements.contains(element);
  }

  /**
   * Returns the number of elements in the array.
   *
   * @return the number of elements in the array.
   */
  public int size() {
    return elements.size();
  }

  /**
   * Returns an iterator to navigate the elements of the array. Since the array is
   * an ordered list, the iterator navigates the elements in the order they were
   * inserted.
   *
   * @return an iterator to navigate the elements of the array.
   */
  public Iterator<JsonElement> iterator() {
    return elements.iterator();
  }

  /**
   * Returns the ith element of the array.
   *
   * @param i the index of the element that is being sought.
   * @return the element present at the ith index.
   * @throws IndexOutOfBoundsException if i is negative or greater than or equal
   *                                   to the {@link #size()} of the array.
   */
  public JsonElement get(int i) {
    if (elements.size() < i) {
      return JsonNull.INSTANCE;
    } else {
      final JsonElement e = elements.get(i);
      return e != null ? e : JsonNull.INSTANCE;
    }
  }
  
  // ---

  public JsonObject getAsJsonObject(int i) {
    return get(i).asJsonObject();
  }
  
  public JsonObject getAsJsonObject(int i, JsonObject defaultValue) {
    return get(i).asJsonObject(defaultValue);
  }
  
  public JsonObject getAsJsonObject(int i, boolean create) {
    return get(i).asJsonObject(create);
  }
  
  public boolean isJsonObject(int i) {
    return get(i).isJsonObject();
  }

  public JsonArray getAsJsonArray(int i) {
    return get(i).asJsonArray();
  }

  public JsonArray getAsJsonArray(int i, JsonArray defaultValue) {
    return get(i).asJsonArray(defaultValue);
  }

  public JsonArray getAsJsonArray(int i, boolean create) {
    return get(i).asJsonArray(create);
  }

  public boolean isJsonArray(int i) {
    return get(i).isJsonArray();
  }

  public byte getAsByte(int i) {
    return get(i).asByte();
  }
  
  public String getAsString(int i) {
    return get(i).asString();
  }
  
  public String getAsString(int i, String defaultValue) {
    return get(i).asString(defaultValue);
  }
  
  // ---

  /**
   * convenience method to get this array as a {@link Number} if it contains a
   * single element.
   *
   * @return get this element as a number if it is single element array.
   * @throws ClassCastException    if the element in the array is of not a
   *                               {@link JsonPrimitive} and is not a valid
   *                               Number.
   * @throws IllegalStateException if the array has more than one element.
   */
  @Override
  public Number asNumber() {
    if (elements.size() == 1) { return elements.get(0).asNumber(); }
    throw new IllegalStateException();
  }

  /**
   * convenience method to get this array as a {@link String} if it contains a
   * single element.
   *
   * @return get this element as a String if it is single element array.
   * @throws ClassCastException    if the element in the array is of not a
   *                               {@link JsonPrimitive} and is not a valid
   *                               String.
   * @throws IllegalStateException if the array has more than one element.
   */
  @Override
  public String asString() {
    if (elements.size() == 1) { return elements.get(0).asString(); }
    throw new IllegalStateException();
  }

  /**
   * convenience method to get this array as a double if it contains a single
   * element.
   *
   * @return get this element as a double if it is single element array.
   * @throws ClassCastException    if the element in the array is of not a
   *                               {@link JsonPrimitive} and is not a valid
   *                               double.
   * @throws IllegalStateException if the array has more than one element.
   */
  @Override
  public double asDouble() {
    if (elements.size() == 1) { return elements.get(0).asDouble(); }
    throw new IllegalStateException();
  }

  /**
   * convenience method to get this array as a {@link BigDecimal} if it contains a
   * single element.
   *
   * @return get this element as a {@link BigDecimal} if it is single element
   *         array.
   * @throws ClassCastException    if the element in the array is of not a
   *                               {@link JsonPrimitive}.
   * @throws NumberFormatException if the element at index 0 is not a valid
   *                               {@link BigDecimal}.
   * @throws IllegalStateException if the array has more than one element.
   * @since 1.2
   */
  @Override
  public BigDecimal asBigDecimal() {
    if (elements.size() == 1) { return elements.get(0).asBigDecimal(); }
    throw new IllegalStateException();
  }

  /**
   * convenience method to get this array as a {@link BigInteger} if it contains a
   * single element.
   *
   * @return get this element as a {@link BigInteger} if it is single element
   *         array.
   * @throws ClassCastException    if the element in the array is of not a
   *                               {@link JsonPrimitive}.
   * @throws NumberFormatException if the element at index 0 is not a valid
   *                               {@link BigInteger}.
   * @throws IllegalStateException if the array has more than one element.
   * @since 1.2
   */
  @Override
  public BigInteger asBigInteger() {
    if (elements.size() == 1) { return elements.get(0).asBigInteger(); }
    throw new IllegalStateException();
  }

  /**
   * convenience method to get this array as a float if it contains a single
   * element.
   *
   * @return get this element as a float if it is single element array.
   * @throws ClassCastException    if the element in the array is of not a
   *                               {@link JsonPrimitive} and is not a valid float.
   * @throws IllegalStateException if the array has more than one element.
   */
  @Override
  public float asFloat() {
    if (elements.size() == 1) { return elements.get(0).asFloat(); }
    throw new IllegalStateException();
  }

  /**
   * convenience method to get this array as a long if it contains a single
   * element.
   *
   * @return get this element as a long if it is single element array.
   * @throws ClassCastException    if the element in the array is of not a
   *                               {@link JsonPrimitive} and is not a valid long.
   * @throws IllegalStateException if the array has more than one element.
   */
  @Override
  public long asLong() {
    if (elements.size() == 1) { return elements.get(0).asLong(); }
    throw new IllegalStateException();
  }

  /**
   * convenience method to get this array as an integer if it contains a single
   * element.
   *
   * @return get this element as an integer if it is single element array.
   * @throws ClassCastException    if the element in the array is of not a
   *                               {@link JsonPrimitive} and is not a valid
   *                               integer.
   * @throws IllegalStateException if the array has more than one element.
   */
  @Override
  public int asInt() {
    if (elements.size() == 1) { return elements.get(0).asInt(); }
    throw new IllegalStateException();
  }

  @Override
  public byte asByte() {
    if (elements.size() == 1) { return elements.get(0).asByte(); }
    throw new IllegalStateException();
  }

  @Override
  public char asCharacter() {
    if (elements.size() == 1) { return elements.get(0).asCharacter(); }
    throw new IllegalStateException();
  }

  /**
   * convenience method to get this array as a primitive short if it contains a
   * single element.
   *
   * @return get this element as a primitive short if it is single element array.
   * @throws ClassCastException    if the element in the array is of not a
   *                               {@link JsonPrimitive} and is not a valid short.
   * @throws IllegalStateException if the array has more than one element.
   */
  @Override
  public short asShort() {
    if (elements.size() == 1) { return elements.get(0).asShort(); }
    throw new IllegalStateException();
  }

  /**
   * convenience method to get this array as a boolean if it contains a single
   * element.
   *
   * @return get this element as a boolean if it is single element array.
   * @throws ClassCastException    if the element in the array is of not a
   *                               {@link JsonPrimitive} and is not a valid
   *                               boolean.
   * @throws IllegalStateException if the array has more than one element.
   */
  @Override
  public boolean asBoolean() {
    if (elements.size() == 1) { return elements.get(0).asBoolean(); }
    throw new IllegalStateException();
  }

  public Date asDate() {
    if (elements.size() == 1) { return elements.get(0).asDate(); }
    throw new IllegalStateException();
  }

  public java.sql.Date asSqlDate() {
    if (elements.size() == 1) { return elements.get(0).asSqlDate(); }
    throw new IllegalStateException();
  }

  public java.sql.Time asSqlTime() {
    if (elements.size() == 1) { return elements.get(0).asSqlTime(); }
    throw new IllegalStateException();
  }

  public java.sql.Timestamp asSqlTimestamp() {
    if (elements.size() == 1) { return elements.get(0).asSqlTimestamp(); }
    throw new IllegalStateException();
  }

  @Override
  public boolean equals(Object o) {
    return (o == this) || (o instanceof JsonArray && ((JsonArray) o).elements.equals(elements));
  }

  @Override
  public int hashCode() {
    return elements.hashCode();
  }

  // -----------------------------------------------------------------------

  @Override
  public boolean isEmpty() { return elements.isEmpty(); }

  @Deprecated
  @Override
  public boolean contains(Object o) {
    return elements.contains(o);
  }

  @Override
  public Object[] toArray() {
    return elements.toArray();
  }

  @Override
  public <T> T[] toArray(T[] a) {
    return elements.toArray(a);
  }

  @Deprecated
  @Override
  public boolean add(JsonElement e) {
    if (e == null) {
      e = JsonNull.INSTANCE;
    }
    return elements.add(e);
  }

  @Deprecated
  @Override
  public boolean remove(Object o) {
    return elements.remove(o);
  }

  @Override
  public boolean containsAll(Collection<?> c) {
    return elements.containsAll(c);
  }

  @Override
  public boolean addAll(Collection<? extends JsonElement> c) {
    return elements.addAll(c);
  }

  @Override
  public boolean addAll(int index, Collection<? extends JsonElement> c) {
    return elements.addAll(index, c);
  }

  @Override
  public boolean removeAll(Collection<?> c) {
    return elements.removeAll(c);
  }

  @Override
  public boolean retainAll(Collection<?> c) {
    return elements.retainAll(c);
  }

  @Override
  public void clear() {
    elements.clear();
  }

  @Override
  public void add(int index, JsonElement element) {
    if (elements.size() < index) {
      for (int i = elements.size(); i < index; i++) {
        elements.add(JsonNull.INSTANCE);
      }
      elements.add(element);
    } else {
      elements.add(index, element);
    }
  }

  @Deprecated
  @Override
  public int indexOf(Object o) {
    return elements.indexOf(o);
  }

  public int indexOf(JsonElement e) {
    return elements.indexOf(e);
  }

  @Deprecated
  @Override
  public int lastIndexOf(Object o) {
    return elements.lastIndexOf(o);
  }

  public int lastIndexOf(JsonElement e) {
    return elements.lastIndexOf(e);
  }

  @Override
  public ListIterator<JsonElement> listIterator() {
    return elements.listIterator();
  }

  @Override
  public ListIterator<JsonElement> listIterator(int index) {
    return elements.listIterator(index);
  }

  @Override
  public List<JsonElement> subList(int fromIndex, int toIndex) {
    return elements.subList(fromIndex, toIndex);
  }

  // ---

  public JsonArray concat(JsonElement element) {
    return concat(this, element);
  }

  /**
   * The {@code concat} method is used to merge two or more arrays.
   * 
   * @param elements Arrays and/or values to concatenate into a new array.
   * @return A new array instance.
   */
  public static JsonArray concat(JsonElement... elements) {
    final JsonArray concat = new JsonArray();
    for (JsonElement e : elements) {
      if (e.isJsonArray()) {
        concat.addAll(e.asJsonArray());
      } else {
        concat.push(e);
      }
    }
    return concat;
  }
}
