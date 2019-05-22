package com.nepheletech.jton;

import java.util.ArrayList;
import java.util.List;

import com.nepheletech.jton.JtonArray;
import com.nepheletech.jton.JtonElement;
import com.nepheletech.jton.JtonNull;
import com.nepheletech.jton.JtonObject;

public final class JsonUtil {

  /**
   * Gets a property of an object.
   * 
   * @param msg  the object
   * @param expr the property expression
   * @return the object property, or undefined if it does not exist
   */
  public static JtonElement getProperty(JtonObject msg, String expr) {
    if (msg == null) { throw new IllegalArgumentException("'root' is null"); }
    if (expr == null) { throw new IllegalArgumentException("'expr' is null"); }
    return getProperty(msg, parsePath(expr));
  }

  public static JtonElement getProperty(JtonObject root, List<String> path) {
    JtonElement e = root;

    for (int i = 0, n = path.size(); i < n; i++) {
      String prop = path.get(i);

      if (e.isJtonObject()) {
        e = e.asJtonObject().get(prop);
      } else if (e.isJtonArray() && prop.startsWith("[")) {
        try {
          e = e.asJtonArray().get(Integer.parseInt(prop.substring(1).trim()));
        } catch (IndexOutOfBoundsException ex) {
          return JtonNull.INSTANCE;
        }
      } else {
        return JtonNull.INSTANCE;
      }
    }

    return e;
  }

  /**
   * Set a property of an object.
   * 
   * @param msg           the object
   * @param prop          the property expression
   * @param value         the value to set
   * @param createMissing whether to create parent properties
   */
  public static void setProperty(final JtonObject msg, String prop, JtonElement value, boolean createMissing) {
    if (msg == null) { throw new IllegalArgumentException("'msg' is null"); }
    if (prop == null) { throw new IllegalArgumentException("'prop' is null"); }
    setObjectProperty(msg, parsePath(prop), value, createMissing);
  }

  public static void setObjectProperty(JtonObject msg, List<String> path, JtonElement value, boolean createMissing) {
    JtonElement e = msg;

    final String lastPath = path.get(path.size() - 1);

    for (int i = 0, n = path.size() - 1; i < n; i++) {
      String prop = path.get(i);

      JtonElement parent = e;

      if (e.isJtonObject()) {
        e = e.asJtonObject().get(prop);
      } else if (e.isJtonArray()) {
        if (prop.startsWith("[")) {
          e = e.asJtonArray().get(Integer.parseInt(prop.substring(1).trim()));
        } else {
          throw new IllegalArgumentException("expecting array index: " + prop);
        }
      }

      if (createMissing
          && (e.isJtonNull() || e.isJtonPrimitive())) {
        final String nextProp = path.get(i + 1);
        if (parent.isJtonObject()) {
          if (nextProp.startsWith("[")) {
            parent.asJtonObject().set(prop, e = new JtonArray());
          } else {
            parent.asJtonObject().set(prop, e = new JtonObject());
          }
        } else if (parent.isJtonArray()) {
          int index = Integer.parseInt(prop.substring(1).trim());
          if (nextProp.startsWith("[")) {
            parent.asJtonArray().set(index, e = new JtonArray());
          } else {
            parent.asJtonArray().set(index, e = new JtonObject());
          }
        }
      }
    }

    if (e.isJtonObject()) {
      e.asJtonObject().set(lastPath, value);
    } else if (e.isJtonArray()) {
      if (lastPath.startsWith("[")) {
        final JtonArray array = e.asJtonArray();
        final int index = Integer.parseInt(lastPath.substring(1).trim());

        for (int idx = array.size() - 1; idx < index; idx++) {
          array.push(JtonNull.INSTANCE);
        }

        e.asJtonArray().set(index, value);
      } else {
        throw new IllegalArgumentException("expecting array index: " + lastPath);
      }
    }
  }

  /**
   * Delete a property of an object.
   * 
   * @param msg  the object
   * @param prop the property expression
   */
  public static void deleteProperty(final JtonObject msg, String prop) {
    if (msg == null) { throw new IllegalArgumentException("'msg' is null"); }
    if (prop == null) { throw new IllegalArgumentException("'prop' is null"); }
    deleteProperty(msg, parsePath(prop));
  }

  public static void deleteProperty(JtonObject msg, List<String> path) {
    JtonElement e = msg;

    final String lastPath = path.get(path.size() - 1);

    for (int i = 0, n = path.size() - 1; i < n; i++) {
      String prop = path.get(i);

      if (e.isJtonObject()) {
        e = e.asJtonObject().get(prop);
      } else if (e.isJtonArray()) {
        if (prop.startsWith("[")) {
          e = e.asJtonArray().get(Integer.parseInt(prop.substring(1).trim()));
        } else {
          throw new IllegalArgumentException("expecting array index: " + prop);
        }
      }

      if (e.isJtonNull() || e.isJtonPrimitive()) { return; }
    }

    if (e.isJtonObject()) {
      e.asJtonObject().remove(lastPath);
    } else if (e.isJtonArray()) {
      if (lastPath.startsWith("[")) {
        final JtonArray array = e.asJtonArray();
        final int index = Integer.parseInt(lastPath.substring(1).trim());

        for (int idx = array.size() - 1; idx < index; idx++) {
          array.push(JtonNull.INSTANCE);
        }

        e.asJtonArray().remove(index);
      } else {
        throw new IllegalArgumentException("expecting array index: " + lastPath);
      }
    }
  }

  public static List<String> parsePath(String path) {
    if (path == null) { throw new IllegalArgumentException("path is null."); }

    final ArrayList<String> keys = new ArrayList<String>();

    int i = 0;
    int n = path.length();

    while (i < n) {
      char c = path.charAt(i++);

      final StringBuilder identifierBuilder = new StringBuilder();

      boolean bracketed = (c == '[');
      if (bracketed && i < n) {
        identifierBuilder.append(c);

        c = path.charAt(i++);

        char quote = Character.UNASSIGNED;

        boolean quoted = (c == '"' || c == '\'');
        if (quoted && i < n) {
          quote = c;
          c = path.charAt(i++);
        }

        while (i <= n && bracketed) {
          bracketed = quoted || (c != ']');

          if (bracketed) {
            if (c == quote) {
              if (i < n) {
                c = path.charAt(i++);
                quoted = (c == quote);
              }
            }

            if (quoted || c != ']') {
              // if (Character.isISOControl(c)) {
              // throw new IllegalArgumentException("Illegal identifier character: " +
              // identifierBuilder.toString() + c);
              // }

              identifierBuilder.append(c);

              if (i < n) {
                c = path.charAt(i++);
              }
            }
          }
        }

        if (quoted) { throw new IllegalArgumentException("Unterminated quoted identifier."); }

        if (bracketed) { throw new IllegalArgumentException("Unterminated bracketed identifier."); }

        if (i < n) {
          c = path.charAt(i);

          if (c == '.') {
            i++;
          }
        }
      } else {
        while (i <= n && c != '.' && c != '[') {
          // if (!Character.isJavaIdentifierPart(c)) {
          // throw new IllegalArgumentException("Illegal identifier character: " +
          // identifierBuilder.toString() + c);
          // }

          identifierBuilder.append(c);

          if (i < n) {
            c = path.charAt(i);
          }

          i++;
        }

        if (c == '[') {
          i--;
        }
      }

      if (c == '.' && i == n) { throw new IllegalArgumentException("Path cannot end with a '.' character."); }

      if (identifierBuilder.length() == 0) { throw new IllegalArgumentException("Missing identifier."); }

      keys.add(identifierBuilder.toString().trim());
    }

    return keys;
  }

  // ---

  private JsonUtil() {}
}
