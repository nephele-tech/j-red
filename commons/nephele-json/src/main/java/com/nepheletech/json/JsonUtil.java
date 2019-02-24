package com.nepheletech.json;

import java.util.ArrayList;
import java.util.List;

public final class JsonUtil {

  /**
   * Gets a property of an object.
   * 
   * @param msg  the object
   * @param expr the property expression
   * @return the object property, or undefined if it does not exist
   */
  public static JsonElement getProperty(JsonObject msg, String expr) {
    if (msg == null) { throw new IllegalArgumentException("'root' is null"); }
    if (expr == null) { throw new IllegalArgumentException("'expr' is null"); }
    return getProperty(msg, parsePath(expr));
  }

  public static JsonElement getProperty(JsonObject root, List<String> path) {
    JsonElement e = root;

    for (int i = 0, n = path.size(); i < n; i++) {
      String prop = path.get(i);

      if (e.isJsonObject()) {
        e = e.asJsonObject().get(prop);
      } else if (e.isJsonArray() && prop.startsWith("[")) {
        try {
          e = e.asJsonArray().get(Integer.parseInt(prop.substring(1).trim()));
        } catch (IndexOutOfBoundsException ex) {
          return JsonNull.INSTANCE;
        }
      } else {
        return JsonNull.INSTANCE;
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
  public static void setProperty(final JsonObject msg, String prop, JsonElement value, boolean createMissing) {
    if (msg == null) { throw new IllegalArgumentException("'msg' is null"); }
    if (prop == null) { throw new IllegalArgumentException("'prop' is null"); }
    setObjectProperty(msg, parsePath(prop), value, createMissing);
  }

  public static void setObjectProperty(JsonObject msg, List<String> path, JsonElement value, boolean createMissing) {
    JsonElement e = msg;

    final String lastPath = path.get(path.size() - 1);

    for (int i = 0, n = path.size() - 1; i < n; i++) {
      String prop = path.get(i);

      JsonElement parent = e;

      if (e.isJsonObject()) {
        e = e.asJsonObject().get(prop);
      } else if (e.isJsonArray()) {
        if (prop.startsWith("[")) {
          e = e.asJsonArray().get(Integer.parseInt(prop.substring(1).trim()));
        } else {
          throw new IllegalArgumentException("expecting array index: " + prop);
        }
      }

      if (createMissing
          && (e.isJsonNull() || e.isJsonPrimitive())) {
        final String nextProp = path.get(i + 1);
        if (parent.isJsonObject()) {
          if (nextProp.startsWith("[")) {
            parent.asJsonObject().set(prop, e = new JsonArray());
          } else {
            parent.asJsonObject().set(prop, e = new JsonObject());
          }
        } else if (parent.isJsonArray()) {
          int index = Integer.parseInt(prop.substring(1).trim());
          if (nextProp.startsWith("[")) {
            parent.asJsonArray().set(index, e = new JsonArray());
          } else {
            parent.asJsonArray().set(index, e = new JsonObject());
          }
        }
      }
    }

    if (e.isJsonObject()) {
      e.asJsonObject().set(lastPath, value);
    } else if (e.isJsonArray()) {
      if (lastPath.startsWith("[")) {
        final JsonArray array = e.asJsonArray();
        final int index = Integer.parseInt(lastPath.substring(1).trim());

        for (int idx = array.size() - 1; idx < index; idx++) {
          array.push(JsonNull.INSTANCE);
        }
        
        e.asJsonArray().set(index, value);
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
