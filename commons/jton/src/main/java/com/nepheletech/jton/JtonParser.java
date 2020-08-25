package com.nepheletech.jton;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import com.google.gson.JsonIOException;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.MalformedJsonException;
import com.nepheletech.jton.internal.Streams;

/**
 * A parser to parse JSON into a parse tree of {@link JtonElement}s
 */
public final class JtonParser {

  private JtonParser() {
    // Do nothing
  }

  /**
   * Parses the specified JSON string into a parse tree
   *
   * @param json JSON text
   * @return a parse tree of {@link JtonElement}s corresponding to the specified
   *         JSON
   * @throws JsonParseException if the specified text is not valid JSON
   */
  public static JtonElement parse(String json) {
    return parse(new StringReader(json));
  }

  /**
   * Parses the specified JSON string into a parse tree
   *
   * @param reader JSON text
   * @return a parse tree of {@link JtonElement}s corresponding to the specified
   *         JSON
   * @throws JsonParseException if the specified text is not valid JSON
   */
  public static JtonElement parse(Reader reader) throws JsonIOException, JsonSyntaxException {
    try {
      JsonReader jsonReader = new JsonReader(reader);
      JtonElement element = parse(jsonReader);
      if (!element.isJtonNull() && jsonReader.peek() != JsonToken.END_DOCUMENT) {
        throw new JsonSyntaxException("Did not consume the entire document.");
      }
      return element;
    } catch (MalformedJsonException e) {
      throw new JsonSyntaxException(e);
    } catch (IOException e) {
      throw new JsonIOException(e);
    } catch (NumberFormatException e) {
      throw new JsonSyntaxException(e);
    }
  }

  /**
   * Returns the next value from the JSON stream as a parse tree.
   *
   * @throws JsonParseException if there is an IOException or if the specified
   *                            text is not valid JSON
   */
  public static JtonElement parse(JsonReader reader) throws JsonIOException, JsonSyntaxException {
    boolean lenient = reader.isLenient();
    reader.setLenient(true);
    try {
      return Streams.parse(reader);
    } catch (StackOverflowError e) {
      throw new JsonParseException("Failed parsing JSON source: " + reader + " to Jton", e);
    } catch (OutOfMemoryError e) {
      throw new JsonParseException("Failed parsing JSON source: " + reader + " to Jton", e);
    } finally {
      reader.setLenient(lenient);
    }
  }
}
