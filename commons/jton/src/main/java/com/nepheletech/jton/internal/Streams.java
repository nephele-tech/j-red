package com.nepheletech.jton.internal;

import java.io.EOFException;
import java.io.IOException;

import com.google.gson.JsonIOException;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.google.gson.stream.MalformedJsonException;
import com.nepheletech.jton.JtonElement;
import com.nepheletech.jton.JtonNull;
import com.nepheletech.jton.internal.bind.JtonTypeAdapter;

/**
 * Reads and writes GSON parse trees over streams.
 */
public final class Streams {
  private Streams() {
    throw new UnsupportedOperationException();
  }

  /**
   * Takes a reader in any state and returns the next value as a JsonElement.
   */
  public static JtonElement parse(JsonReader reader) throws JsonParseException {
    boolean isEmpty = true;
    try {
      reader.peek();
      isEmpty = false;
      return JtonTypeAdapter.JTON_ELEMENT.read(reader);
    } catch (EOFException e) {
      /*
       * For compatibility with JSON 1.5 and earlier, we return a JsonNull for empty
       * documents instead of throwing.
       */
      if (isEmpty) {
        return JtonNull.INSTANCE;
      }
      // The stream ended prematurely so it is likely a syntax error.
      throw new JsonSyntaxException(e);
    } catch (MalformedJsonException e) {
      throw new JsonSyntaxException(e);
    } catch (IOException e) {
      throw new JsonIOException(e);
    } catch (NumberFormatException e) {
      throw new JsonSyntaxException(e);
    }
  }

  /**
   * Writes the JTON element to the writer, recursively.
   */
  public static void write(JtonElement element, JsonWriter writer) throws IOException {
    JtonTypeAdapter.JTON_ELEMENT.write(writer, element);
  }

}
