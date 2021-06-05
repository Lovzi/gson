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

package org.lovzi.gson.internal.bind;

import org.lovzi.gson.Gson;
import org.lovzi.gson.TypeAdapter;
import org.lovzi.gson.TypeAdapterFactory;
import org.lovzi.gson.internal.LinkedTreeMap;
import org.lovzi.gson.reflect.TypeToken;
import org.lovzi.gson.stream.JsonReader;
import org.lovzi.gson.stream.JsonToken;
import org.lovzi.gson.stream.JsonWriter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Adapts types whose static type is only 'Object'. Uses getClass() on
 * serialization and a primitive/Map/List on deserialization.
 */
public final class ObjectTypeAdapter extends TypeAdapter<Object> {
  public static final TypeAdapterFactory FACTORY = new TypeAdapterFactory() {
    @SuppressWarnings("unchecked")
    @Override public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
      if (type.getRawType() == Object.class) {
        return (TypeAdapter<T>) new ObjectTypeAdapter(gson);
      }
      return null;
    }
  };

  private final Gson gson;

  ObjectTypeAdapter(Gson gson) {
    this.gson = gson;
  }

  @Override public Object read(JsonReader in) throws IOException {
    JsonToken token = in.peek();
    switch (token) {
    case BEGIN_ARRAY:
      List<Object> list = new ArrayList<Object>();
      in.beginArray();
      while (in.hasNext()) {
        list.add(read(in));
      }
      in.endArray();
      return list;

    case BEGIN_OBJECT:
      Map<String, Object> map = new LinkedTreeMap<String, Object>();
      in.beginObject();
      while (in.hasNext()) {
        map.put(in.nextName(), read(in));
      }
      in.endObject();
      return map;

    case STRING:
      return in.nextString();

    case NUMBER:
      String valueString = in.nextString();

      if(valueString.contains(".") || valueString.contains("e") || valueString.contains("E")){
        return Double.parseDouble(valueString);
      }

      long longValue = Long.parseLong(valueString);
      if(longValue < Integer.MAX_VALUE){
        return (int) longValue;
      }
      return longValue;

    case BOOLEAN:
      return in.nextBoolean();

    case NULL:
      in.nextNull();
      return null;

    default:
      throw new IllegalStateException();
    }
  }

  @SuppressWarnings("unchecked")
  @Override public void write(JsonWriter out, Object value) throws IOException {
    if (value == null) {
      out.nullValue();
      return;
    }

    TypeAdapter<Object> typeAdapter = (TypeAdapter<Object>) gson.getAdapter(value.getClass());
    if (typeAdapter instanceof ObjectTypeAdapter) {
      out.beginObject();
      out.endObject();
      return;
    }

    typeAdapter.write(out, value);
  }
}
