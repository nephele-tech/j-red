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

import com.nepheletech.jton.JtonElement;
import com.nepheletech.jton.JtonNull;

/**
 * A class representing a Json {@code null} value.
 *
 * @author Inderjeet Singh
 * @author Joel Leitch
 * @since 1.2
 */
public final class JtonNull extends JtonElement {
  /**
   * singleton for JsonNull
   *
   * @since 1.8
   */
  public static final JtonNull INSTANCE = new JtonNull();

  /**
   * Creates a new JsonNull object.
   * Deprecated since Gson version 1.8. Use {@link #INSTANCE} instead
   */
  @Deprecated
  public JtonNull() {
    // Do nothing
  }

  /**
   * Returns the same instance since it is an immutable value
   * @since 2.8.2
   */
  @Override
  public JtonNull deepCopy() {
    return INSTANCE;
  }

  /**
   * All instances of JsonNull have the same hash code since they are indistinguishable
   */
  @Override
  public int hashCode() {
    return JtonNull.class.hashCode();
  }

  /**
   * All instances of JsonNull are the same
   */
  @Override
  public boolean equals(Object other) {
    return this == other || other instanceof JtonNull;
  }
}
