package com.nepheletech.jton.internal.bind;

import java.io.IOException;
import java.util.Map;

import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.internal.LazilyParsedNumber;
import com.google.gson.internal.bind.TypeAdapters;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.nepheletech.jton.JtonArray;
import com.nepheletech.jton.JtonElement;
import com.nepheletech.jton.JtonNull;
import com.nepheletech.jton.JtonObject;
import com.nepheletech.jton.JtonPrimitive;

/**
 * Type adapters for basic types.
 */
public final class JtonTypeAdapter {
	private JtonTypeAdapter() {
		throw new UnsupportedOperationException();
	}

	public static final TypeAdapter<JtonElement> JTON_ELEMENT = new TypeAdapter<JtonElement>() {
		@Override
		public JtonElement read(JsonReader in) throws IOException {
			switch (in.peek()) {
			case STRING:
				return new JtonPrimitive(in.nextString());
			case NUMBER:
				String number = in.nextString();
				return new JtonPrimitive(new LazilyParsedNumber(number));
			case BOOLEAN:
				return new JtonPrimitive(in.nextBoolean());
			case NULL:
				in.nextNull();
				return JtonNull.INSTANCE;
			case BEGIN_ARRAY:
				JtonArray array = new JtonArray();
				in.beginArray();
				while (in.hasNext()) {
					array.push(read(in));
				}
				in.endArray();
				return array;
			case BEGIN_OBJECT:
				JtonObject object = new JtonObject();
				in.beginObject();
				while (in.hasNext()) {
					object.set(in.nextName(), read(in));
				}
				in.endObject();
				return object;
			case END_DOCUMENT:
			case NAME:
			case END_OBJECT:
			case END_ARRAY:
			default:
				throw new IllegalArgumentException();
			}
		}

		@Override
		public void write(JsonWriter out, JtonElement value) throws IOException {
			if (value == null || value.isJtonNull()) {
				out.nullValue();
			} else if (value.isJtonPrimitive()) {
				JtonPrimitive primitive = value.asJtonPrimitive();
				if (primitive.isJtonTransient()) {
					throw new IllegalStateException("transient type" + primitive);
				} else {
					if (primitive.isNumber()) {
						out.value(primitive.asNumber());
					} else if (primitive.isBoolean()) {
						out.value(primitive.asBoolean());
					} else {
						out.value(primitive.asString());
					}
				}
			} else if (value.isJtonArray()) {
				out.beginArray();
				for (JtonElement e : value.asJtonArray()) {
					if (e.isJtonPrimitive() && e.asJtonPrimitive().isJtonTransient())
						continue;
					write(out, e);
				}
				out.endArray();

			} else if (value.isJtonObject()) {
				out.beginObject();
				for (Map.Entry<String, JtonElement> e : value.asJtonObject().entrySet()) {
					JtonElement jtonElement = e.getValue();
					if (jtonElement.isJtonPrimitive() && jtonElement.asJtonPrimitive().isJtonTransient())
						continue;
					out.name(e.getKey());
					write(out, jtonElement);
				}
				out.endObject();

			} else {
				throw new IllegalArgumentException("Couldn't write " + value.getClass());
			}
		}
	};

	public static final TypeAdapterFactory JTON_ELEMENT_FACTORY = TypeAdapters
			.newTypeHierarchyFactory(JtonElement.class, JTON_ELEMENT);

}
