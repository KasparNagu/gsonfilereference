package org.github.kasparnagu.gsonfilereference;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.internal.bind.ArrayTypeAdapter;
import com.google.gson.internal.bind.ReflectiveTypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

public class ReferencingGsonFactory implements TypeAdapterFactory {
	private static class FileJsonReader extends JsonReader {
		Path basePath;

		public FileJsonReader(Reader in, Path basePath) {
			super(in);
			this.basePath = basePath;

		}
		
		@Override
		public String toString() {
			return super.toString() + " basePath "+basePath;
		}

	}

	private static Path getFileFromReference(String reference, JsonReader in) {
		if (reference.startsWith("file://")) {
			Path path = Paths.get(reference.substring(7));
			if (in instanceof FileJsonReader && !path.isAbsolute()) {
				path = ((FileJsonReader)in).basePath.resolve(path);
			}
			return path;
		} else {
			return null;
		}

	}

	private static Reader getReader(Path path) throws FileNotFoundException {
		if (path != null) {
			return new FileReader(path.toFile());
		} else {
			return null;
		}
	}

	private static Reader getReaderOrThrow(String reference, JsonReader in) throws IOException {
		Path path = getFileFromReference(reference, in);
		if (path != null) {
			return getReader(path);
		} else {
			throw new IOException("Couldn't parse reference");
		}
	}

	private static JsonReader getJsonReader(String reference, JsonReader in) throws IOException {
		Path path = getFileFromReference(reference, in);
		if (path == null) {
			throw new IOException("Couldn't parse reference " + reference);
		}
		Reader rdr = getReader(path);
		return new FileJsonReader(rdr, path.getParent());
	}

	private static String readAsString(Reader rdr) throws IOException {
		StringBuilder bld = new StringBuilder();
		char[] cbuf = new char[2048];
		int len;
		while ((len = rdr.read(cbuf)) > 0) {
			bld.append(cbuf, 0, len);
		}
		return bld.toString();
	}

	public abstract static class NonStringRefernceAdapter<T> extends TypeAdapter<T> {

		final TypeAdapter<T> underlying;

		public NonStringRefernceAdapter(TypeAdapter<T> underlying) {
			this.underlying = underlying;
		}

		@Override
		public void write(JsonWriter out, T value) throws IOException {
			underlying.write(out, value);
		}

		@Override
		public T read(JsonReader in) throws IOException {
			if (in.peek() == JsonToken.STRING) {
				String reference = in.nextString();
				return readReference(reference, in);
			} else {
				return underlying.read(in);
			}
		}

		protected abstract T readReference(String reference, JsonReader in) throws IOException;
	}

	public static class ObjectRefernceAdapter<T> extends NonStringRefernceAdapter<T> {

		public ObjectRefernceAdapter(TypeAdapter<T> underlying) {
			super(underlying);
		}

		@Override
		protected T readReference(String reference, JsonReader in) throws IOException {
			return underlying.read(getJsonReader(reference, in));
		}
	}

	public static class IntRefernceAdapter extends NonStringRefernceAdapter<Integer> {

		public IntRefernceAdapter(TypeAdapter<Integer> underlying) {
			super(underlying);
		}

		@Override
		protected Integer readReference(String reference, JsonReader in) throws IOException {
			return Integer.parseInt(readAsString(getReaderOrThrow(reference, in)).trim());
		}
	}

	public static class StringRefernceAdapter extends TypeAdapter<String> {

		@Override
		public String read(JsonReader in) throws IOException {
			JsonToken peek = in.peek();
			if (peek == JsonToken.NULL) {
				in.nextNull();
				return null;
			}
			String str = in.nextString();
			Path path = getFileFromReference(str, in);
			if (path == null) {
				return str;
			} else {
				return readAsString(getReader(path));
			}

		}

		@Override
		public void write(JsonWriter out, String value) throws IOException {
			out.value(value);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> TypeAdapter<T> create(Gson _gson, TypeToken<T> _type) {
		Class<? super T> rawType = _type.getRawType();
		if (String.class.equals(rawType)) {
			return (TypeAdapter<T>) new StringRefernceAdapter();
		}
		final TypeAdapter<T> adapter = _gson.getDelegateAdapter(this, _type);
		if (ReflectiveTypeAdapterFactory.Adapter.class.equals(adapter.getClass())) {
			return new ObjectRefernceAdapter<T>(adapter);
		} else if (int.class.equals(rawType)) {
			return (TypeAdapter<T>) new IntRefernceAdapter((TypeAdapter<Integer>) adapter);
		} else if(ArrayTypeAdapter.class.equals(adapter.getClass())){
			return new ObjectRefernceAdapter<T>(adapter);
		}

		return null;
	}

	public static GsonBuilder getBuilder() {
		GsonBuilder gsonBuilder = new GsonBuilder();
		gsonBuilder.registerTypeAdapterFactory(new ReferencingGsonFactory());
		return gsonBuilder;
	}
	
	public static JsonReader getFileReader(String filepath) throws FileNotFoundException{
		return getFileReader(Paths.get(filepath));
	}
	
	public static JsonReader getFileReader(Path filepath) throws FileNotFoundException{
		return new FileJsonReader(new FileReader(filepath.toFile()), filepath.getParent());
	}
}
