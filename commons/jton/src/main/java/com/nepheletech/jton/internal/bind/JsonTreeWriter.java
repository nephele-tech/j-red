/*
 * Copyright (C) 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nepheletech.jton.internal.bind;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import com.nepheletech.jton.JtonArray;
import com.nepheletech.jton.JtonElement;
import com.nepheletech.jton.JtonNull;
import com.nepheletech.jton.JtonObject;
import com.nepheletech.jton.JtonPrimitive;
import com.nepheletech.jton.stream.JsonWriter;

/**
 * This writer creates a JsonElement.
 */
public final class JsonTreeWriter extends JsonWriter {
  private static final Writer UNWRITABLE_WRITER = new Writer() {
    @Override public void write(char[] buffer, int offset, int counter) {
      throw new AssertionError();
    }
    @Override public void flush() throws IOException {
      throw new AssertionError();
    }
    @Override public void close() throws IOException {
      throw new AssertionError();
    }
  };
  /** Added to the top of the stack when this writer is closed to cause following ops to fail. */
  private static final JtonPrimitive SENTINEL_CLOSED = new JtonPrimitive("closed");

  /** The JsonElements and JsonArrays under modification, outermost to innermost. */
  private final List<JtonElement> stack = new ArrayList<JtonElement>();

  /** The name for the next JSON object value. If non-null, the top of the stack is a JsonObject. */
  private String pendingName;

  /** the JSON element constructed by this writer. */
  private JtonElement product = JtonNull.INSTANCE; // TODO: is this really what we want?;

  public JsonTreeWriter() {
    super(UNWRITABLE_WRITER);
  }

  /**
   * Returns the top level object produced by this writer.
   */
  public JtonElement get() {
    if (!stack.isEmpty()) {
      throw new IllegalStateException("Expected one JSON element but was " + stack);
    }
    return product;
  }

  private JtonElement peek() {
    return stack.get(stack.size() - 1);
  }

  private void put(JtonElement value) {
    if (pendingName != null) {
      if (!value.isJtonNull() || getSerializeNulls()) {
        JtonObject object = (JtonObject) peek();
        object.set(pendingName, value);
      }
      pendingName = null;
    } else if (stack.isEmpty()) {
      product = value;
    } else {
      JtonElement element = peek();
      if (element instanceof JtonArray) {
        ((JtonArray) element).push(value);
      } else {
        throw new IllegalStateException();
      }
    }
  }

  @Override public JsonWriter beginArray() throws IOException {
    JtonArray array = new JtonArray();
    put(array);
    stack.add(array);
    return this;
  }

  @Override public JsonWriter endArray() throws IOException {
    if (stack.isEmpty() || pendingName != null) {
      throw new IllegalStateException();
    }
    JtonElement element = peek();
    if (element instanceof JtonArray) {
      stack.remove(stack.size() - 1);
      return this;
    }
    throw new IllegalStateException();
  }

  @Override public JsonWriter beginObject() throws IOException {
    JtonObject object = new JtonObject();
    put(object);
    stack.add(object);
    return this;
  }

  @Override public JsonWriter endObject() throws IOException {
    if (stack.isEmpty() || pendingName != null) {
      throw new IllegalStateException();
    }
    JtonElement element = peek();
    if (element instanceof JtonObject) {
      stack.remove(stack.size() - 1);
      return this;
    }
    throw new IllegalStateException();
  }

  @Override public JsonWriter name(String name) throws IOException {
    if (stack.isEmpty() || pendingName != null) {
      throw new IllegalStateException();
    }
    JtonElement element = peek();
    if (element instanceof JtonObject) {
      pendingName = name;
      return this;
    }
    throw new IllegalStateException();
  }

  @Override public JsonWriter value(String value) throws IOException {
    if (value == null) {
      return nullValue();
    }
    put(new JtonPrimitive(value));
    return this;
  }

  @Override public JsonWriter nullValue() throws IOException {
    put(JtonNull.INSTANCE);
    return this;
  }

  @Override public JsonWriter value(boolean value) throws IOException {
    put(new JtonPrimitive(value));
    return this;
  }

  @Override public JsonWriter value(Boolean value) throws IOException {
    if (value == null) {
      return nullValue();
    }
    put(new JtonPrimitive(value));
    return this;
  }

  @Override public JsonWriter value(double value) throws IOException {
    if (!isLenient() && (Double.isNaN(value) || Double.isInfinite(value))) {
      throw new IllegalArgumentException("JSON forbids NaN and infinities: " + value);
    }
    put(new JtonPrimitive(value));
    return this;
  }

  @Override public JsonWriter value(long value) throws IOException {
    put(new JtonPrimitive(value));
    return this;
  }

  @Override public JsonWriter value(Number value) throws IOException {
    if (value == null) {
      return nullValue();
    }

    if (!isLenient()) {
      double d = value.doubleValue();
      if (Double.isNaN(d) || Double.isInfinite(d)) {
        throw new IllegalArgumentException("JSON forbids NaN and infinities: " + value);
      }
    }

    put(new JtonPrimitive(value));
    return this;
  }

  @Override public void flush() throws IOException {
  }

  @Override public void close() throws IOException {
    if (!stack.isEmpty()) {
      throw new IOException("Incomplete document");
    }
    stack.add(SENTINEL_CLOSED);
  }
}
