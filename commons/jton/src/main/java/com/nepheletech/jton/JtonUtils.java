package com.nepheletech.jton;

import java.util.stream.Stream;

@Deprecated
public final class JtonUtils {

  private JtonUtils() {}

  public static JtonObject getAsJtonObject(final JtonElement element, boolean empty) {
    if (element != null && element.isJtonObject()) {
      return element.asJtonObject();
    } else {
      return empty ? new JtonObject() : null;
    }
  }
  
  public static JtonObject getAsJtonObject(final JtonElement element, JtonObject fallback) {
    if (element != null && element.isJtonObject()) {
      return element.asJtonObject();
    } else {
      return fallback;
    }
  }
  
  public static JtonArray getAsJtonArray(final JtonElement element, boolean empty) {
    if (element != null && element.isJtonObject()) {
      return element.asJtonArray();
    } else {
      return empty ? new JtonArray() : null;
    }
  }
  
  public static JtonArray getAsJtonArray(final JtonElement element, JtonArray fallback) {
    if (element != null && element.isJtonObject()) {
      return element.asJtonArray();
    } else {
      return fallback;
    }
  }
  
  public static JtonPrimitive getAsJtonPrimitive(final JtonElement element, String fallback) {
    if (element != null && element.isJtonObject()) {
      return element.asJtonPrimitive();
    } else {
      return new JtonPrimitive(fallback);
    }
  }
  
  public static JtonPrimitive getAsJtonPrimitive(final JtonElement element, boolean fallback) {
    if (element != null && element.isJtonObject()) {
      return element.asJtonPrimitive();
    } else {
      return new JtonPrimitive(fallback);
    }
  }
  
  public static JtonPrimitive getAsJtonPrimitive(final JtonElement element, Number fallback) {
    if (element != null && element.isJtonObject()) {
      return element.asJtonPrimitive();
    } else {
      return new JtonPrimitive(fallback);
    }
  }
  
  public static JtonPrimitive getAsJtonPrimitive(final JtonElement element, double fallback) {
    if (element != null && element.isJtonObject()) {
      return element.asJtonPrimitive();
    } else {
      return new JtonPrimitive(fallback);
    }
  }
  
  public static JtonPrimitive getAsJtonPrimitive(final JtonElement element, float fallback) {
    if (element != null && element.isJtonObject()) {
      return element.asJtonPrimitive();
    } else {
      return new JtonPrimitive(fallback);
    }
  }
  
  public static JtonPrimitive getAsJtonPrimitive(final JtonElement element, long fallback) {
    if (element != null && element.isJtonObject()) {
      return element.asJtonPrimitive();
    } else {
      return new JtonPrimitive(fallback);
    }
  }
  
  public static JtonPrimitive getAsJtonPrimitive(final JtonElement element, int fallback) {
    if (element != null && element.isJtonObject()) {
      return element.asJtonPrimitive();
    } else {
      return new JtonPrimitive(fallback);
    }
  }
  
  public static JtonPrimitive getAsJtonPrimitive(final JtonElement element, short fallback) {
    if (element != null && element.isJtonObject()) {
      return element.asJtonPrimitive();
    } else {
      return new JtonPrimitive(fallback);
    }
  }
  
  public static JtonPrimitive getAsJtonPrimitive(final JtonElement element, byte fallback) {
    if (element != null && element.isJtonObject()) {
      return element.asJtonPrimitive();
    } else {
      return new JtonPrimitive(fallback);
    }
  }
  
  public static JtonPrimitive getAsJtonPrimitive(final JtonElement element, char fallback) {
    if (element != null && element.isJtonObject()) {
      return element.asJtonPrimitive();
    } else {
      return new JtonPrimitive(fallback);
    }
  }
  
  public static JtonPrimitive getAsJtonPrimitive(final JtonElement element, JtonPrimitive fallback) {
    if (element != null && element.isJtonPrimitive()) {
      return element.asJtonPrimitive();
    } else {
      return fallback;
    }
  }
  
  public static String getAsString(final JtonElement element, String fallback) {
    if (element != null && element.isJtonPrimitive()) {
      return element.asString();
    } else {
      return fallback;
    }
  }
  
  public static boolean getAsBoolesn(final JtonElement element, boolean fallback) {
    if (element != null && element.isJtonPrimitive()) {
      return element.asBoolean();
    } else {
      return fallback;
    }
  }
  
  public static Number getAsNumber(final JtonElement element, Number fallback) {
    if (element != null && element.isJtonPrimitive()) {
      return element.asNumber();
    } else {
      return fallback;
    }
  }
  
  public static double getAsDouble(final JtonElement element, double fallback) {
    if (element != null && element.isJtonPrimitive()) {
      return element.asDouble();
    } else {
      return fallback;
    }
  }
  
  public static float getAsFloat(final JtonElement element, float fallback) {
    if (element != null && element.isJtonPrimitive()) {
      return element.asFloat();
    } else {
      return fallback;
    }
  }
  
  public static long getAsLong(final JtonElement element, long fallback) {
    if (element != null && element.isJtonPrimitive()) {
      return element.asLong();
    } else {
      return fallback;
    }
  }
  
  public static int getAsInt(final JtonElement element, int fallback) {
    if (element != null && element.isJtonPrimitive()) {
      return element.asInt();
    } else {
      return fallback;
    }
  }
  
  public static short getAsShort(final JtonElement element, short fallback) {
    if (element != null && element.isJtonPrimitive()) {
      return element.asShort();
    } else {
      return fallback;
    }
  }
  
  public static byte getAsByte(final JtonElement element, byte fallback) {
    if (element != null && element.isJtonPrimitive()) {
      return element.asByte();
    } else {
      return fallback;
    }
  }
  
  public static char getAsByte(final JtonElement element, char fallback) {
    if (element != null && element.isJtonPrimitive()) {
      return element.asCharacter();
    } else {
      return fallback;
    }
  }

  public static JtonArray keys(JtonObject o) {
    final JtonArray result = new JtonArray();
    for (String key : o.keySet()) {
      result.push(key);
    }
    return result;
  }

  /**
   * The {@code concat} method is used to merge two or more arrays.
   * 
   * @param elements Arrays and/or values to concatenate into a new array.
   * @return A new array instance.
   */
  public static JtonArray concat(JtonElement ... elements) {
    final JtonArray result = new JtonArray();
    for (JtonElement element : elements) {
      if (element.isJtonArray()) {
        result.addAll(element.asJtonArray());
      } else {
        result.push(element);
      }
    }
    return result;
  }

  public static JtonArray toJtonArray(Stream<JtonElement> stream) {
    final JtonArray result = new JtonArray();
    stream.forEach(result::push);
    return result;
  }
}
