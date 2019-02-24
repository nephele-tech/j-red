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

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import com.nepheletech.json.internal.LinkedTreeMap;

/**
 * A class representing an object type in Json. An object consists of name-value
 * pairs where names are strings, and values are any other type of
 * {@link JsonElement}. This allows for a creating a tree of JsonElements. The
 * member elements of this object are maintained in order they were added.
 *
 * @author Inderjeet Singh
 * @author Joel Leitch
 */
public final class JsonObject extends JsonElement implements Map<String, JsonElement> {
  private final LinkedTreeMap<String, JsonElement> members = new LinkedTreeMap<String, JsonElement>();

  /**
   * Creates a deep copy of this element and all its children
   * 
   * @since 2.8.2
   */
  @Override
  public JsonObject deepCopy() {
    JsonObject result = new JsonObject();
    for (Map.Entry<String, JsonElement> entry : members.entrySet()) {
      result.set(entry.getKey(), entry.getValue().deepCopy());
    }
    return result;
  }

  /**
   * Adds a member, which is a name-value pair, to self. The name must be a
   * String, but the value can be an arbitrary JsonElement, thereby allowing you
   * to build a full tree of JsonElements rooted at this node.
   *
   * @param property name of the member.
   * @param value    the member object.
   */
  public JsonObject set(String property, JsonElement value) {
    if (value == null) {
      value = JsonNull.INSTANCE;
    }
    members.put(property, value);
    return this;
  }

  /**
   * Removes the {@code property} from this {@link JsonObject}.
   *
   * @param property name of the member that should be removed.
   * @return the {@link JsonElement} object that is being removed.
   * @since 1.3
   */
  public JsonElement remove(String property) {
    final JsonElement member =  members.remove(property);
    return member != null ? member : JsonNull.INSTANCE;
  }

  /**
   * Convenience method to add a primitive member. The specified value is
   * converted to a JsonPrimitive of String.
   *
   * @param property name of the member.
   * @param value    the string value associated with the member.
   */
  public JsonObject set(String property, String value) {
    set(property, createJsonElement(value));
    return this;
  }

  /**
   * Convenience method to add a primitive member. The specified value is
   * converted to a JsonPrimitive of Number.
   *
   * @param property name of the member.
   * @param value    the number value associated with the member.
   */
  public JsonObject set(String property, Number value) {
    set(property, createJsonElement(value));
    return this;
  }

  /**
   * Convenience method to add a boolean member. The specified value is converted
   * to a JsonPrimitive of Boolean.
   *
   * @param property name of the member.
   * @param value    the number value associated with the member.
   */
  public JsonObject set(String property, Boolean value) {
    set(property, createJsonElement(value));
    return this;
  }

  /**
   * Convenience method to add a char member. The specified value is converted to
   * a JsonPrimitive of Character.
   *
   * @param property name of the member.
   * @param value    the number value associated with the member.
   */
  public JsonObject set(String property, Character value) {
    set(property, createJsonElement(value));
    return this;
  }

  public JsonObject set(String property, Object value, boolean jsonTransient) {
    final JsonElement e = createJsonElement(value, jsonTransient);
    return set(property, e);
  }

  /**
   * Creates the proper {@link JsonElement} object from the given {@code value}
   * object.
   *
   * @param value the object to generate the {@link JsonElement} for
   * @return a {@link JsonPrimitive} if the {@code value} is not null, otherwise a
   *         {@link JsonNull}
   */
  private JsonElement createJsonElement(Object value) {
    return createJsonElement(value, false);
  }

  private JsonElement createJsonElement(Object value, boolean jsonTransient) {
    return value == null ? JsonNull.INSTANCE : 
      jsonTransient ? new JsonTransient(value) : new JsonPrimitive(value);
  }

  /**
   * Returns a set of members of this object. The set is ordered, and the order is
   * in which the elements were added.
   *
   * @return a set of members of this object.
   */
  @Override
  public Set<Map.Entry<String, JsonElement>> entrySet() {
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
   * @return the member matching the name. {@link JsonNull} if no such member exists.
   */
  public JsonElement get(String memberName) {
    final JsonElement member = members.get(memberName);
    return member != null ? member : JsonNull.INSTANCE;
  }

  /**
   * Convenience method to get the specified member as a JsonPrimitive element.
   *
   * @param memberName name of the member being requested.
   * @return the JsonPrimitive corresponding to the specified member.
   */
  public JsonPrimitive asJsonPrimitive(String memberName) {
    return (JsonPrimitive) members.get(memberName);
  }
  
  public boolean isJsonPrimitive(String memberName) {
    return get(memberName).isJsonPrimitive();
  }

  /**
   * Convenience method to get the specified member as a JsonArray.
   *
   * @param memberName name of the member being requested.
   * @return the JsonArray corresponding to the specified member.
   */
  public JsonArray asJsonArray(String memberName) {
    return (JsonArray) members.get(memberName);
  }
  
  public boolean isJsonArray(String memberName) {
    return get(memberName).isJsonArray();
  }

  /**
   * Convenience method to get the specified member as a JsonObject.
   *
   * @param memberName name of the member being requested.
   * @return the JsonObject corresponding to the specified member.
   */
  public JsonObject asJsonObject(String memberName) {
    return (JsonObject) members.get(memberName);
  }
  
  public boolean isJsonObject(String memberName) {
    return get(memberName).isJsonObject();
  }

  @Override
  public boolean equals(Object o) {
    return (o == this) || (o instanceof JsonObject
        && ((JsonObject) o).members.equals(members));
  }

  @Override
  public int hashCode() {
    return members.hashCode();
  }

  // -----------------------------------------------------------------------
  // Map<String, JsonElement> implementation (not i GSON).
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
  public JsonElement get(Object key) {
    return get((String) key);
  }

  @Deprecated
  @Override
  public JsonElement put(String key, JsonElement value) {
    if (value == null) {
      value = JsonNull.INSTANCE;
    }
    return members.put(key, value);
  }

  @Deprecated
  @Override
  public JsonElement remove(Object key) {
    return remove(key);
  }

  @Override
  public void putAll(Map<? extends String, ? extends JsonElement> m) {
    members.putAll(m);
  }

  @Override
  public void clear() {
    members.clear();
  }

  @Override
  public Collection<JsonElement> values() {
    return members.values();
  }

  // ---

  public JsonArray keys() {
    return keys(this);
  }

  public static JsonArray keys(Map<String, ?> map) {
    final JsonArray keys = new JsonArray();
    map.keySet().forEach(keys::push);
    return keys;
  }
}
